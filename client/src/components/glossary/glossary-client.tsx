"use client";

import { useState } from "react";
import { useSearchParams } from "next/navigation";
import { Columns3, TriangleAlert } from "lucide-react";
import { semantics } from "@/lib/api";
import { useAsync } from "@/lib/use-async";
import { cn } from "@/lib/cn";
import { EmptyState, ErrorState, PageHeader } from "@/components/ui/panel";

export function GlossaryClient() {
  const params = useSearchParams();
  const initial = params.get("conceptId");

  const concepts = useAsync(() => semantics.listConcepts(), []);
  const [picked, setPicked] = useState<number | undefined>(
    initial ? Number(initial) : undefined,
  );

  // Mientras no se elija nada manda el primer concepto del glosario. Derivarlo
  // en vez de fijarlo con un efecto evita un render encadenado en cada carga.
  const conceptId = picked ?? concepts.data?.[0]?.id;

  const comparison = useAsync(
    () => semantics.compareConcept(conceptId!),
    [conceptId],
    { enabled: conceptId !== undefined },
  );

  if (concepts.error) {
    return (
      <div className="p-6">
        <ErrorState message={concepts.error} />
      </div>
    );
  }

  const readings = comparison.data?.readings ?? [];

  return (
    <div className="flex min-h-screen flex-col">
      <PageHeader
        title="Sobrecarga semántica"
        subtitle="La misma palabra en boca de autores distintos. Ver las definiciones en columnas paralelas es constatar que no hablan de lo mismo."
      />

      <div className="flex flex-wrap gap-1.5 border-b border-ink-800 px-6 py-3">
        {concepts.data?.map((concept) => (
          <button
            key={concept.id}
            onClick={() => setPicked(concept.id)}
            className={cn(
              "rounded border px-3 py-1.5 text-xs transition-colors",
              concept.id === conceptId
                ? "border-term-500/50 bg-term-900 text-term-300"
                : "border-ink-700 text-ink-400 hover:border-ink-600 hover:text-ink-200",
            )}
          >
            <span className="font-serif">{concept.term}</span>
            <span className="ml-1.5 font-mono text-[10px] text-ink-500">
              {concept.definitionCount}
            </span>
          </button>
        ))}
      </div>

      <div className="flex-1 p-6">
        {comparison.loading && <p className="text-sm text-ink-500">Cargando…</p>}

        {comparison.data && (
          <>
            <div className="mb-5">
              <h2 className="font-serif text-2xl text-ink-50">{comparison.data.term}</h2>
              {comparison.data.description && (
                <p className="mt-1.5 max-w-3xl text-[13px] leading-relaxed text-ink-400">
                  {comparison.data.description}
                </p>
              )}
              <p className="mt-3 inline-flex items-center gap-1.5 rounded border border-fallacy-500/30 bg-fallacy-900/25 px-2.5 py-1 text-[11px] text-fallacy-300">
                <TriangleAlert className="size-3" />
                {readings.length} acepciones incompatibles
              </p>
            </div>

            {readings.length === 0 ? (
              <EmptyState title="Ningún autor ha fijado este término todavía." />
            ) : (
              <div
                className="grid gap-4"
                style={{
                  gridTemplateColumns: `repeat(auto-fit, minmax(min(100%, 20rem), 1fr))`,
                }}
              >
                {readings.map((reading) => (
                  <article
                    key={reading.id}
                    className="flex flex-col rounded-lg border border-ink-800 bg-ink-900/60"
                  >
                    <header className="border-b border-ink-800 bg-ink-850/60 px-4 py-3">
                      <h3 className="font-serif text-[15px] text-ink-50">
                        {reading.philosopherName}
                      </h3>
                      {reading.workTitle && (
                        <p className="mt-0.5 text-[11px] italic text-ink-500">
                          {reading.workTitle}
                        </p>
                      )}
                    </header>

                    <div className="flex-1 px-4 py-3.5">
                      <p className="prose-source text-[13.5px] leading-relaxed text-ink-100">
                        {reading.operationalDefinition}
                      </p>
                    </div>

                    {reading.notes && (
                      <footer className="border-t border-ink-800 px-4 py-2.5">
                        <p className="text-[11.5px] italic leading-relaxed text-ink-400">
                          {reading.notes}
                        </p>
                      </footer>
                    )}
                  </article>
                ))}
              </div>
            )}

            <p className="mt-6 flex items-start gap-1.5 text-[11px] leading-relaxed text-ink-600">
              <Columns3 className="mt-px size-3.5 shrink-0" />
              Cada definición está delimitada por autor y obra. Al leer, el inspector
              resuelve automáticamente cuál de estas acepciones gobierna el pasaje que
              tienes delante.
            </p>
          </>
        )}
      </div>
    </div>
  );
}
