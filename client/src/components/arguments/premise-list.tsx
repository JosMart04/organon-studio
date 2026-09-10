"use client";

import {
  DndContext,
  KeyboardSensor,
  PointerSensor,
  closestCenter,
  useSensor,
  useSensors,
  type DragEndEvent,
} from "@dnd-kit/core";
import { restrictToVerticalAxis } from "@dnd-kit/modifiers";
import {
  SortableContext,
  arrayMove,
  sortableKeyboardCoordinates,
  useSortable,
  verticalListSortingStrategy,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { EyeOff, GripVertical, TriangleAlert, Trash2 } from "lucide-react";
import { PREMISE_TYPES, type PremiseType } from "@/types/organon";
import { cn } from "@/lib/cn";
import { TEXTOS, humanize } from "@/lib/vocabulario";

/** Razón en edición. `key` es estable aunque todavía no exista en el servidor. */
export interface DraftPremise {
  key: string;
  id: number | null;
  statement: string;
  enthymeme: boolean;
  premiseType: PremiseType;
}

/**
 * ¿Hay una conclusión y no está al final?
 *
 * El servidor también lo detecta al revisar, pero eso llega tarde: el error se
 * comete arrastrando, y el aviso tiene que aparecer en ese momento. Se calcula
 * en cada render a partir de la lista, sin estado ni efecto.
 */
export function conclusionFueraDeSitio(premises: DraftPremise[]): boolean {
  const indice = premises.findIndex((p) => p.premiseType === "CONCLUSION");
  return indice >= 0 && indice !== premises.length - 1;
}

export function PremiseList({
  premises,
  onChange,
}: {
  premises: DraftPremise[];
  onChange: (next: DraftPremise[]) => void;
}) {
  const sensors = useSensors(
    // 6px de holgura para que un clic en el campo de texto no inicie un arrastre.
    useSensor(PointerSensor, { activationConstraint: { distance: 6 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  );

  const handleDragEnd = ({ active, over }: DragEndEvent) => {
    if (!over || active.id === over.id) return;
    const from = premises.findIndex((p) => p.key === active.id);
    const to = premises.findIndex((p) => p.key === over.id);
    if (from < 0 || to < 0) return;
    onChange(arrayMove(premises, from, to));
  };

  const update = (key: string, patch: Partial<DraftPremise>) =>
    onChange(premises.map((p) => (p.key === key ? { ...p, ...patch } : p)));

  const remove = (key: string) => onChange(premises.filter((p) => p.key !== key));

  // La numeración salta la conclusión: no es una razón más, es a donde llegan.
  let numero = 0;
  const etiquetas = premises.map((p) =>
    p.premiseType === "CONCLUSION" ? "C" : `P${++numero}`,
  );

  const avisoConclusion = conclusionFueraDeSitio(premises);

  return (
    <>
      <DndContext
        sensors={sensors}
        collisionDetection={closestCenter}
        onDragEnd={handleDragEnd}
        // Solo el eje vertical: una razón nunca se mueve de lado.
        modifiers={[restrictToVerticalAxis]}
      >
        <SortableContext
          items={premises.map((p) => p.key)}
          strategy={verticalListSortingStrategy}
        >
          <ol className="space-y-2">
            {premises.map((premise, i) => (
              <SortablePremise
                key={premise.key}
                premise={premise}
                label={etiquetas[i]}
                onUpdate={(patch) => update(premise.key, patch)}
                onRemove={() => remove(premise.key)}
              />
            ))}
          </ol>
        </SortableContext>
      </DndContext>

      {avisoConclusion && (
        <p
          role="status"
          className="mt-3 flex items-center gap-2 rounded-md border border-accent-500/40 bg-accent-900/40 px-3 py-2 text-[12px] text-accent-300"
        >
          <TriangleAlert className="size-4 shrink-0" />
          {TEXTOS.conclusionFueraDeSitio}
        </p>
      )}
    </>
  );
}

function SortablePremise({
  premise,
  label,
  onUpdate,
  onRemove,
}: {
  premise: DraftPremise;
  label: string;
  onUpdate: (patch: Partial<DraftPremise>) => void;
  onRemove: () => void;
}) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } =
    useSortable({ id: premise.key });

  const esConclusion = premise.premiseType === "CONCLUSION";

  return (
    <li
      ref={setNodeRef}
      style={{ transform: CSS.Transform.toString(transform), transition }}
      className={cn(
        "rounded-md border bg-ink-850/60 transition-shadow",
        isDragging && "z-10 shadow-lg shadow-black/50",
        esConclusion
          ? "border-accent-500/40"
          : premise.enthymeme
            ? "border-dashed border-accent-500/25"
            : "border-ink-800",
      )}
    >
      <div className="flex items-start gap-2 p-2.5">
        <button
          type="button"
          {...attributes}
          {...listeners}
          className="mt-1 cursor-grab touch-none rounded p-0.5 text-ink-600 transition-colors hover:text-ink-300 active:cursor-grabbing"
          aria-label="Reordenar"
        >
          <GripVertical className="size-4" />
        </button>

        <span
          className={cn(
            "mt-1 w-7 shrink-0 text-center font-mono text-[11px]",
            esConclusion ? "text-accent-400" : "text-ink-500",
          )}
          title={esConclusion ? "Conclusión" : "Razón"}
        >
          {label}
        </span>

        <div className="min-w-0 flex-1 space-y-2">
          <textarea
            value={premise.statement}
            onChange={(e) => onUpdate({ statement: e.target.value })}
            rows={2}
            placeholder={
              esConclusion
                ? "¿A qué conclusión llega el autor?"
                : "¿Qué razón da el autor?"
            }
            className="w-full resize-y rounded border border-ink-700 bg-ink-900 px-2.5 py-1.5 font-serif text-[13px] leading-relaxed text-ink-100 outline-none placeholder:text-ink-600 focus:border-accent-500"
          />

          <div className="flex flex-wrap items-center gap-2">
            <select
              value={premise.premiseType}
              onChange={(e) => onUpdate({ premiseType: e.target.value as PremiseType })}
              className="rounded border border-ink-700 bg-ink-900 px-2 py-1 text-[11px] text-ink-200 outline-none focus:border-accent-500"
              title="Qué clase de razón es"
            >
              {PREMISE_TYPES.map((t) => (
                <option key={t} value={t}>
                  {humanize(t)}
                </option>
              ))}
            </select>

            <button
              type="button"
              onClick={() => onUpdate({ enthymeme: !premise.enthymeme })}
              title={TEXTOS.supuestoImplicitoAyuda}
              className={cn(
                "inline-flex items-center gap-1.5 rounded border px-2 py-1 text-[11px] font-medium transition-colors",
                premise.enthymeme
                  ? "border-accent-500/50 bg-accent-900 text-accent-300"
                  : "border-ink-700 text-ink-400 hover:border-ink-600 hover:text-ink-200",
              )}
            >
              <EyeOff className="size-3" />
              {TEXTOS.supuestoImplicito}
            </button>

            <button
              type="button"
              onClick={onRemove}
              className="ml-auto rounded p-1 text-ink-600 transition-colors hover:bg-fallacy-900/60 hover:text-fallacy-300"
              aria-label="Eliminar"
            >
              <Trash2 className="size-3.5" />
            </button>
          </div>
        </div>
      </div>
    </li>
  );
}
