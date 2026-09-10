"use client";

import { Handle, Position, type NodeProps } from "@xyflow/react";
import { BookText, SquareSigma, User } from "lucide-react";
import { cn } from "@/lib/cn";
import { humanize } from "@/components/ui/badges";

const HANDLE = "!size-1.5 !border-0 !bg-ink-600";

export function PhilosopherNode({ data }: NodeProps) {
  const d = data as { label: string; epoch: string; school: string };
  return (
    <div className="w-56 rounded-lg border border-ink-600 bg-ink-800 px-3.5 py-2.5 shadow-lg shadow-black/40">
      <Handle type="source" position={Position.Bottom} className={HANDLE} />
      <div className="flex items-center gap-2">
        <User className="size-3.5 shrink-0 text-ink-400" strokeWidth={1.75} />
        <span className="truncate font-serif text-[14px] text-ink-50">{d.label}</span>
      </div>
      <p className="mt-1 truncate text-[10px] text-ink-400">
        {humanize(d.epoch)}
        {d.school ? ` · ${d.school}` : ""}
      </p>
    </div>
  );
}

export function WorkNode({ data }: NodeProps) {
  const d = data as { label: string; originalYear: number | string; coreThesis: string };
  return (
    <div className="w-60 rounded-lg border border-ink-700 bg-ink-850 px-3.5 py-2.5 shadow-lg shadow-black/40">
      <Handle type="target" position={Position.Top} className={HANDLE} />
      <Handle type="source" position={Position.Bottom} className={HANDLE} />
      <Handle type="source" position={Position.Right} id="adversary" className={HANDLE} />
      <Handle type="target" position={Position.Left} id="adversary" className={HANDLE} />
      <div className="flex items-start gap-2">
        <BookText className="mt-0.5 size-3.5 shrink-0 text-ink-500" strokeWidth={1.75} />
        <div className="min-w-0">
          <p className="font-serif text-[13px] leading-snug text-ink-100">{d.label}</p>
          {d.originalYear !== "" && (
            <p className="mt-0.5 font-mono text-[10px] text-ink-500">{d.originalYear}</p>
          )}
        </div>
      </div>
    </div>
  );
}

export function ArgumentNode({ data }: NodeProps) {
  const d = data as {
    label: string;
    formalScheme: string;
    soundStatus: "PENDIENTE" | "SOLIDO" | "FALAZ";
    premiseCount: number;
    enthymemeCount: number;
  };

  const statusRing = {
    SOLIDO: "border-valid-500/50",
    FALAZ: "border-fallacy-500/50",
    PENDIENTE: "border-accent-500/40",
  }[d.soundStatus];

  const statusText = {
    SOLIDO: "text-valid-300",
    FALAZ: "text-fallacy-300",
    PENDIENTE: "text-accent-300",
  }[d.soundStatus];

  return (
    <div
      className={cn(
        "w-64 rounded-lg border-2 bg-ink-900 px-3.5 py-3 shadow-lg shadow-black/40",
        statusRing,
      )}
    >
      <Handle type="target" position={Position.Top} className={HANDLE} />
      <Handle type="source" position={Position.Bottom} className={HANDLE} />
      <Handle type="source" position={Position.Right} id="dialectic" className={HANDLE} />
      <Handle type="target" position={Position.Left} id="dialectic" className={HANDLE} />

      <div className="flex items-start gap-2">
        <SquareSigma className="mt-0.5 size-3.5 shrink-0 text-accent-400" strokeWidth={1.75} />
        <p className="font-serif text-[13px] leading-snug text-ink-50">{d.label}</p>
      </div>

      <p className="mt-1.5 font-mono text-[9.5px] uppercase tracking-wider text-ink-500">
        {humanize(d.formalScheme)}
      </p>

      <div className="mt-2 flex items-center gap-2 border-t border-ink-800 pt-2 text-[10px]">
        <span className={cn("font-medium uppercase tracking-wider", statusText)}>
          {humanize(d.soundStatus)}
        </span>
        <span className="ml-auto font-mono text-ink-500">
          {d.premiseCount}P
          {d.enthymemeCount > 0 && (
            <span className="ml-1 text-accent-400" title="premisas implícitas">
              {d.enthymemeCount}E
            </span>
          )}
        </span>
      </div>
    </div>
  );
}

export const NODE_TYPES = {
  philosopher: PhilosopherNode,
  work: WorkNode,
  argument: ArgumentNode,
};
