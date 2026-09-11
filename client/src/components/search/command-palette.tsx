"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { BookMarked, Loader2, Quote, RefreshCw, SquareSigma, User } from "lucide-react";
import { search } from "@/lib/api";
import type { MatchSource, SearchHitType, SearchIndexStatus, SearchResult } from "@/types/organon";
import { useAsync } from "@/lib/use-async";
import { cn } from "@/lib/cn";
import { Dialog } from "@/components/ui/dialog";

type Modo = "palabras" | "significado";

const TIPOS: Record<SearchHitType, { titulo: string; icon: typeof Quote }> = {
  PASSAGE: { titulo: "Fragmentos", icon: Quote },
  ARGUMENT: { titulo: "Ideas", icon: SquareSigma },
  DEFINITION: { titulo: "Palabras del glosario", icon: BookMarked },
  PHILOSOPHER: { titulo: "Pensadores", icon: User },
};

/**
 * Buscador de todo el cuaderno, con Ctrl+K desde cualquier pantalla.
 *
 * Dos pestañas porque responden a dos preguntas distintas: «¿dónde anoté esta
 * palabra?», que se contesta al instante mientras se escribe, y «¿dónde hablaba
 * de algo parecido a esto?», que consulta al modelo local y por eso espera a
 * que se pulse Enter.
 */
export function CommandPalette({ onClose }: { onClose: () => void }) {
  const router = useRouter();
  const [modo, setModo] = useState<Modo>("palabras");
  const [consulta, setConsulta] = useState("");
  const [enviada, setEnviada] = useState<string>();
  const [activo, setActivo] = useState(0);
  const [versionEstado, setVersionEstado] = useState(0);
  const [actualizando, setActualizando] = useState(false);
  const entradaRef = useRef<HTMLInputElement>(null);

  // showModal() enfoca el primer botón del diálogo (la X de cerrar), y React no
  // deja en el DOM el atributo autofocus que lo evitaría. Los efectos del hijo
  // corren antes, así que esto llega justo después de abrirlo: se escribe sin clic.
  useEffect(() => {
    entradaRef.current?.focus();
  }, []);

  const palabras = useDiferido(consulta.trim(), 200);
  const porPalabras = useAsync(() => search.text(palabras), [palabras], {
    enabled: modo === "palabras" && palabras.length >= 2,
  });
  const porSignificado = useAsync(() => search.semantic(enviada!), [enviada], {
    enabled: modo === "significado" && enviada !== undefined,
  });
  const estado = useAsync(() => search.status(), [versionEstado]);

  const resultados: SearchResult[] =
    modo === "palabras"
      ? palabras.length >= 2
        ? (porPalabras.data ?? [])
        : []
      : enviada !== undefined
        ? (porSignificado.data?.results ?? [])
        : [];

  const grupos = agrupar(resultados);
  const ordenados = grupos.flatMap((grupo) => grupo.resultados);
  const indice = Math.min(activo, Math.max(ordenados.length - 1, 0));
  const cargando = modo === "palabras" ? porPalabras.loading : porSignificado.loading;
  const error = modo === "palabras" ? porPalabras.error : porSignificado.error;

  const aviso =
    modo === "significado"
      ? (porSignificado.data?.notice ??
        (estado.data && !estado.data.available ? estado.data.message : null))
      : null;

  const ir = (resultado: SearchResult) => {
    router.push(enlaceDe(resultado));
    onClose();
  };

  const cambiarModo = (nuevo: Modo) => {
    setModo(nuevo);
    setActivo(0);
    // Pasar a «significado» con algo escrito lanza la búsqueda sin otro Enter.
    if (nuevo === "significado" && consulta.trim()) setEnviada(consulta.trim());
  };

  const alTeclear = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.nativeEvent.isComposing) return;
    if (e.key === "ArrowDown" || e.key === "ArrowUp") {
      e.preventDefault();
      if (ordenados.length === 0) return;
      const paso = e.key === "ArrowDown" ? 1 : -1;
      setActivo((indice + paso + ordenados.length) % ordenados.length);
    } else if (e.key === "Enter") {
      e.preventDefault();
      const texto = consulta.trim();
      if (modo === "significado" && texto && texto !== enviada) {
        setEnviada(texto);
        setActivo(0);
      } else if (ordenados[indice]) {
        ir(ordenados[indice]);
      }
    }
  };

  const actualizarIndice = async () => {
    setActualizando(true);
    try {
      await search.reindex();
    } finally {
      setActualizando(false);
      setVersionEstado((v) => v + 1);
      // La indexación sigue en segundo plano: se vuelve a mirar al rato.
      setTimeout(() => setVersionEstado((v) => v + 1), 4000);
    }
  };

  return (
    <Dialog
      open
      onClose={onClose}
      title="Buscar en tu cuaderno"
      wide
      footer={
        <>
          <PieEstado estado={estado.data} actualizando={actualizando} onActualizar={actualizarIndice} />
          <span className="hidden text-[10.5px] text-ink-600 sm:inline">
            ↑↓ para moverte · Enter para abrir · Esc para cerrar
          </span>
        </>
      }
    >
      <div className="space-y-3">
        <input
          ref={entradaRef}
          value={consulta}
          onChange={(e) => {
            setConsulta(e.target.value);
            setActivo(0);
          }}
          onKeyDown={alTeclear}
          aria-label="Qué buscas"
          placeholder={
            modo === "palabras"
              ? "Una palabra o un nombre: razón, tiempo, Séneca…"
              : "Describe la idea con tus palabras y pulsa Enter"
          }
          className="w-full rounded-md border border-ink-700 bg-ink-950 px-3 py-2 text-[14px] text-ink-50 outline-none placeholder:text-ink-600 focus:border-accent-500"
        />

        <div role="tablist" aria-label="Cómo buscar" className="flex items-center gap-1">
          {(["palabras", "significado"] as const).map((m) => (
            <button
              key={m}
              type="button"
              role="tab"
              aria-selected={modo === m}
              onClick={() => cambiarModo(m)}
              className={cn(
                "rounded px-2.5 py-1 text-[11px] font-medium transition-colors",
                modo === m
                  ? "bg-accent-900 text-accent-300"
                  : "text-ink-400 hover:bg-ink-800 hover:text-ink-200",
              )}
            >
              {m === "palabras" ? "Palabras" : "Por significado (IA)"}
            </button>
          ))}
          {cargando && <Loader2 className="ml-1 size-3.5 animate-spin text-accent-400" />}
        </div>

        {aviso && (
          <p className="rounded-md border border-accent-500/30 bg-accent-900/30 px-3 py-2 text-[12px] leading-relaxed text-accent-200">
            {aviso}
          </p>
        )}

        {error && <p className="text-[12px] text-fallacy-300">{error}</p>}

        <Indicacion
          modo={modo}
          palabras={palabras}
          enviada={enviada}
          cargando={cargando}
          hayResultados={ordenados.length > 0}
        />

        {grupos.map((grupo) => {
          const { titulo, icon: Icon } = TIPOS[grupo.tipo];
          return (
            <section key={grupo.tipo}>
              <h3 className="mb-1 flex items-center gap-1.5 text-[10px] font-semibold uppercase tracking-[0.12em] text-ink-500">
                <Icon className="size-3" />
                {titulo}
              </h3>
              <ul role="listbox" aria-label={titulo} className="space-y-1">
                {grupo.resultados.map((resultado) => {
                  const posicion = ordenados.indexOf(resultado);
                  const esActivo = posicion === indice;
                  const detalle = contexto(resultado);
                  return (
                    <li key={`${resultado.type}-${resultado.id}`} role="option" aria-selected={esActivo}>
                      <button
                        type="button"
                        ref={(elemento) => {
                          if (esActivo) elemento?.scrollIntoView({ block: "nearest" });
                        }}
                        onMouseEnter={() => setActivo(posicion)}
                        onClick={() => ir(resultado)}
                        className={cn(
                          "w-full rounded-md border px-3 py-2 text-left transition-colors",
                          esActivo
                            ? "border-accent-500/50 bg-accent-900/25"
                            : "border-ink-800 bg-ink-850/40 hover:border-ink-700",
                        )}
                      >
                        <span className="flex items-baseline justify-between gap-2">
                          <span className="truncate font-serif text-[13.5px] text-ink-50">
                            {resultado.title}
                          </span>
                          {modo === "significado" && <Motivos motivos={resultado.matchedBy} />}
                        </span>
                        {detalle && <span className="block text-[10.5px] text-ink-500">{detalle}</span>}
                        {resultado.snippet && (
                          <span className="mt-1 line-clamp-2 block text-[12px] leading-relaxed text-ink-300">
                            <Fragmento texto={resultado.snippet} />
                          </span>
                        )}
                      </button>
                    </li>
                  );
                })}
              </ul>
            </section>
          );
        })}
      </div>
    </Dialog>
  );
}

function Indicacion({
  modo,
  palabras,
  enviada,
  cargando,
  hayResultados,
}: {
  modo: Modo;
  palabras: string;
  enviada: string | undefined;
  cargando: boolean;
  hayResultados: boolean;
}) {
  let texto: string | null = null;
  if (modo === "palabras") {
    if (palabras.length < 2) texto = "Escribe al menos dos letras.";
    else if (!cargando && !hayResultados)
      texto = `Nada coincide con «${palabras}». Prueba a buscar por significado.`;
  } else if (enviada === undefined) {
    texto =
      "Encuentra notas que dicen algo parecido aunque no compartan palabras: «lo efímero de la existencia» da con una carta sobre el tiempo.";
  } else if (cargando && !hayResultados) {
    texto = "Buscando por significado en tu ordenador…";
  } else if (!cargando && !hayResultados) {
    texto = "No he encontrado nada parecido.";
  }
  return texto ? <p className="text-[12px] leading-relaxed text-ink-500">{texto}</p> : null;
}

function Motivos({ motivos }: { motivos: MatchSource[] }) {
  return (
    <span className="flex shrink-0 gap-1">
      {motivos.map((motivo) => (
        <span
          key={motivo}
          className={cn(
            "rounded px-1.5 py-px text-[9.5px] uppercase tracking-wider",
            motivo === "SIGNIFICADO" ? "bg-accent-900 text-accent-300" : "bg-ink-800 text-ink-400",
          )}
        >
          {motivo === "SIGNIFICADO" ? "significado" : "palabras"}
        </span>
      ))}
    </span>
  );
}

/** Pinta resaltadas las coincidencias que el servidor marca entre ⟦ y ⟧, sin interpretar HTML. */
function Fragmento({ texto }: { texto: string }) {
  return (
    <>
      {texto.split(/(⟦[^⟧]*⟧)/g).map((parte, i) =>
        parte.startsWith("⟦") && parte.endsWith("⟧") ? (
          <mark key={i} className="rounded bg-accent-500/25 px-0.5 text-accent-200">
            {parte.slice(1, -1)}
          </mark>
        ) : (
          <span key={i}>{parte}</span>
        ),
      )}
    </>
  );
}

function PieEstado({
  estado,
  actualizando,
  onActualizar,
}: {
  estado: SearchIndexStatus | undefined;
  actualizando: boolean;
  onActualizar: () => void;
}) {
  if (!estado) {
    return <span className="mr-auto text-[11px] text-ink-600">Comprobando el índice…</span>;
  }
  if (!estado.available) {
    return (
      <span className="mr-auto text-[11px] text-ink-500">
        Búsqueda por significado no disponible: solo palabras
      </span>
    );
  }
  return (
    <span className="mr-auto flex min-w-0 items-center gap-2 text-[11px] text-ink-500">
      <span className="truncate">
        {estado.indexed} {estado.indexed === 1 ? "nota indexada" : "notas indexadas"}
        {estado.pending > 0 && ` · ${estado.pending} pendientes`}
      </span>
      <button
        type="button"
        onClick={onActualizar}
        disabled={actualizando}
        className="inline-flex shrink-0 items-center gap-1 rounded px-1.5 py-0.5 text-ink-400 transition-colors hover:bg-ink-800 hover:text-ink-200 disabled:opacity-50"
      >
        <RefreshCw className={cn("size-3", actualizando && "animate-spin")} />
        Actualizar
      </button>
    </span>
  );
}

/** A dónde lleva cada tipo de resultado. */
export function enlaceDe(resultado: SearchResult): string {
  switch (resultado.type) {
    case "PASSAGE":
      return `/reader?workId=${resultado.workId}&passageId=${resultado.id}`;
    case "ARGUMENT":
      return `/arguments/builder?argumentId=${resultado.id}`;
    case "DEFINITION":
      return `/glossary?conceptId=${resultado.conceptId}`;
    case "PHILOSOPHER":
      // No hay ficha de pensador: se abre su primer libro o, si no tiene, el mapa.
      return resultado.workId ? `/reader?workId=${resultado.workId}` : "/graph";
  }
}

function contexto(resultado: SearchResult): string | null {
  if (resultado.type === "PHILOSOPHER") return null;
  return [resultado.workTitle, resultado.author].filter(Boolean).join(" · ") || null;
}

/** Agrupa por tipo sin perder el orden: el grupo del mejor resultado va primero. */
function agrupar(resultados: SearchResult[]) {
  const grupos = new Map<SearchHitType, SearchResult[]>();
  for (const resultado of resultados) {
    grupos.set(resultado.type, [...(grupos.get(resultado.type) ?? []), resultado]);
  }
  return [...grupos].map(([tipo, lista]) => ({ tipo, resultados: lista }));
}

function useDiferido<T>(valor: T, espera: number): T {
  const [diferido, setDiferido] = useState(valor);
  useEffect(() => {
    const temporizador = setTimeout(() => setDiferido(valor), espera);
    return () => clearTimeout(temporizador);
  }, [valor, espera]);
  return diferido;
}
