"use client";

import { useState } from "react";
import { CircleAlert, Plus, Trash2 } from "lucide-react";
import { argumentsApi } from "@/lib/api";
import { OBJECTION_TYPES, type Argument, type ObjectionType } from "@/types/organon";
import { Button, Panel } from "@/components/ui/panel";
import { EnthymemeBadge, ObjectionTypeBadge, humanize } from "@/components/ui/badges";

/**
 * Anclaje de objeciones. Se elige la premisa concreta que cede, no el
 * argumento entero: una crítica que no señala el eslabón exacto no es
 * utilizable.
 */
export function ObjectionPanel({
  argument,
  onChanged,
}: {
  argument: Argument;
  onChanged: () => void | Promise<void>;
}) {
  const [premiseId, setPremiseId] = useState<number | "">("");
  const [type, setType] = useState<ObjectionType>("CONTRAEJEMPLO");
  const [explanation, setExplanation] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>();

  const anchored = argument.premises.flatMap((p) =>
    p.objections.map((o) => ({ objection: o, premise: p })),
  );

  const submit = async () => {
    if (premiseId === "" || !explanation.trim()) return;
    setBusy(true);
    setError(undefined);
    try {
      await argumentsApi.addObjection(Number(premiseId), {
        objectionType: type,
        explanation: explanation.trim(),
      });
      setExplanation("");
      await onChanged();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  };

  const remove = async (id: number) => {
    setBusy(true);
    try {
      await argumentsApi.removeObjection(id);
      await onChanged();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <Panel title="Objeciones ancladas">
      {anchored.length === 0 ? (
        <p className="text-[12px] leading-relaxed text-ink-500">
          Ninguna todavía. Los entimemas sin objeción son el sitio por donde el
          argumento suele ceder: conviene atacarlos antes de darlo por sólido.
        </p>
      ) : (
        <ul className="mb-4 space-y-2">
          {anchored.map(({ objection, premise }) => (
            <li
              key={objection.id}
              className="rounded border border-ink-800 bg-ink-950 p-2.5"
            >
              <div className="flex flex-wrap items-center gap-1.5">
                <ObjectionTypeBadge type={objection.objectionType} />
                <span className="font-mono text-[10px] text-ink-500">
                  P{premise.orderIndex + 1}
                </span>
                {premise.enthymeme && <EnthymemeBadge />}
                <button
                  onClick={() => remove(objection.id)}
                  disabled={busy}
                  className="ml-auto rounded p-1 text-ink-600 transition-colors hover:bg-fallacy-900/60 hover:text-fallacy-300 disabled:opacity-40"
                  aria-label="Eliminar objeción"
                >
                  <Trash2 className="size-3" />
                </button>
              </div>
              <p className="mt-1.5 text-[12px] leading-relaxed text-ink-200">
                {objection.explanation}
              </p>
            </li>
          ))}
        </ul>
      )}

      <div className="space-y-2 border-t border-ink-800 pt-3">
        <div className="flex flex-wrap gap-2">
          <select
            value={premiseId}
            onChange={(e) => setPremiseId(e.target.value ? Number(e.target.value) : "")}
            className="min-w-0 flex-1 rounded border border-ink-700 bg-ink-900 px-2 py-1.5 text-[11px] text-ink-100 outline-none focus:border-accent-500"
          >
            <option value="">Premisa que cede…</option>
            {argument.premises.map((p, i) => (
              <option key={p.id} value={p.id}>
                {p.premiseType === "CONCLUSION" ? "C" : `P${i + 1}`}
                {p.enthymeme ? " ⟨entimema⟩" : ""} — {p.statement.slice(0, 60)}
              </option>
            ))}
          </select>

          <select
            value={type}
            onChange={(e) => setType(e.target.value as ObjectionType)}
            className="rounded border border-ink-700 bg-ink-900 px-2 py-1.5 text-[11px] text-ink-100 outline-none focus:border-accent-500"
          >
            {OBJECTION_TYPES.map((t) => (
              <option key={t} value={t}>
                {humanize(t)}
              </option>
            ))}
          </select>
        </div>

        <textarea
          value={explanation}
          onChange={(e) => setExplanation(e.target.value)}
          rows={2}
          placeholder="En qué falla exactamente esa premisa…"
          className="w-full resize-y rounded border border-ink-700 bg-ink-900 px-2.5 py-1.5 text-[12px] leading-relaxed text-ink-100 outline-none placeholder:text-ink-600 focus:border-accent-500"
        />

        {error && <p className="text-[11px] text-fallacy-300">{error}</p>}

        <Button
          onClick={submit}
          disabled={busy || premiseId === "" || !explanation.trim()}
        >
          <Plus className="size-3" /> Anclar objeción
        </Button>
        <p className="flex items-start gap-1.5 pt-1 text-[10px] leading-relaxed text-ink-600">
          <CircleAlert className="mt-px size-3 shrink-0" />
          Falacia formal, petición de principio y falsa dicotomía invalidan la
          inferencia; el resto solo la debilita.
        </p>
      </div>
    </Panel>
  );
}
