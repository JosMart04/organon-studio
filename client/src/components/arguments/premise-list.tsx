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
import { restrictToParentElement, restrictToVerticalAxis } from "@dnd-kit/modifiers";
import {
  SortableContext,
  arrayMove,
  sortableKeyboardCoordinates,
  useSortable,
  verticalListSortingStrategy,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { EyeOff, GripVertical, Trash2 } from "lucide-react";
import { PREMISE_TYPES, type PremiseType } from "@/types/organon";
import { cn } from "@/lib/cn";
import { humanize } from "@/components/ui/badges";

/** Premisa en edición. `key` es estable aunque la premisa aún no exista en el servidor. */
export interface DraftPremise {
  key: string;
  id: number | null;
  statement: string;
  enthymeme: boolean;
  premiseType: PremiseType;
}

export function PremiseList({
  premises,
  onChange,
}: {
  premises: DraftPremise[];
  onChange: (next: DraftPremise[]) => void;
}) {
  const sensors = useSensors(
    // 6px de holgura para que un clic en el textarea no inicie un arrastre.
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

  // La numeración salta la conclusión: en forma estándar C no es una premisa más.
  let premiseNumber = 0;
  const labels = premises.map((p) =>
    p.premiseType === "CONCLUSION" ? "C" : `P${++premiseNumber}`,
  );

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={closestCenter}
      onDragEnd={handleDragEnd}
      modifiers={[restrictToVerticalAxis, restrictToParentElement]}
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
              label={labels[i]}
              onUpdate={(patch) => update(premise.key, patch)}
              onRemove={() => remove(premise.key)}
            />
          ))}
        </ol>
      </SortableContext>
    </DndContext>
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

  const isConclusion = premise.premiseType === "CONCLUSION";

  return (
    <li
      ref={setNodeRef}
      style={{ transform: CSS.Transform.toString(transform), transition }}
      className={cn(
        "rounded-md border bg-ink-850/60 transition-shadow",
        isDragging && "z-10 shadow-lg shadow-black/50",
        isConclusion
          ? "border-accent-500/40"
          : premise.enthymeme
            ? "border-accent-500/25 border-dashed"
            : "border-ink-800",
      )}
    >
      <div className="flex items-start gap-2 p-2.5">
        <button
          {...attributes}
          {...listeners}
          className="mt-1 cursor-grab touch-none rounded p-0.5 text-ink-600 transition-colors hover:text-ink-300 active:cursor-grabbing"
          aria-label="Reordenar premisa"
        >
          <GripVertical className="size-4" />
        </button>

        <span
          className={cn(
            "mt-1 w-7 shrink-0 text-center font-mono text-[11px]",
            isConclusion ? "text-accent-400" : "text-ink-500",
          )}
        >
          {label}
        </span>

        <div className="min-w-0 flex-1 space-y-2">
          <textarea
            value={premise.statement}
            onChange={(e) => onUpdate({ statement: e.target.value })}
            rows={2}
            placeholder="Enuncia la premisa…"
            className="w-full resize-y rounded border border-ink-700 bg-ink-900 px-2.5 py-1.5 font-serif text-[13px] leading-relaxed text-ink-100 outline-none placeholder:text-ink-600 focus:border-accent-500"
          />

          <div className="flex flex-wrap items-center gap-2">
            <select
              value={premise.premiseType}
              onChange={(e) => onUpdate({ premiseType: e.target.value as PremiseType })}
              className="rounded border border-ink-700 bg-ink-900 px-2 py-1 text-[11px] text-ink-200 outline-none focus:border-accent-500"
            >
              {PREMISE_TYPES.map((t) => (
                <option key={t} value={t}>
                  {humanize(t)}
                </option>
              ))}
            </select>

            <button
              onClick={() => onUpdate({ enthymeme: !premise.enthymeme })}
              title="Marca la premisa que el autor no escribió pero su inferencia necesita"
              className={cn(
                "inline-flex items-center gap-1.5 rounded border px-2 py-1 text-[11px] font-medium transition-colors",
                premise.enthymeme
                  ? "border-accent-500/50 bg-accent-900 text-accent-300"
                  : "border-ink-700 text-ink-400 hover:border-ink-600 hover:text-ink-200",
              )}
            >
              <EyeOff className="size-3" />
              Entimema
            </button>

            <button
              onClick={onRemove}
              className="ml-auto rounded p-1 text-ink-600 transition-colors hover:bg-fallacy-900/60 hover:text-fallacy-300"
              aria-label="Eliminar premisa"
            >
              <Trash2 className="size-3.5" />
            </button>
          </div>
        </div>
      </div>
    </li>
  );
}
