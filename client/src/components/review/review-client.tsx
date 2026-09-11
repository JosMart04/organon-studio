"use client";

import { useState } from "react";
import { useSearchParams } from "next/navigation";
import { CircleAlert, GraduationCap, Lightbulb, Loader2, Send } from "lucide-react";
import { ai, corpus, review } from "@/lib/api";
import type { ReviewAttempt, ReviewCard, ReviewRating } from "@/types/organon";
import { useAsync } from "@/lib/use-async";
import { cn } from "@/lib/cn";
import { Button, EmptyState, ErrorState, PageHeader } from "@/components/ui/panel";
import { Avatar } from "@/components/ui/badges";

const MAX_RESPUESTA = 4000;

const VALORACIONES: Record<ReviewRating, { etiqueta: string; clase: string }> = {
  SOLIDA: { etiqueta: "Sólida", clase: "border-valid-500/50 bg-valid-900/60 text-valid-300" },
  A_MEDIAS: { etiqueta: "A medias", clase: "border-accent-500/50 bg-accent-900/60 text-accent-300" },
  FLOJA: { etiqueta: "Floja", clase: "border-fallacy-500/50 bg-fallacy-900/60 text-fallacy-300" },
};

/**
 * Repaso socrático: la tarjeta enseña una idea desmontada y, al pedir un
 * desafío, se da la vuelta con la pregunta del asistente. Lo que se valora es
 * cómo razona el lector, no si coincide con el autor.
 */
export function ReviewClient() {
  const params = useSearchParams();
  // Desde «Desafíame» en el constructor o en el mapa llega una idea concreta.
  const [ideaPedida, setIdeaPedida] = useState(() => entero(params.get("argumentId")));
  const [libro, setLibro] = useState<number | "">("");
  const [anterior, setAnterior] = useState<number>();
  const [ronda, setRonda] = useState(0);

  const estadoIa = useAsync(() => ai.status(), []);
  const libros = useAsync(() => corpus.listWorks(), []);
  const progreso = useAsync(() => review.progress(), [ronda]);
  const tarjeta = useAsync(
    () =>
      review.next({
        argumentId: ideaPedida,
        workId: libro === "" ? undefined : libro,
        after: anterior,
      }),
    [ideaPedida, libro, anterior, ronda],
  );

  const iaLista = estadoIa.data?.available === true;
  const sinIdeas = !tarjeta.loading && !tarjeta.error && tarjeta.data === undefined;

  const siguiente = () => {
    if (tarjeta.data) setAnterior(tarjeta.data.argumentId);
    if (ideaPedida !== undefined) {
      setIdeaPedida(undefined);
      window.history.replaceState(null, "", "/review");
    }
    setRonda((r) => r + 1);
  };

  return (
    <div className="flex min-h-screen flex-col">
      <PageHeader
        title="Repasar"
        subtitle="Ponte a prueba con las ideas que has desmontado. Lo que cuenta es cómo razonas, no si coincides con el autor."
        actions={
          (libros.data?.length ?? 0) > 0 && (
            <select
              value={libro}
              onChange={(e) => {
                setLibro(e.target.value ? Number(e.target.value) : "");
                setIdeaPedida(undefined);
                setAnterior(undefined);
              }}
              className="max-w-[20rem] rounded border border-ink-700 bg-ink-850 px-2.5 py-1.5 text-xs text-ink-100 outline-none focus:border-accent-500"
            >
              <option value="">Todo el cuaderno</option>
              {libros.data?.map((w) => (
                <option key={w.id} value={w.id}>
                  {w.philosopherName} — {w.title}
                </option>
              ))}
            </select>
          )
        }
      />

      <div className="mx-auto w-full max-w-[46rem] flex-1 space-y-4 px-6 py-6">
        {progreso.data && progreso.data.reviewable > 0 && (
          <p className="text-[12px] text-ink-500">
            Repasadas esta semana:{" "}
            <span className="tabular-nums text-ink-200">{progreso.data.reviewedThisWeek}</span>
            {" · "}
            Sin repasar nunca:{" "}
            <span className="tabular-nums text-ink-200">{progreso.data.neverReviewed}</span>
          </p>
        )}

        {!estadoIa.loading && !iaLista && (
          <div className="rounded-md border border-accent-500/40 bg-accent-900/30 p-3">
            <p className="flex items-start gap-2 text-[12.5px] leading-relaxed text-accent-200">
              <CircleAlert className="mt-0.5 size-4 shrink-0" />
              <span>
                {estadoIa.data?.message ?? "No he podido comprobar si el asistente está disponible."}{" "}
                Mientras tanto puedes repasar tus ideas, pero no pedir desafíos.
              </span>
            </p>
            <Button size="sm" className="mt-2.5" onClick={estadoIa.reload}>
              Volver a comprobar
            </Button>
          </div>
        )}

        {tarjeta.error && <ErrorState message={tarjeta.error} />}
        {tarjeta.loading && <p className="text-sm text-ink-500">Buscando la idea que toca…</p>}
        {sinIdeas && (
          <EmptyState
            title="Todavía no hay ideas con razones que repasar."
            hint="Desmonta una idea en «Desmontar», con al menos una razón, y vuelve aquí."
          />
        )}

        {tarjeta.data && !tarjeta.loading && (
          <SesionDeRepaso
            key={`${tarjeta.data.argumentId}:${ronda}`}
            tarjeta={tarjeta.data}
            iaLista={iaLista}
            onRespondida={progreso.reload}
            onSiguiente={siguiente}
          />
        )}
      </div>
    </div>
  );
}

function SesionDeRepaso({
  tarjeta,
  iaLista,
  onRespondida,
  onSiguiente,
}: {
  tarjeta: ReviewCard;
  iaLista: boolean;
  onRespondida: () => void;
  onSiguiente: () => void;
}) {
  const [intento, setIntento] = useState<ReviewAttempt>();
  const [respuesta, setRespuesta] = useState("");
  const [trabajando, setTrabajando] = useState<"desafio" | "respuesta" | null>(null);
  const [error, setError] = useState<string>();
  const [verPista, setVerPista] = useState(false);

  const volteada = intento !== undefined;
  const valorada = intento?.rating != null;

  const ejecutar = async (tarea: "desafio" | "respuesta", accion: () => Promise<void>) => {
    setTrabajando(tarea);
    setError(undefined);
    try {
      await accion();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setTrabajando(null);
    }
  };

  const desafiar = () =>
    ejecutar("desafio", async () => {
      setIntento(await review.challenge({ argumentId: tarjeta.argumentId }));
    });

  const responder = () =>
    ejecutar("respuesta", async () => {
      setIntento(await review.answer(intento!.id, respuesta));
      onRespondida();
    });

  return (
    <div className="space-y-4">
      <div className="tarjeta-repaso">
        <div className="tarjeta-repaso__cuerpo" data-volteada={volteada}>
          <section
            className="tarjeta-repaso__cara rounded-xl border border-ink-700 bg-ink-900 p-6 shadow-xl shadow-black/40"
            inert={volteada}
            aria-hidden={volteada}
          >
            <div className="flex items-center gap-3">
              <Avatar name={tarjeta.author} emoji={tarjeta.avatarEmoji ?? undefined} size="md" />
              <div className="min-w-0">
                <p className="truncate text-[12px] text-ink-300">
                  {tarjeta.author} · {tarjeta.workTitle}
                </p>
                <p className="text-[10.5px] text-ink-500">
                  {tarjeta.lastRating
                    ? `La última vez salió ${VALORACIONES[tarjeta.lastRating].etiqueta.toLowerCase()}`
                    : "Nunca la has repasado"}
                </p>
              </div>
            </div>

            <h2 className="mt-5 font-serif text-[22px] leading-snug text-ink-50">{tarjeta.name}</h2>

            <p className="mt-4 text-[10px] font-semibold uppercase tracking-[0.12em] text-ink-500">
              A dónde llega
            </p>
            <p className="prose-source mt-1 text-[15px]">{tarjeta.conclusion}</p>

            {tarjeta.reasons.length > 0 && (
              <details className="mt-4">
                <summary className="cursor-pointer text-[11.5px] text-ink-500 transition-colors hover:text-ink-300">
                  {tarjeta.reasons.length === 1 ? "Ver su razón" : `Ver sus ${tarjeta.reasons.length} razones`}
                  {tarjeta.assumptions > 0 &&
                    ` (${tarjeta.assumptions} ${tarjeta.assumptions === 1 ? "la da" : "las da"} por supuesta${tarjeta.assumptions === 1 ? "" : "s"})`}
                </summary>
                <ol className="mt-2 space-y-1 pl-1">
                  {tarjeta.reasons.map((razon, i) => (
                    <li key={i} className="flex gap-2 text-[13px] leading-relaxed text-ink-300">
                      <span className="font-mono text-[10px] text-ink-600">{i + 1}</span>
                      {razon}
                    </li>
                  ))}
                </ol>
              </details>
            )}

            <div className="mt-6 flex flex-wrap items-center gap-3">
              <Button variant="primary" onClick={desafiar} disabled={!iaLista || trabajando !== null}>
                {trabajando === "desafio" ? (
                  <Loader2 className="size-3.5 animate-spin" />
                ) : (
                  <GraduationCap className="size-3.5" />
                )}
                Desafíame
              </Button>
              <button
                type="button"
                onClick={onSiguiente}
                disabled={trabajando !== null}
                className="text-[12px] text-ink-500 transition-colors hover:text-ink-300 disabled:opacity-50"
              >
                Prefiero otra idea
              </button>
            </div>
            {trabajando === "desafio" && (
              <p className="mt-3 text-[12px] leading-relaxed text-ink-500">
                Pensando una pregunta difícil en tu ordenador. Un modelo local puede tardar un poco.
              </p>
            )}
          </section>

          <section
            className="tarjeta-repaso__cara tarjeta-repaso__cara--dorso rounded-xl border border-accent-500/40 bg-ink-900 p-6 shadow-xl shadow-black/40"
            inert={!volteada}
            aria-hidden={!volteada}
          >
            {intento && (
              <>
                <p className="text-[10px] font-semibold uppercase tracking-[0.12em] text-accent-300">
                  {intento.kind === "SUPUESTO" ? "Sobre lo que da por supuesto" : "Un caso difícil"} ·{" "}
                  {tarjeta.name}
                </p>
                <p className="mt-2 font-serif text-[18px] leading-snug text-ink-50">{intento.question}</p>

                {intento.counterexample && (
                  <blockquote className="mt-3 border-l-2 border-accent-500/50 pl-3 text-[13.5px] leading-relaxed text-ink-200">
                    {intento.counterexample}
                  </blockquote>
                )}

                {intento.hint &&
                  !valorada &&
                  (verPista ? (
                    <p className="mt-3 flex gap-2 text-[12.5px] leading-relaxed text-ink-300">
                      <Lightbulb className="mt-0.5 size-3.5 shrink-0 text-accent-400" />
                      {intento.hint}
                    </p>
                  ) : (
                    <button
                      type="button"
                      onClick={() => setVerPista(true)}
                      className="mt-3 inline-flex items-center gap-1.5 text-[12px] text-ink-500 transition-colors hover:text-accent-300"
                    >
                      <Lightbulb className="size-3.5" />
                      Pista
                    </button>
                  ))}

                {valorada ? (
                  <Valoracion intento={intento} />
                ) : (
                  <div className="mt-4 space-y-2">
                    <label htmlFor="respuesta-repaso" className="block text-[11px] font-medium text-ink-300">
                      Tu respuesta
                    </label>
                    <textarea
                      id="respuesta-repaso"
                      value={respuesta}
                      onChange={(e) => setRespuesta(e.target.value)}
                      rows={6}
                      maxLength={MAX_RESPUESTA}
                      placeholder="Razónalo con tus palabras. Discrepar del autor está bien, si das razones."
                      className="w-full resize-y rounded border border-ink-700 bg-ink-950 px-3 py-2 font-serif text-[14px] leading-relaxed text-ink-100 outline-none placeholder:text-ink-600 focus:border-accent-500"
                    />
                    <div className="flex items-center justify-between gap-2">
                      <span className="text-[10.5px] tabular-nums text-ink-600">
                        {respuesta.length} / {MAX_RESPUESTA}
                      </span>
                      <Button
                        variant="primary"
                        onClick={responder}
                        disabled={!respuesta.trim() || trabajando !== null}
                      >
                        {trabajando === "respuesta" ? (
                          <Loader2 className="size-3.5 animate-spin" />
                        ) : (
                          <Send className="size-3.5" />
                        )}
                        Enviar respuesta
                      </Button>
                    </div>
                    {trabajando === "respuesta" && (
                      <p className="text-[12px] text-ink-500">Leyendo tu respuesta con calma…</p>
                    )}
                  </div>
                )}
              </>
            )}
          </section>
        </div>
      </div>

      {error && (
        <p
          role="alert"
          className="rounded-md border border-fallacy-500/40 bg-fallacy-900/30 px-3 py-2.5 text-[12px] leading-relaxed text-fallacy-300"
        >
          {error}
        </p>
      )}

      {valorada && (
        <div className="flex justify-end">
          <Button variant="primary" onClick={onSiguiente}>
            Siguiente idea →
          </Button>
        </div>
      )}

      <Historial argumentId={tarjeta.argumentId} version={intento?.rating ?? ""} />
    </div>
  );
}

function Valoracion({ intento }: { intento: ReviewAttempt }) {
  const valoracion = VALORACIONES[intento.rating ?? "A_MEDIAS"];
  return (
    <div className="mt-4 space-y-3">
      <span
        className={cn(
          "inline-block rounded-full border px-2.5 py-0.5 text-[11px] font-semibold uppercase tracking-wider",
          valoracion.clase,
        )}
      >
        {valoracion.etiqueta}
      </span>
      <blockquote className="border-l-2 border-ink-700 pl-3 font-serif text-[13px] italic leading-relaxed text-ink-400">
        {intento.answer}
      </blockquote>
      {intento.whatWorked && <Bloque titulo="Lo que funciona" texto={intento.whatWorked} />}
      {intento.whatToImprove && <Bloque titulo="Qué mejorar" texto={intento.whatToImprove} />}
      {intento.followUpQuestion && (
        <Bloque titulo="Para seguir pensando" texto={intento.followUpQuestion} serif />
      )}
    </div>
  );
}

function Bloque({ titulo, texto, serif }: { titulo: string; texto: string; serif?: boolean }) {
  return (
    <div>
      <p className="text-[10px] font-semibold uppercase tracking-[0.12em] text-ink-500">{titulo}</p>
      <p className={cn("mt-0.5 text-[13px] leading-relaxed text-ink-200", serif && "font-serif text-[14px]")}>
        {texto}
      </p>
    </div>
  );
}

/** Los repasos anteriores de la idea. Se recarga cuando llega una valoración nueva. */
function Historial({ argumentId, version }: { argumentId: number; version: string }) {
  const historial = useAsync(() => review.history(argumentId), [argumentId, version]);
  const respondidos = (historial.data ?? []).filter((i) => i.rating !== null && i.answeredAt !== null);
  if (respondidos.length === 0) return null;

  return (
    <details className="rounded-md border border-ink-800 bg-ink-900/40 px-4 py-3">
      <summary className="cursor-pointer text-[11.5px] text-ink-500 transition-colors hover:text-ink-300">
        Repasos anteriores de esta idea ({respondidos.length})
      </summary>
      <ul className="mt-2 space-y-2.5">
        {respondidos.map((i) => (
          <li key={i.id} className="text-[12px] leading-relaxed">
            <span
              className={cn(
                "mr-2 rounded border px-1.5 py-px text-[9.5px] uppercase tracking-wider",
                VALORACIONES[i.rating!].clase,
              )}
            >
              {VALORACIONES[i.rating!].etiqueta}
            </span>
            <span className="text-ink-500">
              {new Date(i.answeredAt!).toLocaleDateString("es-ES", { day: "numeric", month: "short" })}
            </span>
            <span className="mt-0.5 block text-ink-300">{i.question}</span>
          </li>
        ))}
      </ul>
    </details>
  );
}

function entero(valor: string | null): number | undefined {
  const numero = Number(valor);
  return valor && Number.isInteger(numero) ? numero : undefined;
}
