"use client";

import { useCallback, useState, type RefObject } from "react";
import { limpiarSeleccion } from "@/lib/limpiar-seleccion";

export interface Seleccion {
  texto: string;
  /** Centro superior de la selección, relativo al contenedor: ahí se ancla el menú. */
  x: number;
  y: number;
  /** Página del PDF donde empieza la selección, si la hay. */
  pagina?: number;
}

/** Texto seleccionado dentro de `hostRef`, listo para guardarse. */
export function useSeleccion(hostRef: RefObject<HTMLElement | null>) {
  const [seleccion, setSeleccion] = useState<Seleccion>();

  const capturar = useCallback(() => {
    const host = hostRef.current;
    const sel = window.getSelection();
    const texto = limpiarSeleccion(sel?.toString() ?? "");
    if (!host || !texto || !sel?.rangeCount) {
      setSeleccion(undefined);
      return;
    }

    const rango = sel.getRangeAt(0);
    if (!host.contains(rango.commonAncestorContainer)) {
      setSeleccion(undefined);
      return;
    }

    const caja = rango.getBoundingClientRect();
    const base = host.getBoundingClientRect();
    const inicio =
      rango.startContainer instanceof Element
        ? rango.startContainer
        : rango.startContainer.parentElement;
    const pagina = Number(inicio?.closest("[data-pagina]")?.getAttribute("data-pagina"));

    setSeleccion({
      texto,
      x: caja.left - base.left + caja.width / 2,
      y: caja.top - base.top,
      pagina: pagina > 0 ? pagina : undefined,
    });
  }, [hostRef]);

  const limpiar = useCallback(() => setSeleccion(undefined), []);

  return { seleccion, capturar, limpiar };
}
