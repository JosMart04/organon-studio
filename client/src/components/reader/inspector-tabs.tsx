"use client";

import { useState } from "react";
import Link from "next/link";
import { BookMarked, CircleAlert, SquareSigma } from "lucide-react";
import type { Argument, Passage, TermDefinition, Work } from "@/types/organon";
import { cn } from "@/lib/cn";
import { Latex } from "@/components/ui/latex";
import {
  EnthymemeBadge,
  ObjectionTypeBadge,
  PremiseTypeBadge,
  SoundStatusBadge,
} from "@/components/ui/badges";
import { TEXTOS, humanize } from "@/lib/vocabulario";
import { EmptyState } from "@/components/ui/panel";

type Tab = "glosario" | "argumentos" | "objeciones";

const TABS: { id: Tab; label: string; icon: typeof BookMarked }[] = [
  { id: "glosario", label: "Glosario", icon: BookMarked },
  { id: "argumentos", label: "Ideas", icon: SquareSigma },
  { id: "objeciones", label: "Críticas", icon: CircleAlert },
];

export function InspectorTabs({
  work,
  passage,
  glossary,
  glossaryLoading,
  argumentsForWork,
  argumentsLoading,
}: {
  work: Work | undefined;
  passage: Passage | undefined;
  glossary: TermDefinition[];
  glossaryLoading: boolean;
  argumentsForWork: Argument[];
  argumentsLoading: boolean;
}) {
  const [tab, setTab] = useState<Tab>("glosario");

  // Los argumentos anclados a este pasaje concreto primero: son el aparato
  // crítico de lo que el lector tiene delante, no de la obra en abstracto.
  const anchored = argumentsForWork.filter((a) => a.passageId === passage?.id);
  const rest = argumentsForWork.filter((a) => a.passageId !== passage?.id);
  const objections = argumentsForWork.flatMap((a) =>
    a.premises.flatMap((p) =>
      p.objections.map((o) => ({ objection: o, premise: p, argument: a })),
    ),
  );

  return (
    <div className="flex min-h-full flex-col">
      <div className="sticky top-0 z-10 flex border-b border-ink-800 bg-ink-900">
        {TABS.map(({ id, label, icon: Icon }) => {
          const count =
            id === "glosario"
              ? glossary.length
              : id === "argumentos"
                ? argumentsForWork.length
                : objections.length;
          return (
            <button
              key={id}
              onClick={() => setTab(id)}
              className={cn(
                "flex flex-1 items-center justify-center gap-1.5 border-b-2 px-2 py-2.5 text-[11px] font-medium transition-colors",
                tab === id
                  ? "border-accent-500 text-ink-50"
                  : "border-transparent text-ink-400 hover:text-ink-200",
              )}
            >
              <Icon className="size-3.5" />
              {label}
              <span className="tabular-nums text-ink-600">{count}</span>
            </button>
          );
        })}
      </div>

      <div className="flex-1 space-y-3 p-4">
        {tab === "glosario" && (
          <>
            <p className="text-[11px] leading-relaxed text-ink-500">
              Qué entiende por estas palabras{" "}
              <span className="text-ink-300">{work?.philosopherName}</span>. Otros
              autores usan las mismas con otro sentido.
            </p>
            {glossaryLoading && <p className="text-xs text-ink-500">Cargando…</p>}
            {!glossaryLoading && glossary.length === 0 && (
              <EmptyState title="Todavía no has anotado ningún término de este autor." />
            )}
            {glossary.map((definition) => (
              <article
                key={definition.id}
                className="rounded-md border border-ink-800 bg-ink-850/50 p-3"
              >
                <div className="flex items-baseline justify-between gap-2">
                  <h3 className="font-serif text-sm text-term-300">{definition.term}</h3>
                  <Link
                    href={`/glossary?conceptId=${definition.conceptId}`}
                    className="shrink-0 text-[10px] text-ink-500 transition-colors hover:text-accent-400"
                  >
                    comparar →
                  </Link>
                </div>
                <p className="mt-1.5 text-[12.5px] leading-relaxed text-ink-200">
                  {definition.operationalDefinition}
                </p>
                {definition.notes && (
                  <p className="mt-2 border-l-2 border-ink-700 pl-2 text-[11px] italic leading-relaxed text-ink-500">
                    {definition.notes}
                  </p>
                )}
              </article>
            ))}
          </>
        )}

        {tab === "argumentos" && (
          <>
            {argumentsLoading && <p className="text-xs text-ink-500">Cargando…</p>}
            {!argumentsLoading && argumentsForWork.length === 0 && (
              <EmptyState
                title="Todavía no has desmontado ninguna idea de este libro."
                hint="Selecciona una frase a la izquierda para empezar."
              />
            )}
            {anchored.length > 0 && (
              <p className="text-[11px] text-ink-500">
                Anclados a <span className="locator">{passage?.locator}</span>
              </p>
            )}
            {anchored.map((a) => (
              <ArgumentCard key={a.id} argument={a} />
            ))}
            {rest.length > 0 && anchored.length > 0 && (
              <p className="pt-1 text-[11px] text-ink-500">En otros pasajes de la obra</p>
            )}
            {rest.map((a) => (
              <ArgumentCard key={a.id} argument={a} muted />
            ))}
          </>
        )}

        {tab === "objeciones" && (
          <>
            <p className="text-[11px] leading-relaxed text-ink-500">
              Cada crítica señala la razón exacta que falla, no la idea entera.
            </p>
            {objections.length === 0 && (
              <EmptyState title="Ninguna crítica anotada en este libro." />
            )}
            {objections.map(({ objection, premise, argument }) => (
              <article
                key={objection.id}
                className="rounded-md border border-ink-800 bg-ink-850/50 p-3"
              >
                <div className="flex flex-wrap items-center gap-1.5">
                  <ObjectionTypeBadge type={objection.objectionType} />
                  <span className="font-mono text-[10px] text-ink-500">
                    P{premise.orderIndex + 1}
                  </span>
                  {premise.enthymeme && <EnthymemeBadge />}
                </div>
                <p className="mt-2 border-l-2 border-ink-700 pl-2 font-serif text-[12.5px] leading-relaxed text-ink-300">
                  {premise.statement}
                </p>
                <p className="mt-2 text-[12.5px] leading-relaxed text-ink-200">
                  {objection.explanation}
                </p>
                <Link
                  href={`/arguments/builder?argumentId=${argument.id}`}
                  className="mt-2 inline-block text-[10px] text-ink-500 transition-colors hover:text-accent-400"
                >
                  {argument.name} →
                </Link>
              </article>
            ))}
          </>
        )}
      </div>
    </div>
  );
}

function ArgumentCard({ argument, muted }: { argument: Argument; muted?: boolean }) {
  return (
    <article
      className={cn(
        "rounded-md border border-ink-800 bg-ink-850/50 p-3",
        muted && "opacity-70",
      )}
    >
      <div className="flex items-start justify-between gap-2">
        <Link
          href={`/arguments/builder?argumentId=${argument.id}`}
          className="font-serif text-sm leading-snug text-ink-100 transition-colors hover:text-accent-300"
        >
          {argument.name}
        </Link>
        <SoundStatusBadge status={argument.soundStatus} />
      </div>

      <p className="mt-1 font-mono text-[10px] text-ink-500">
        {humanize(argument.formalScheme)}
      </p>

      <ol className="mt-2 space-y-1">
        {argument.premises.map((p, i) => (
          <li key={p.id} className="flex gap-1.5 text-[12px] leading-snug">
            <span className="mt-px shrink-0 font-mono text-[10px] text-ink-600">
              {p.premiseType === "CONCLUSION" ? "C" : `P${i + 1}`}
            </span>
            <span className="min-w-0">
              <span className="text-ink-300">{p.statement}</span>
              {p.enthymeme && (
                <span className="ml-1.5 inline-block align-middle">
                  <EnthymemeBadge />
                </span>
              )}
            </span>
          </li>
        ))}
      </ol>

      {argument.latexFormalization && (
        // Colapsada tambien aqui: el aparato critico del lector no debe abrirse
        // con una formula delante.
        <details className="mt-2.5">
          <summary className="cursor-pointer list-none text-[10px] text-ink-600 transition-colors hover:text-ink-400">
            🔬 {TEXTOS.notacionFormal}
          </summary>
          <div className="mt-1.5 overflow-x-auto rounded bg-ink-900/80 px-2 py-1.5">
            <Latex expression={argument.latexFormalization} display className="text-ink-200" />
          </div>
        </details>
      )}

      <div className="mt-2 flex flex-wrap gap-1">
        {argument.premises
          .filter((p) => p.premiseType !== "EMPIRICA")
          .slice(0, 3)
          .map((p) => (
            <PremiseTypeBadge key={p.id} type={p.premiseType} />
          ))}
      </div>
    </article>
  );
}
