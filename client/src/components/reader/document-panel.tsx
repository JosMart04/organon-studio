"use client";

import { useRef, useState } from "react";
import dynamic from "next/dynamic";
import { FileText, X } from "lucide-react";
import type { Work } from "@/types/organon";
import { useAsync } from "@/lib/use-async";
import {
  borrarRecientes,
  leerTexto,
  listarRecientes,
  type DocumentoLocal,
} from "@/lib/local-documents";
import type { DocumentoActivo } from "@/components/reader/use-documento-activo";
import { useSeleccion } from "@/components/reader/use-seleccion";
import { SelectionMenu } from "@/components/reader/selection-menu";
import { DocumentDropZone } from "@/components/reader/document-drop-zone";
import { TextDocumentViewer } from "@/components/reader/text-document-viewer";

// pdf.js usa APIs del navegador al importarse: se deja fuera del prerenderizado.
const PdfViewer = dynamic(() => import("@/components/reader/pdf-viewer"), {
  ssr: false,
  loading: () => <p className="px-6 py-8 text-sm text-ink-500">Preparando el lector de PDF…</p>,
});

/** Columna izquierda del lector cuando se lee un documento del ordenador. */
export function DocumentPanel({
  activo,
  works,
  onAbrir,
  onCerrar,
  onVincular,
  onGuardar,
  onPreguntar,
}: {
  activo: DocumentoActivo;
  works: Work[];
  onAbrir: (documento: { id: string; workId?: number }) => void;
  onCerrar: () => void;
  onVincular: (workId: number | undefined) => void;
  onGuardar: (texto: string, locator: string) => void;
  onPreguntar: (texto: string) => void;
}) {
  const [versionRecientes, setVersionRecientes] = useState(0);
  const recientes = useAsync(() => listarRecientes(), [versionRecientes], {
    enabled: activo.documento === undefined,
  });

  if (activo.documento) {
    return (
      // La clave descarta selección, recuento de páginas y scroll al cambiar de documento.
      <VisorDocumento
        key={activo.documento.id}
        documento={activo.documento}
        aviso={activo.aviso}
        works={works}
        onCerrar={onCerrar}
        onVincular={onVincular}
        onPaginaVisible={activo.recordarPagina}
        onGuardar={onGuardar}
        onPreguntar={onPreguntar}
      />
    );
  }

  return (
    <div className="min-h-0 flex-1 overflow-y-auto">
      {activo.cargando ? (
        <p className="px-6 py-8 text-sm text-ink-500">Abriendo el documento…</p>
      ) : (
        <DocumentDropZone
          recientes={recientes.data ?? []}
          abriendo={activo.abriendo}
          error={
            activo.error ??
            (activo.perdido
              ? "Ese documento ya no está guardado en este navegador. Vuelve a abrirlo desde tu ordenador."
              : undefined)
          }
          onFichero={async (fichero) => {
            const documento = await activo.abrir(fichero);
            if (documento) onAbrir(documento);
          }}
          onReciente={onAbrir}
          onBorrarRecientes={async () => {
            await borrarRecientes();
            setVersionRecientes((v) => v + 1);
          }}
        />
      )}
    </div>
  );
}

function VisorDocumento({
  documento,
  aviso,
  works,
  onCerrar,
  onVincular,
  onPaginaVisible,
  onGuardar,
  onPreguntar,
}: {
  documento: DocumentoLocal;
  aviso: string | undefined;
  works: Work[];
  onCerrar: () => void;
  onVincular: (workId: number | undefined) => void;
  onPaginaVisible: (pagina: number) => void;
  onGuardar: (texto: string, locator: string) => void;
  onPreguntar: (texto: string) => void;
}) {
  const hostRef = useRef<HTMLDivElement>(null);
  const [contenedor, setContenedor] = useState<HTMLDivElement | null>(null);
  const [total, setTotal] = useState<number>();
  const { seleccion, capturar, limpiar } = useSeleccion(hostRef);
  const texto = useAsync(() => leerTexto(documento.blob), [documento.id], {
    enabled: documento.tipo !== "pdf",
  });

  const libro = works.find((w) => w.id === documento.workId);

  // En un PDF la referencia es la página; en un texto no la hay, así que se
  // propone el nombre del fichero y el lector la corrige si quiere.
  const referencia = (pagina?: number) =>
    pagina ? `pág. ${pagina}` : documento.nombre.replace(/\.[^.]+$/, "").slice(0, 120);

  return (
    <section className="flex min-h-0 flex-1 flex-col">
      <div className="flex flex-wrap items-center gap-x-3 gap-y-1.5 border-b border-ink-800 px-6 py-2">
        <FileText className="size-3.5 shrink-0 text-ink-600" />
        <span
          className="min-w-0 max-w-[18rem] truncate text-[12px] text-ink-200"
          title={documento.nombre}
        >
          {documento.nombre}
        </span>
        {documento.tipo === "pdf" && total !== undefined && (
          <span className="text-[11px] tabular-nums text-ink-500">
            pág. {Math.min(documento.ultimaPagina, total)} de {total}
          </span>
        )}

        <label className="ml-auto flex items-center gap-1.5 text-[11px] text-ink-500">
          Este documento es
          <select
            value={documento.workId ?? ""}
            onChange={(e) => onVincular(e.target.value ? Number(e.target.value) : undefined)}
            className="max-w-[15rem] rounded border border-ink-700 bg-ink-850 px-2 py-1 text-[11px] text-ink-100 outline-none focus:border-accent-500"
          >
            <option value="">un libro sin vincular</option>
            {works.map((w) => (
              <option key={w.id} value={w.id}>
                {w.philosopherName} — {w.title}
              </option>
            ))}
          </select>
        </label>

        <button
          type="button"
          onClick={onCerrar}
          aria-label="Cerrar documento"
          title="Cerrar. Seguirá en «Seguir leyendo»."
          className="rounded p-1 text-ink-500 transition-colors hover:bg-ink-800 hover:text-ink-200"
        >
          <X className="size-3.5" />
        </button>
      </div>

      {aviso && (
        <p className="border-b border-accent-500/30 bg-accent-900/30 px-6 py-2 text-[12px] text-accent-200">
          {aviso}
        </p>
      )}
      {!libro && works.length > 0 && (
        <p className="border-b border-ink-800 px-6 py-1.5 text-[11px] text-ink-500">
          Vincúlalo a un libro de tu cuaderno para ver qué entiende su autor por las palabras que
          selecciones.
        </p>
      )}

      {/* En pantallas estrechas la página no tiene alto fijo: el visor necesita el suyo
          para que el scroll sea suyo y sepa qué páginas pintar. */}
      <div
        ref={setContenedor}
        className="min-h-0 flex-1 overflow-y-auto max-lg:h-[85vh] max-lg:flex-none"
      >
        <div ref={hostRef} className="relative" onMouseUp={capturar} onKeyUp={capturar}>
          {documento.tipo === "pdf" ? (
            <PdfViewer
              archivo={documento.blob}
              paginaInicial={documento.ultimaPagina}
              contenedor={contenedor}
              onPaginaVisible={onPaginaVisible}
              onPaginas={setTotal}
            />
          ) : (
            <div className="mx-auto max-w-[42rem] px-6 py-8">
              {texto.error ? (
                <p className="text-sm text-fallacy-300">No he podido leer este fichero.</p>
              ) : texto.data === undefined ? (
                <p className="text-sm text-ink-500">Cargando…</p>
              ) : (
                <TextDocumentViewer texto={texto.data} tipo={documento.tipo} />
              )}
            </div>
          )}

          {seleccion && (
            <SelectionMenu
              text={seleccion.texto}
              x={seleccion.x}
              y={seleccion.y}
              workId={libro?.id}
              philosopherId={libro?.philosopherId}
              onGuardar={() => {
                onGuardar(seleccion.texto, referencia(seleccion.pagina));
                limpiar();
              }}
              onPreguntar={() => {
                onPreguntar(seleccion.texto);
                limpiar();
              }}
              onClose={limpiar}
            />
          )}
        </div>
      </div>
    </section>
  );
}
