"use client";

import Markdown from "react-markdown";
import type { TipoDocumento } from "@/lib/local-documents";

/**
 * Markdown o texto plano con la tipografía del lector.
 *
 * react-markdown no usa `dangerouslySetInnerHTML` ni interpreta el HTML
 * incrustado: un `<script>` del fichero nunca se ejecuta. `skipHtml` además lo
 * omite, en vez de mostrar las etiquetas como texto en mitad de la lectura.
 */
export function TextDocumentViewer({
  texto,
  tipo,
}: {
  texto: string;
  tipo: Exclude<TipoDocumento, "pdf">;
}) {
  if (tipo === "markdown") {
    return (
      <div className="prose-source prose-markdown select-text">
        <Markdown
          skipHtml
          components={{
            // Un enlace del documento no debe sacar al lector de su lectura.
            a: ({ href, children }) => (
              <a href={href} target="_blank" rel="noopener noreferrer">
                {children}
              </a>
            ),
          }}
        >
          {texto}
        </Markdown>
      </div>
    );
  }

  return (
    <div className="prose-source select-text">
      {texto.split(/\n\s*\n/).map((parrafo, i) => (
        // pre-line respeta los saltos dentro del párrafo: versos, listas escritas a mano.
        <p key={i} className="whitespace-pre-line">
          {parrafo}
        </p>
      ))}
    </div>
  );
}
