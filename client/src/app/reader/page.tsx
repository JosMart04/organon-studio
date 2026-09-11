"use client";

import { Suspense, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { ChevronLeft, ChevronRight, Download, FileText, Quote, SquarePen } from "lucide-react";
import { argumentsApi, corpus, exports, semantics } from "@/lib/api";
import type { Passage, Work } from "@/types/organon";
import { useAsync } from "@/lib/use-async";
import { useCorpusVersion, useRefrescarCorpus } from "@/lib/corpus-refresh";
import { cn } from "@/lib/cn";
import { Button, ErrorState, PageHeader } from "@/components/ui/panel";
import { InspectorTabs } from "@/components/reader/inspector-tabs";
import { SelectionMenu } from "@/components/reader/selection-menu";
import { DocumentPanel } from "@/components/reader/document-panel";
import { useDocumentoActivo } from "@/components/reader/use-documento-activo";
import { useSeleccion } from "@/components/reader/use-seleccion";
import { SocraticButton, SocraticPanel } from "@/components/ai/socratic-panel";
import { PassageDialog } from "@/components/create/create-dialogs";

type Vista = "notas" | "documento";

/** Lo seleccionado en un documento, camino del diálogo de guardado. */
interface Borrador {
  clave: number;
  texto: string;
  locator: string;
  workId?: number;
}

export default function ReaderPage() {
  // La vista y el documento abierto viajan en la URL, así una recarga vuelve a
  // la misma página del PDF. useSearchParams exige un límite de Suspense en una
  // ruta prerenderizada.
  return (
    <Suspense fallback={<p className="p-6 text-sm text-ink-500">Cargando…</p>}>
      <Lector />
    </Suspense>
  );
}

function Lector() {
  const searchParams = useSearchParams();
  const vista: Vista = searchParams.get("vista") === "documento" ? "documento" : "notas";
  const documentoId = searchParams.get("doc") ?? undefined;

  const [asistenteAbierto, setAsistenteAbierto] = useState(false);
  const [textoAsistente, setTextoAsistente] = useState<string>();
  const [borrador, setBorrador] = useState<Borrador>();
  const corpusVersion = useCorpusVersion();
  const refrescar = useRefrescarCorpus();
  const works = useAsync(() => corpus.listWorks(), [corpusVersion]);
  const activo = useDocumentoActivo(documentoId);

  // Obra y pasaje viajan juntos: cambiar de obra debe reiniciar el pasaje, y
  // guardarlos en un solo estado lo consigue sin un efecto que los sincronice.
  const [picked, setPicked] = useState<{ workId: number; index: number }>();

  // Mientras el usuario no elija, manda el libro del documento abierto y, si no
  // hay, el primero del catálogo.
  const workId =
    picked?.workId ??
    (vista === "documento" ? activo.documento?.workId : undefined) ??
    works.data?.[0]?.id;
  const passageIndex = picked?.index ?? 0;

  const activeWork = useMemo(
    () => works.data?.find((w) => w.id === workId),
    [works.data, workId],
  );

  const passages = useAsync(() => corpus.listPassages(workId!), [workId, corpusVersion], {
    enabled: workId !== undefined,
  });

  const passage = passages.data?.[passageIndex];

  const glossary = useAsync(
    () => semantics.readingContext(activeWork!.philosopherId, activeWork!.id),
    [activeWork?.philosopherId, activeWork?.id],
    { enabled: activeWork !== undefined },
  );

  const workArguments = useAsync(() => argumentsApi.list({ workId: workId! }), [workId], {
    enabled: workId !== undefined,
  });

  // Es estado del cliente: replaceState actualiza la URL y Next lo sincroniza
  // con useSearchParams sin navegar, de modo que la página conserva su estado
  // (incluido un documento que solo vive en memoria).
  const navegar = (cambios: { vista?: Vista; doc?: string | null }) => {
    const params = new URLSearchParams(searchParams.toString());
    if (cambios.vista === "documento") params.set("vista", "documento");
    if (cambios.vista === "notas") params.delete("vista");
    if (cambios.doc === null) params.delete("doc");
    if (cambios.doc) params.set("doc", cambios.doc);
    const consulta = params.toString();
    window.history.replaceState(null, "", consulta ? `/reader?${consulta}` : "/reader");
  };

  const cambiarVista = (nueva: Vista) => {
    if (nueva === "documento" && activo.documento?.workId !== undefined) {
      setPicked({ workId: activo.documento.workId, index: 0 });
    }
    navegar({ vista: nueva });
  };

  const abrirDocumento = (documento: { id: string; workId?: number }) => {
    if (documento.workId !== undefined) setPicked({ workId: documento.workId, index: 0 });
    navegar({ doc: documento.id });
  };

  const vincular = (id: number | undefined) => {
    activo.vincular(id);
    if (id !== undefined) setPicked({ workId: id, index: 0 });
  };

  const preguntar = (texto: string) => {
    setTextoAsistente(texto);
    setAsistenteAbierto(true);
  };

  if (works.error) {
    return (
      <div className="p-6">
        <ErrorState message={works.error} />
      </div>
    );
  }

  const hayLibros = (works.data?.length ?? 0) > 0;

  return (
    <div className="flex flex-col md:h-screen">
      <PageHeader
        title="Leer"
        subtitle="El texto a la izquierda, lo que vas descubriendo a la derecha."
        actions={
          <>
            {hayLibros && (
              <select
                value={workId ?? ""}
                onChange={(e) => setPicked({ workId: Number(e.target.value), index: 0 })}
                className="max-w-[22rem] rounded border border-ink-700 bg-ink-850 px-2.5 py-1.5 text-xs text-ink-100 outline-none focus:border-accent-500"
              >
                {works.data?.map((w) => (
                  <option key={w.id} value={w.id}>
                    {w.philosopherName} — {w.title}
                    {w.originalYear ? ` (${w.originalYear})` : ""}
                  </option>
                ))}
              </select>
            )}
            <SocraticButton onClick={() => setAsistenteAbierto(true)} />
            {workId !== undefined && (
              <a
                href={exports.markdownUrl(workId)}
                className="inline-flex items-center gap-1.5 rounded border border-ink-700 bg-ink-800 px-3 py-1.5 text-xs font-medium text-ink-200 transition-colors hover:bg-ink-700 hover:text-ink-50"
                title="Descargar tus notas de este libro en Markdown (Obsidian)"
              >
                <Download className="size-3.5" /> Markdown
              </a>
            )}
          </>
        }
      />

      <div className="grid min-h-0 flex-1 grid-cols-1 lg:grid-cols-[minmax(0,1fr)_26rem]">
        <div className="flex min-h-0 flex-col">
          <Pestanas vista={vista} onCambiar={cambiarVista} />

          {vista === "notas" ? (
            <SourcePanel
              work={activeWork}
              hayLibros={hayLibros}
              passages={passages.data ?? []}
              index={passageIndex}
              onIndexChange={(index) => workId !== undefined && setPicked({ workId, index })}
              loading={passages.loading}
              onPreguntar={preguntar}
            />
          ) : (
            <DocumentPanel
              activo={activo}
              works={works.data ?? []}
              onAbrir={abrirDocumento}
              onCerrar={() => navegar({ doc: null })}
              onVincular={vincular}
              onGuardar={(texto, locator) =>
                setBorrador({
                  clave: Date.now(),
                  texto,
                  locator,
                  workId: activo.documento?.workId ?? workId,
                })
              }
              onPreguntar={preguntar}
            />
          )}
        </div>

        <aside className="min-h-0 overflow-y-auto border-t border-ink-800 lg:border-l lg:border-t-0">
          <InspectorTabs
            work={activeWork}
            passage={passage}
            glossary={glossary.data ?? []}
            glossaryLoading={glossary.loading}
            argumentsForWork={workArguments.data ?? []}
            argumentsLoading={workArguments.loading}
          />
        </aside>
      </div>

      <SocraticPanel
        open={asistenteAbierto}
        onClose={() => {
          setAsistenteAbierto(false);
          setTextoAsistente(undefined);
        }}
        text={textoAsistente ?? (vista === "notas" ? (passage?.textContent ?? "") : "")}
        author={activeWork?.philosopherName}
        workTitle={activeWork?.title}
      />

      {borrador && (
        // La clave vuelve a montar el diálogo con cada selección, y con ella sus valores iniciales.
        <PassageDialog
          key={borrador.clave}
          open
          onClose={() => setBorrador(undefined)}
          onCreated={refrescar}
          defaultWorkId={borrador.workId}
          defaultText={borrador.texto}
          defaultLocator={borrador.locator}
        />
      )}
    </div>
  );
}

const PESTANAS: { id: Vista; label: string; icon: typeof Quote }[] = [
  { id: "notas", label: "Notas del cuaderno", icon: Quote },
  { id: "documento", label: "Documento", icon: FileText },
];

function Pestanas({ vista, onCambiar }: { vista: Vista; onCambiar: (vista: Vista) => void }) {
  return (
    <div role="tablist" aria-label="Qué leer" className="flex border-b border-ink-800 px-4">
      {PESTANAS.map(({ id, label, icon: Icon }) => (
        <button
          key={id}
          type="button"
          role="tab"
          aria-selected={vista === id}
          onClick={() => onCambiar(id)}
          className={cn(
            "flex items-center gap-1.5 border-b-2 px-3 py-2.5 text-[11px] font-medium transition-colors",
            vista === id
              ? "border-accent-500 text-ink-50"
              : "border-transparent text-ink-400 hover:text-ink-200",
          )}
        >
          <Icon className="size-3.5" />
          {label}
        </button>
      ))}
    </div>
  );
}

function SourcePanel({
  work,
  hayLibros,
  passages,
  index,
  onIndexChange,
  loading,
  onPreguntar,
}: {
  work: Work | undefined;
  hayLibros: boolean;
  passages: Passage[];
  index: number;
  onIndexChange: (i: number) => void;
  loading: boolean;
  onPreguntar: (texto: string) => void;
}) {
  const passage = passages[index];

  return (
    <section className="relative flex min-h-0 flex-1 flex-col">
      <div className="flex items-center justify-between gap-3 border-b border-ink-800 px-6 py-2.5">
        <div className="flex min-w-0 items-center gap-2">
          <Quote className="size-3.5 shrink-0 text-ink-600" />
          <span className="locator truncate">{passage?.locator ?? "—"}</span>
          {passage?.pageNumber != null && (
            <span className="text-[11px] text-ink-600">p. {passage.pageNumber}</span>
          )}
        </div>
        <div className="flex items-center gap-1.5">
          <span className="text-[11px] tabular-nums text-ink-500">
            {passages.length ? index + 1 : 0} / {passages.length}
          </span>
          <Button
            size="sm"
            variant="ghost"
            disabled={index <= 0}
            onClick={() => onIndexChange(index - 1)}
          >
            <ChevronLeft className="size-3.5" />
          </Button>
          <Button
            size="sm"
            variant="ghost"
            disabled={index >= passages.length - 1}
            onClick={() => onIndexChange(index + 1)}
          >
            <ChevronRight className="size-3.5" />
          </Button>
        </div>
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto">
        <div className="mx-auto max-w-[42rem] px-6 py-8">
          {work && (
            <header className="mb-6 border-b border-ink-800 pb-4">
              <h2 className="font-serif text-lg text-ink-50">{work.title}</h2>
              <p className="mt-0.5 text-xs text-ink-500">
                {work.philosopherName}
                {work.originalYear ? ` · ${work.originalYear}` : ""}
              </p>
            </header>
          )}

          {loading && <p className="text-sm text-ink-500">Cargando…</p>}

          {!loading && !passage && (
            <p className="text-sm leading-relaxed text-ink-500">
              {hayLibros
                ? "Todavía no has guardado ningún fragmento de este libro. Ábrelo en la pestaña «Documento» y selecciona una frase, o usa «Añadir» en el menú de la izquierda."
                : "Tu cuaderno todavía no tiene libros. Añade uno desde el menú de la izquierda, o abre en la pestaña «Documento» el PDF que estás leyendo."}
            </p>
          )}

          {passage && work && (
            // La clave remonta el bloque al cambiar de pasaje, lo que descarta
            // la selección pendiente sin necesidad de un efecto que la limpie.
            <SelectableText
              key={passage.id}
              passage={passage}
              work={work}
              onPreguntar={onPreguntar}
            />
          )}

          {passages.length > 1 && (
            <nav className="mt-8 flex flex-wrap gap-1.5 border-t border-ink-800 pt-4">
              {passages.map((p, i) => (
                <button
                  key={p.id}
                  onClick={() => onIndexChange(i)}
                  className={cn(
                    "rounded border px-2 py-1 font-mono text-[10px] transition-colors",
                    i === index
                      ? "border-accent-500/50 bg-accent-900 text-accent-300"
                      : "border-ink-700 text-ink-400 hover:border-ink-600 hover:text-ink-200",
                  )}
                >
                  {p.locator}
                </button>
              ))}
            </nav>
          )}
        </div>
      </div>

      <footer className="border-t border-ink-800 px-6 py-2">
        <Link
          href="/arguments/builder"
          className="inline-flex items-center gap-1.5 text-[11px] text-ink-500 transition-colors hover:text-accent-400"
        >
          <SquarePen className="size-3" />
          Desmontar una idea de este libro
        </Link>
      </footer>
    </section>
  );
}

function SelectableText({
  passage,
  work,
  onPreguntar,
}: {
  passage: Passage;
  work: Work;
  onPreguntar: (texto: string) => void;
}) {
  const hostRef = useRef<HTMLDivElement>(null);
  const { seleccion, capturar, limpiar } = useSeleccion(hostRef);

  return (
    <div ref={hostRef} className="relative" onMouseUp={capturar} onKeyUp={capturar}>
      <div className="prose-source select-text">
        {passage.textContent.split("\n").map((paragraph, i) => (
          <p key={i}>{paragraph}</p>
        ))}
      </div>

      {seleccion && (
        <SelectionMenu
          text={seleccion.texto}
          x={seleccion.x}
          y={seleccion.y}
          workId={work.id}
          philosopherId={work.philosopherId}
          passageId={passage.id}
          onPreguntar={() => {
            onPreguntar(seleccion.texto);
            limpiar();
          }}
          onClose={limpiar}
        />
      )}
    </div>
  );
}
