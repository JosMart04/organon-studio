"use client";

import { useEffect, useRef } from "react";
import { X } from "lucide-react";
import { cn } from "@/lib/cn";

/**
 * Modal sobre `<dialog>` nativo.
 *
 * Usar el elemento del navegador en vez de montar un `div` con `position:
 * fixed` no es purismo: `showModal()` trae de fábrica el atrapado del foco, el
 * cierre con Escape, el fondo inerte y el papel de `aria-modal`. Todo eso,
 * hecho a mano, es donde se cuelan los fallos de accesibilidad.
 */
export function Dialog({
  open,
  onClose,
  title,
  description,
  children,
  footer,
  wide,
}: {
  open: boolean;
  onClose: () => void;
  title: string;
  description?: string;
  children: React.ReactNode;
  footer?: React.ReactNode;
  wide?: boolean;
}) {
  const ref = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) {
      dialog.showModal();
    } else if (!open && dialog.open) {
      dialog.close();
    }
  }, [open]);

  return (
    <dialog
      ref={ref}
      // `cancel` cubre la tecla Escape, que de lo contrario cerraria el
      // elemento sin avisar al estado de React.
      onCancel={(e) => {
        e.preventDefault();
        onClose();
      }}
      onClose={onClose}
      // Clic en el fondo: el backdrop forma parte del propio <dialog>, asi que
      // el evento llega con el dialogo como target y no con su contenido.
      onClick={(e) => {
        if (e.target === ref.current) onClose();
      }}
      className={cn(
        "m-auto w-[min(92vw,32rem)] rounded-xl border border-ink-700 bg-ink-900 p-0 text-ink-100 shadow-2xl shadow-black/60 backdrop:bg-black/60 backdrop:backdrop-blur-sm",
        wide && "w-[min(94vw,46rem)]",
      )}
    >
      <header className="flex items-start justify-between gap-3 border-b border-ink-800 px-5 py-3.5">
        <div className="min-w-0">
          <h2 className="font-serif text-[17px] text-ink-50">{title}</h2>
          {description && (
            <p className="mt-0.5 text-[12px] leading-relaxed text-ink-400">{description}</p>
          )}
        </div>
        <button
          type="button"
          onClick={onClose}
          aria-label="Cerrar"
          className="shrink-0 rounded p-1 text-ink-500 transition-colors hover:bg-ink-800 hover:text-ink-200"
        >
          <X className="size-4" />
        </button>
      </header>

      <div className="max-h-[70vh] overflow-y-auto px-5 py-4">{children}</div>

      {footer && (
        <footer className="flex items-center justify-end gap-2 border-t border-ink-800 px-5 py-3">
          {footer}
        </footer>
      )}
    </dialog>
  );
}

/** Campo de formulario con su etiqueta y su pista, para no repetirlo doce veces. */
export function Field({
  label,
  hint,
  children,
  className,
}: {
  label: string;
  hint?: string;
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <label className={cn("block", className)}>
      <span className="mb-1 block text-[11px] font-medium text-ink-300">{label}</span>
      {children}
      {hint && <span className="mt-1 block text-[10.5px] text-ink-500">{hint}</span>}
    </label>
  );
}

export const CAMPO =
  "w-full rounded border border-ink-700 bg-ink-950 px-2.5 py-1.5 text-[13px] text-ink-100 outline-none placeholder:text-ink-600 focus:border-accent-500";

export const CAMPO_SERIF = `${CAMPO} font-serif leading-relaxed`;
