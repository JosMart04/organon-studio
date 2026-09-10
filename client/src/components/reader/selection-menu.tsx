"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { BookMarked, Loader2, SquarePlus, X } from "lucide-react";
import { semantics } from "@/lib/api";
import type { ResolvedTerm } from "@/types/organon";
import { humanize } from "@/components/ui/badges";

/**
 * Menú flotante sobre el texto seleccionado. Dos gestos, los dos que se hacen
 * de verdad mientras se lee: convertir la frase en premisa de un argumento
 * nuevo, o preguntar qué significa ese término en boca de este autor.
 */
export function SelectionMenu({
  text,
  x,
  y,
  workId,
  philosopherId,
  passageId,
  onClose,
}: {
  text: string;
  x: number;
  y: number;
  workId: number;
  philosopherId: number;
  passageId: number;
  onClose: () => void;
}) {
  const router = useRouter();
  const [lookup, setLookup] = useState<ResolvedTerm>();
  const [looking, setLooking] = useState(false);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  const createPremise = () => {
    const params = new URLSearchParams({
      workId: String(workId),
      passageId: String(passageId),
      statement: text,
    });
    router.push(`/arguments/builder?${params}`);
  };

  const resolveTerm = async () => {
    setLooking(true);
    setNotFound(false);
    try {
      const result = await semantics.resolve(text, philosopherId, workId);
      if (result.scope === "SIN_DEFINICION" && result.rivalReadings.length === 0) {
        setNotFound(true);
      } else {
        setLookup(result);
      }
    } catch {
      setNotFound(true);
    } finally {
      setLooking(false);
    }
  };

  return (
    <div
      className="absolute z-20 -translate-x-1/2 -translate-y-full pb-2"
      style={{ left: x, top: y }}
    >
      <div className="w-max max-w-sm rounded-lg border border-ink-600 bg-ink-800 shadow-xl shadow-black/50">
        <div className="flex items-center gap-0.5 p-1">
          <button
            onClick={createPremise}
            className="flex items-center gap-1.5 rounded px-2.5 py-1.5 text-[11px] font-medium text-ink-200 transition-colors hover:bg-ink-700 hover:text-accent-300"
          >
            <SquarePlus className="size-3.5" />
            Crear premisa
          </button>
          <button
            onClick={resolveTerm}
            disabled={looking}
            className="flex items-center gap-1.5 rounded px-2.5 py-1.5 text-[11px] font-medium text-ink-200 transition-colors hover:bg-ink-700 hover:text-term-300 disabled:opacity-50"
          >
            {looking ? (
              <Loader2 className="size-3.5 animate-spin" />
            ) : (
              <BookMarked className="size-3.5" />
            )}
            Ver término
          </button>
          <button
            onClick={onClose}
            className="rounded px-1.5 py-1.5 text-ink-500 transition-colors hover:bg-ink-700 hover:text-ink-200"
          >
            <X className="size-3.5" />
          </button>
        </div>

        {notFound && (
          <p className="border-t border-ink-700 px-3 py-2 text-[11px] text-ink-500">
            «{truncate(text)}» no está en el glosario.
          </p>
        )}

        {lookup && (
          <div className="max-h-64 space-y-2 overflow-y-auto border-t border-ink-700 p-3">
            {lookup.definition ? (
              <div>
                <p className="text-[10px] uppercase tracking-wider text-ink-500">
                  {lookup.definition.philosopherName} · {humanize(lookup.scope)}
                </p>
                <p className="mt-1 text-[12px] leading-relaxed text-ink-100">
                  {lookup.definition.operationalDefinition}
                </p>
              </div>
            ) : (
              <p className="text-[11px] text-ink-500">
                Este autor no fija el término, pero otros sí:
              </p>
            )}

            {lookup.rivalReadings.length > 0 && (
              <div className="space-y-1.5 border-t border-ink-700 pt-2">
                <p className="text-[10px] uppercase tracking-wider text-fallacy-300">
                  Lecturas rivales
                </p>
                {lookup.rivalReadings.map((rival) => (
                  <div key={rival.id}>
                    <p className="text-[10px] text-ink-400">{rival.philosopherName}</p>
                    <p className="text-[11.5px] leading-relaxed text-ink-300">
                      {truncate(rival.operationalDefinition, 130)}
                    </p>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

function truncate(value: string, max = 40) {
  return value.length > max ? `${value.slice(0, max)}…` : value;
}
