"use client";

import { useRef, useState } from "react";
import { FileText, FileUp } from "lucide-react";
import type { ResumenDocumento, TipoDocumento } from "@/lib/local-documents";
import { cn } from "@/lib/cn";
import { Button } from "@/components/ui/panel";

const ACEPTA = ".pdf,.md,.markdown,.txt,application/pdf,text/markdown,text/plain";

const TIPOS: Record<TipoDocumento, string> = {
  pdf: "PDF",
  markdown: "Markdown",
  texto: "Texto",
};

/** Punto de entrada del lector de documentos: arrastrar, elegir o seguir con uno reciente. */
export function DocumentDropZone({
  recientes,
  abriendo,
  error,
  onFichero,
  onReciente,
  onBorrarRecientes,
}: {
  recientes: ResumenDocumento[];
  abriendo: boolean;
  error?: string;
  onFichero: (fichero: File) => void;
  onReciente: (documento: ResumenDocumento) => void;
  onBorrarRecientes: () => void;
}) {
  const entrada = useRef<HTMLInputElement>(null);
  const [encima, setEncima] = useState(false);

  return (
    <div className="mx-auto max-w-[42rem] space-y-6 px-6 py-8">
      <div
        onDragOver={(e) => {
          e.preventDefault();
          setEncima(true);
        }}
        onDragLeave={(e) => {
          // Pasar por encima de un hijo también dispara dragleave en el padre.
          if (!e.currentTarget.contains(e.relatedTarget as Node | null)) setEncima(false);
        }}
        onDrop={(e) => {
          e.preventDefault();
          setEncima(false);
          const fichero = e.dataTransfer.files[0];
          if (fichero) onFichero(fichero);
        }}
        className={cn(
          "rounded-lg border-2 border-dashed px-6 py-10 text-center transition-colors",
          encima ? "border-accent-500 bg-accent-900/30" : "border-ink-700 bg-ink-900/40",
        )}
      >
        <FileUp className="mx-auto size-8 text-ink-500" strokeWidth={1.5} />
        <p className="mt-3 font-serif text-[15px] text-ink-100">
          Arrastra aquí el libro que estás leyendo
        </p>
        <p className="mt-1 text-[12px] leading-relaxed text-ink-500">
          PDF, Markdown (.md) o texto (.txt). Se abre en tu navegador: no se sube a ningún sitio.
        </p>
        <Button
          variant="primary"
          className="mt-4"
          disabled={abriendo}
          onClick={() => entrada.current?.click()}
        >
          {abriendo ? "Abriendo…" : "Elegir un fichero"}
        </Button>
        <input
          ref={entrada}
          type="file"
          accept={ACEPTA}
          hidden
          onChange={(e) => {
            const fichero = e.target.files?.[0];
            // Vaciarlo permite volver a elegir el mismo fichero.
            e.target.value = "";
            if (fichero) onFichero(fichero);
          }}
        />
      </div>

      {error && (
        <p
          role="alert"
          className="rounded-md border border-fallacy-500/40 bg-fallacy-900/30 px-3 py-2.5 text-[12px] text-fallacy-300"
        >
          {error}
        </p>
      )}

      {recientes.length > 0 && (
        <section>
          <div className="flex items-center justify-between gap-3">
            <h3 className="text-[10px] font-semibold uppercase tracking-[0.12em] text-ink-400">
              Seguir leyendo
            </h3>
            <button
              type="button"
              onClick={onBorrarRecientes}
              title="Olvida los documentos guardados en este navegador. Tus ficheros originales no se tocan."
              className="text-[11px] text-ink-500 transition-colors hover:text-fallacy-300"
            >
              Borrar recientes
            </button>
          </div>
          <ul className="mt-2 space-y-1.5">
            {recientes.map((documento) => (
              <li key={documento.id}>
                <button
                  type="button"
                  onClick={() => onReciente(documento)}
                  className="flex w-full items-center gap-3 rounded-md border border-ink-800 bg-ink-850/50 px-3 py-2.5 text-left transition-colors hover:border-accent-500/50 hover:bg-accent-900/20"
                >
                  <FileText className="size-4 shrink-0 text-ink-500" strokeWidth={1.75} />
                  <span className="min-w-0 flex-1">
                    <span className="block truncate text-[13px] text-ink-100">
                      {documento.nombre}
                    </span>
                    <span className="block text-[11px] text-ink-500">{detalle(documento)}</span>
                  </span>
                </button>
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  );
}

function detalle(documento: ResumenDocumento): string {
  const partes = [TIPOS[documento.tipo], tamano(documento.tamano)];
  if (documento.tipo === "pdf") partes.push(`pág. ${documento.ultimaPagina}`);
  partes.push(
    new Date(documento.abiertoEn).toLocaleDateString("es-ES", { day: "numeric", month: "short" }),
  );
  return partes.join(" · ");
}

function tamano(bytes: number): string {
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`;
  return `${(bytes / 1024 / 1024).toLocaleString("es-ES", { maximumFractionDigits: 1 })} MB`;
}
