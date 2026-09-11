"use client";

import { useState } from "react";
import type { Node } from "@xyflow/react";
import { FileCode, ImageDown } from "lucide-react";
import { exportarGrafo, type FormatoImagen } from "@/lib/graph-export";
import { Button } from "@/components/ui/panel";

/** Botones de la cabecera del mapa para guardarlo como PNG o SVG. */
export function ExportarMapa({
  nodos,
  epoca,
  palabra,
  disabled,
}: {
  nodos: Node[];
  epoca?: string;
  palabra?: string;
  disabled?: boolean;
}) {
  const [enCurso, setEnCurso] = useState<FormatoImagen | null>(null);
  const [error, setError] = useState<string | null>(null);

  const exportar = async (formato: FormatoImagen) => {
    // Solo hay un mapa en la página; su viewport es lo que se captura.
    const lienzo = document.querySelector<HTMLElement>(".react-flow__viewport");
    if (!lienzo) return;

    setEnCurso(formato);
    setError(null);
    try {
      await exportarGrafo({ lienzo, nodos, formato, epoca, palabra });
    } catch (e) {
      console.error(e);
      setError("No se ha podido guardar el mapa. Inténtalo de nuevo.");
    } finally {
      setEnCurso(null);
    }
  };

  const bloqueado = disabled || nodos.length === 0 || enCurso !== null;

  return (
    <>
      {error && (
        <span role="alert" className="text-[11px] text-fallacy-300">
          {error}
        </span>
      )}
      <Button
        onClick={() => void exportar("png")}
        disabled={bloqueado}
        title="Guarda el mapa completo, con su leyenda, como imagen"
        className="inline-flex items-center gap-1.5"
      >
        <ImageDown className="size-3.5" strokeWidth={1.75} />
        {enCurso === "png" ? "Guardando…" : "Imagen PNG"}
      </Button>
      <Button
        onClick={() => void exportar("svg")}
        disabled={bloqueado}
        title="Nítido a cualquier tamaño; se abre en el navegador"
        className="inline-flex items-center gap-1.5"
      >
        <FileCode className="size-3.5" strokeWidth={1.75} />
        {enCurso === "svg" ? "Guardando…" : "SVG"}
      </Button>
    </>
  );
}
