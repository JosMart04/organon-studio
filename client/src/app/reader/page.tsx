"use client";

import { useCallback, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { ChevronLeft, ChevronRight, Download, Quote, SquarePen } from "lucide-react";
import { argumentsApi, corpus, exports, semantics } from "@/lib/api";
import type { Passage, Work } from "@/types/organon";
import { useAsync } from "@/lib/use-async";
import { useCorpusVersion } from "@/lib/corpus-refresh";
import { cn } from "@/lib/cn";
import { Button, ErrorState, PageHeader } from "@/components/ui/panel";
import { InspectorTabs } from "@/components/reader/inspector-tabs";
import { SelectionMenu } from "@/components/reader/selection-menu";
import { SocraticButton, SocraticPanel } from "@/components/ai/socratic-panel";

export default function ReaderPage() {
  const [asistenteAbierto, setAsistenteAbierto] = useState(false);
  const corpusVersion = useCorpusVersion();
  const works = useAsync(() => corpus.listWorks(), [corpusVersion]);

  // Obra y pasaje viajan juntos: cambiar de obra debe reiniciar el pasaje, y
  // guardarlos en un solo estado lo consigue sin un efecto que los sincronice.
  const [picked, setPicked] = useState<{ workId: number; index: number }>();

  // Mientras el usuario no elija, manda la primera obra del catálogo.
  const workId = picked?.workId ?? works.data?.[0]?.id;
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

  if (works.error) {
    return (
      <div className="p-6">
        <ErrorState message={works.error} />
      </div>
    );
  }

  return (
    <div className="flex flex-col md:h-screen">
      <PageHeader
        title="Leer"
        subtitle="El texto a la izquierda, lo que vas descubriendo a la derecha."
        actions={
          <>
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
        <SourcePanel
          work={activeWork}
          passages={passages.data ?? []}
          index={passageIndex}
          onIndexChange={(index) =>
            workId !== undefined && setPicked({ workId, index })
          }
          loading={passages.loading}
        />

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
        onClose={() => setAsistenteAbierto(false)}
        text={passage?.textContent ?? ""}
        author={activeWork?.philosopherName}
        workTitle={activeWork?.title}
      />
    </div>
  );
}

function SourcePanel({
  work,
  passages,
  index,
  onIndexChange,
  loading,
}: {
  work: Work | undefined;
  passages: Passage[];
  index: number;
  onIndexChange: (i: number) => void;
  loading: boolean;
}) {
  const passage = passages[index];

  return (
    <section className="relative flex min-h-0 flex-col">
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
            <p className="text-sm text-ink-500">
              Este libro todavía no tiene ninguna idea anotada. Usa «+ Idea» en el
              menú de la izquierda.
            </p>
          )}

          {passage && work && (
            // La clave remonta el bloque al cambiar de pasaje, lo que descarta
            // la selección pendiente sin necesidad de un efecto que la limpie.
            <SelectableText key={passage.id} passage={passage} work={work} />
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

function SelectableText({ passage, work }: { passage: Passage; work: Work }) {
  const hostRef = useRef<HTMLDivElement>(null);
  const [selection, setSelection] = useState<{ text: string; x: number; y: number }>();

  const captureSelection = useCallback(() => {
    const sel = window.getSelection();
    const text = sel?.toString().trim() ?? "";
    if (!text || !sel?.rangeCount || !hostRef.current) {
      setSelection(undefined);
      return;
    }
    const range = sel.getRangeAt(0);
    if (!hostRef.current.contains(range.commonAncestorContainer)) {
      setSelection(undefined);
      return;
    }
    const rect = range.getBoundingClientRect();
    const host = hostRef.current.getBoundingClientRect();
    setSelection({
      text,
      x: rect.left - host.left + rect.width / 2,
      y: rect.top - host.top,
    });
  }, []);

  return (
    <div
      ref={hostRef}
      className="relative"
      onMouseUp={captureSelection}
      onKeyUp={captureSelection}
    >
      <div className="prose-source select-text">
        {passage.textContent.split("\n").map((paragraph, i) => (
          <p key={i}>{paragraph}</p>
        ))}
      </div>

      {selection && (
        <SelectionMenu
          text={selection.text}
          x={selection.x}
          y={selection.y}
          workId={work.id}
          philosopherId={work.philosopherId}
          passageId={passage.id}
          onClose={() => setSelection(undefined)}
        />
      )}
    </div>
  );
}
