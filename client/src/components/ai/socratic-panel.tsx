"use client";

import { useState } from "react";
import {
  Bot,
  CircleAlert,
  Lightbulb,
  Loader2,
  Sparkles,
  Swords,
  Wand2,
  X,
} from "lucide-react";
import { ai } from "@/lib/api";
import { useAsync } from "@/lib/use-async";
import { cn } from "@/lib/cn";
import { humanize } from "@/lib/vocabulario";
import type { ExtractedIdeas, RivalSuggestion } from "@/types/organon";
import { Button } from "@/components/ui/panel";

type Accion = "explicar" | "desglosar" | "rivales";

/**
 * El asistente socrático.
 *
 * Dos principios que gobiernan todo el panel:
 *
 * 1. Nunca guarda nada. Propone, y el lector decide. Un modelo local se
 *    equivoca con soltura —puede atribuir a Berkeley un libro de Hume— y
 *    escribir eso en el cuaderno sin revisar sería peor que no tener asistente.
 * 2. Si Ollama no está, se dice y se sigue. Tomar notas a mano nunca deja de
 *    funcionar.
 */
export function SocraticPanel({
  open,
  onClose,
  text,
  author,
  workTitle,
  onUseIdeas,
}: {
  open: boolean;
  onClose: () => void;
  /** El fragmento a analizar. Sin esto el panel se abre pero no puede hacer nada. */
  text: string;
  author?: string;
  workTitle?: string;
  /** Solo en el constructor: vuelca la propuesta en el formulario, sin guardar. */
  onUseIdeas?: (ideas: ExtractedIdeas) => void;
}) {
  const status = useAsync(() => ai.status(), [open], { enabled: open });

  const [trabajando, setTrabajando] = useState<Accion | null>(null);
  const [error, setError] = useState<string>();
  const [explicacion, setExplicacion] = useState<string>();
  const [ideas, setIdeas] = useState<ExtractedIdeas>();
  const [rivales, setRivales] = useState<RivalSuggestion[]>();

  const hayTexto = text.trim().length > 0;
  const listo = status.data?.available === true;
  const bloqueado = !listo || !hayTexto || trabajando !== null;

  const ejecutar = async (accion: Accion, tarea: () => Promise<void>) => {
    setTrabajando(accion);
    setError(undefined);
    try {
      await tarea();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setTrabajando(null);
    }
  };

  if (!open) return null;

  return (
    <>
      {/* Velo: cerrar tocando fuera, sin atrapar el foco (el panel no es modal;
          el lector debe poder seguir viendo el texto detrás). */}
      <div
        className="fixed inset-0 z-30 bg-black/40 backdrop-blur-[1px]"
        onClick={onClose}
        aria-hidden
      />

      <aside
        role="complementary"
        aria-label="Asistente socrático"
        className="fixed right-0 top-0 z-40 flex h-full w-[min(94vw,28rem)] flex-col border-l border-ink-700 bg-ink-900 shadow-2xl shadow-black/60"
      >
        <header className="flex items-start justify-between gap-3 border-b border-ink-800 px-4 py-3">
          <div className="flex items-center gap-2">
            <Bot className="size-4 text-accent-400" />
            <div>
              <h2 className="font-serif text-[15px] text-ink-50">Asistente</h2>
              <p className="text-[10.5px] text-ink-500">
                {status.loading
                  ? "Comprobando…"
                  : listo
                    ? `Modelo local: ${status.data?.configuredModel}`
                    : "No disponible"}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Cerrar"
            className="rounded p-1 text-ink-500 transition-colors hover:bg-ink-800 hover:text-ink-200"
          >
            <X className="size-4" />
          </button>
        </header>

        <div className="min-h-0 flex-1 space-y-3 overflow-y-auto p-4">
          {/* Ollama apagado: se explica y se deja seguir. */}
          {!status.loading && !listo && (
            <div className="rounded-md border border-accent-500/40 bg-accent-900/30 p-3">
              <p className="flex items-start gap-2 text-[12.5px] leading-relaxed text-accent-200">
                <CircleAlert className="mt-0.5 size-4 shrink-0" />
                {status.data?.message ??
                  "No he podido comprobar si el asistente está disponible."}
              </p>
              <Button size="sm" className="mt-2.5" onClick={status.reload}>
                Volver a comprobar
              </Button>
            </div>
          )}

          {listo && !hayTexto && (
            <p className="rounded-md border border-dashed border-ink-700 px-3 py-4 text-center text-[12px] text-ink-500">
              Selecciona un fragmento o escribe una idea para que el asistente
              tenga algo con lo que trabajar.
            </p>
          )}

          {/* Las tres acciones */}
          <div className="space-y-1.5">
            <AccionBoton
              icon={Lightbulb}
              titulo="Explícamelo sencillo"
              descripcion="Dos párrafos en cristiano sobre qué dice y por qué importaba."
              cargando={trabajando === "explicar"}
              disabled={bloqueado}
              onClick={() =>
                ejecutar("explicar", async () => {
                  const r = await ai.explainSimple({ text, author, workTitle });
                  setExplicacion(r.explanation);
                })
              }
            />
            <AccionBoton
              icon={Wand2}
              titulo="Desglosar razones"
              descripcion="Su tesis, sus razones y lo que da por supuesto sin decirlo."
              cargando={trabajando === "desglosar"}
              disabled={bloqueado}
              onClick={() =>
                ejecutar("desglosar", async () => {
                  setIdeas(await ai.extractIdeas({ text, author, workTitle }));
                })
              }
            />
            <AccionBoton
              icon={Swords}
              titulo="¿Quién le lleva la contraria?"
              descripcion="Pensadores que discutirían esta idea, y por qué."
              cargando={trabajando === "rivales"}
              disabled={bloqueado}
              onClick={() =>
                ejecutar("rivales", async () => {
                  const r = await ai.findRivals({ claim: text, author });
                  setRivales(r.suggestions);
                })
              }
            />
          </div>

          {trabajando && (
            <p className="flex items-center gap-2 rounded-md border border-ink-800 bg-ink-850/60 px-3 py-2.5 text-[12px] text-ink-400">
              <Loader2 className="size-3.5 shrink-0 animate-spin text-accent-400" />
              Pensando en tu ordenador. Un modelo local puede tardar un poco.
            </p>
          )}

          {error && (
            <p className="rounded-md border border-fallacy-500/40 bg-fallacy-900/30 px-3 py-2.5 text-[12px] leading-relaxed text-fallacy-300">
              {error}
            </p>
          )}

          {explicacion && (
            <Resultado titulo="Explicación">
              {explicacion.split("\n").filter(Boolean).map((parrafo, i) => (
                <p key={i} className="prose-source text-[13px] text-ink-100">
                  {parrafo}
                </p>
              ))}
            </Resultado>
          )}

          {ideas && (
            <Resultado titulo="Propuesta de desglose">
              <div>
                <p className="text-[10px] uppercase tracking-wider text-ink-500">
                  Lo que sostiene
                </p>
                <p className="mt-0.5 font-serif text-[13px] leading-relaxed text-ink-100">
                  {ideas.mainClaim}
                </p>
              </div>

              {ideas.reasons.length > 0 && (
                <div>
                  <p className="text-[10px] uppercase tracking-wider text-ink-500">Razones</p>
                  <ol className="mt-1 space-y-1">
                    {ideas.reasons.map((r, i) => (
                      <li key={i} className="flex gap-1.5 text-[12.5px] leading-relaxed">
                        <span className="font-mono text-[10px] text-ink-600">P{i + 1}</span>
                        <span className="text-ink-200">{r}</span>
                      </li>
                    ))}
                  </ol>
                </div>
              )}

              {ideas.unstatedAssumptions.length > 0 && (
                <div className="rounded border border-accent-500/30 bg-accent-900/25 p-2.5">
                  <p className="text-[10px] uppercase tracking-wider text-accent-300">
                    Lo que da por supuesto sin decirlo
                  </p>
                  <ul className="mt-1 space-y-1">
                    {ideas.unstatedAssumptions.map((a, i) => (
                      <li key={i} className="text-[12.5px] leading-relaxed text-accent-100">
                        · {a}
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {onUseIdeas ? (
                <Button
                  variant="primary"
                  onClick={() => {
                    onUseIdeas(ideas);
                    onClose();
                  }}
                >
                  <Sparkles className="size-3.5" />
                  Usar esta propuesta
                </Button>
              ) : (
                <p className="text-[11px] text-ink-500">
                  Abre «Desmontar» para volcarlo en el formulario y editarlo.
                </p>
              )}

              <p className="text-[10.5px] leading-relaxed text-ink-600">
                Nada de esto está guardado. Revísalo antes de darlo por bueno: un
                modelo local se equivoca con soltura.
              </p>
            </Resultado>
          )}

          {rivales && (
            <Resultado titulo="Quién discutiría esto">
              {rivales.length === 0 && (
                <p className="text-[12px] text-ink-500">No ha propuesto ninguno.</p>
              )}
              {rivales.map((r, i) => (
                <article key={i} className="rounded border border-ink-800 bg-ink-950 p-2.5">
                  <div className="flex flex-wrap items-center gap-1.5">
                    <span className="font-serif text-[13px] text-ink-100">{r.thinker}</span>
                    <span className="rounded bg-ink-800 px-1.5 py-0.5 text-[10px] uppercase tracking-wider text-ink-300">
                      {humanize(r.relation)}
                    </span>
                    {r.inNotebook && (
                      <span className="rounded bg-valid-900 px-1.5 py-0.5 text-[10px] text-valid-300">
                        ya en tu cuaderno
                      </span>
                    )}
                  </div>
                  {r.work && (
                    <p className="mt-0.5 text-[11px] italic text-ink-500">{r.work}</p>
                  )}
                  <p className="mt-1.5 text-[12.5px] leading-relaxed text-ink-300">
                    {r.explanation}
                  </p>
                </article>
              ))}
              <p className="text-[10.5px] leading-relaxed text-ink-600">
                Comprueba las referencias antes de fiarte: los modelos pequeños
                confunden títulos y autores con facilidad.
              </p>
            </Resultado>
          )}
        </div>
      </aside>
    </>
  );
}

function AccionBoton({
  icon: Icon,
  titulo,
  descripcion,
  cargando,
  disabled,
  onClick,
}: {
  icon: typeof Lightbulb;
  titulo: string;
  descripcion: string;
  cargando: boolean;
  disabled: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className={cn(
        "flex w-full items-start gap-2.5 rounded-md border border-ink-800 bg-ink-850/60 px-3 py-2.5 text-left transition-colors",
        disabled
          ? "cursor-not-allowed opacity-40"
          : "hover:border-accent-500/50 hover:bg-accent-900/30",
      )}
    >
      {cargando ? (
        <Loader2 className="mt-0.5 size-4 shrink-0 animate-spin text-accent-400" />
      ) : (
        <Icon className="mt-0.5 size-4 shrink-0 text-accent-400" strokeWidth={1.75} />
      )}
      <span className="min-w-0">
        <span className="block text-[12.5px] font-medium text-ink-100">{titulo}</span>
        <span className="block text-[11px] leading-snug text-ink-500">{descripcion}</span>
      </span>
    </button>
  );
}

function Resultado({ titulo, children }: { titulo: string; children: React.ReactNode }) {
  return (
    <section className="space-y-2.5 rounded-md border border-ink-800 bg-ink-850/40 p-3">
      <h3 className="text-[10px] font-semibold uppercase tracking-[0.12em] text-ink-400">
        {titulo}
      </h3>
      {children}
    </section>
  );
}

/** Botón que abre el panel. Se repite en el lector y en el constructor. */
export function SocraticButton({ onClick }: { onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="inline-flex items-center gap-1.5 rounded border border-accent-500/40 bg-accent-900/60 px-3 py-1.5 text-xs font-medium text-accent-300 transition-colors hover:bg-accent-500/25"
      title="Pide ayuda a un modelo que corre en tu propio ordenador"
    >
      <Bot className="size-3.5" />
      Asistente
    </button>
  );
}
