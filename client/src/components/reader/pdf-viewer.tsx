"use client";

import { useEffect, useRef, useState } from "react";
import { Document, Page, pdfjs, type DocumentProps, type TextContent } from "react-pdf";
import "react-pdf/dist/Page/TextLayer.css";
import "react-pdf/dist/Page/AnnotationLayer.css";

// En el mismo módulo que <Document>: configurado en otro fichero, el orden de
// carga puede devolverle el valor por defecto (advertencia del README de react-pdf).
pdfjs.GlobalWorkerOptions.workerSrc = new URL(
  "pdfjs-dist/build/pdf.worker.min.mjs",
  import.meta.url,
).toString();

/**
 * Lo que pdf.js descarga para PDF reales: wasm para escaneos (JBIG2, JPEG 2000),
 * fuentes estándar no incrustadas, CMaps de alfabetos no latinos y perfiles de
 * color. Los copia `scripts/copiar-recursos-pdf.mjs` antes de `dev` y `build`.
 * Constante de módulo: react-pdf recarga el documento si cambia su identidad.
 */
const OPCIONES = {
  cMapUrl: "/pdfjs/cmaps/",
  standardFontDataUrl: "/pdfjs/standard_fonts/",
  wasmUrl: "/pdfjs/wasm/",
  iccUrl: "/pdfjs/iccs/",
};

const ANCHO_MAXIMO = 860;
const MARGEN_LATERAL = 48;
const SEPARACION = 16;
/** Páginas pintadas a cada lado de la visible. Las demás son huecos del mismo tamaño. */
const VENTANA = 2;

type DocumentoPdf = Parameters<NonNullable<DocumentProps["onLoadSuccess"]>>[0];

export default function PdfViewer({
  archivo,
  paginaInicial,
  contenedor,
  onPaginaVisible,
  onPaginas,
}: {
  archivo: Blob;
  paginaInicial: number;
  /** El elemento con scroll que contiene el visor. */
  contenedor: HTMLElement | null;
  onPaginaVisible: (pagina: number) => void;
  onPaginas: (total: number) => void;
}) {
  const [total, setTotal] = useState<number>();
  const [proporcion, setProporcion] = useState<number>();
  const [ancho, setAncho] = useState<number>();
  const [actual, setActual] = useState(paginaInicial);
  const [sinTexto, setSinTexto] = useState(false);

  const listaRef = useRef<HTMLDivElement>(null);
  const paginasRef = useRef<(HTMLDivElement | null)[]>([]);
  const conTexto = useRef(new Map<number, boolean>());
  const saltoInicialHecho = useRef(false);
  const avisarPagina = useRef(onPaginaVisible);

  useEffect(() => {
    avisarPagina.current = onPaginaVisible;
  });

  useEffect(() => {
    if (!contenedor) return;
    const observador = new ResizeObserver(([entrada]) => {
      const medido = anchoUtil(entrada.contentRect.width);
      if (medido) setAncho(medido);
    });
    observador.observe(contenedor);
    return () => observador.disconnect();
  }, [contenedor]);

  // El observador avisa en el siguiente fotograma, y nunca si la ventana no se
  // está pintando. Hasta entonces se mide a mano, sin esperar un fotograma en blanco.
  const anchoVigente = ancho ?? (contenedor ? anchoUtil(contenedor.clientWidth) : undefined);

  // La página actual sale del scroll: la que ocupa el primer tercio visible.
  useEffect(() => {
    if (!contenedor || !total) return;
    let ultima = 0;
    const alDesplazar = () => {
      const lista = listaRef.current;
      if (!lista) return;
      const inicioLista = lista.getBoundingClientRect().top - contenedor.getBoundingClientRect().top;
      const pagina = paginaEn(paginasRef.current, contenedor.clientHeight / 3 - inicioLista);
      if (pagina !== ultima) {
        ultima = pagina;
        setActual(pagina);
        avisarPagina.current(pagina);
      }
    };
    contenedor.addEventListener("scroll", alDesplazar, { passive: true });
    return () => contenedor.removeEventListener("scroll", alDesplazar);
  }, [contenedor, total]);

  // Volver a la página donde se dejó, una sola vez y con los huecos ya medidos.
  useEffect(() => {
    if (saltoInicialHecho.current || !contenedor || !total || !anchoVigente || !proporcion) return;
    saltoInicialHecho.current = true;
    const destino = paginasRef.current[Math.min(paginaInicial, total) - 1];
    if (destino && paginaInicial > 1) {
      contenedor.scrollBy({
        top: destino.getBoundingClientRect().top - contenedor.getBoundingClientRect().top - SEPARACION,
      });
    }
  }, [contenedor, total, anchoVigente, proporcion, paginaInicial]);

  const alCargar = async (pdf: DocumentoPdf) => {
    const vista = (await pdf.getPage(1)).getViewport({ scale: 1 });
    setProporcion(vista.height / vista.width);
    setTotal(pdf.numPages);
    onPaginas(pdf.numPages);
  };

  // Un escaneo no trae capa de texto: se avisa en vez de dejar que la selección falle en silencio.
  const registrarTexto = (pagina: number, contenido: TextContent) => {
    const tiene = contenido.items.some((item) => "str" in item && item.str.trim() !== "");
    conTexto.current.set(pagina, tiene);
    const muestras = Math.min(3, total ?? 1);
    if (!tiene && conTexto.current.size >= muestras && [...conTexto.current.values()].every((v) => !v)) {
      setSinTexto(true);
    }
  };

  return (
    <div className="lector-pdf">
      {sinTexto && (
        <p
          role="status"
          className="sticky top-0 z-10 border-b border-accent-500/40 bg-accent-900/90 px-6 py-2 text-[12px] leading-relaxed text-accent-200 backdrop-blur"
        >
          Este PDF es una imagen: no tiene texto que se pueda seleccionar. Puedes leerlo aquí,
          pero para guardar un fragmento tendrás que copiarlo a mano.
        </p>
      )}

      <Document
        file={archivo}
        options={OPCIONES}
        onLoadSuccess={alCargar}
        onLoadError={(error) => console.error(error)}
        loading={<p className="px-6 py-8 text-sm text-ink-500">Abriendo el PDF…</p>}
        error={
          <p className="px-6 py-8 text-sm text-fallacy-300">
            No he podido abrir este PDF. Puede estar dañado o protegido con contraseña.
          </p>
        }
      >
        {total !== undefined && anchoVigente !== undefined && proporcion !== undefined && (
          <div
            ref={listaRef}
            className="relative flex flex-col items-center py-6"
            style={{ gap: SEPARACION }}
          >
            {Array.from({ length: total }, (_, i) => {
              const pagina = i + 1;
              return (
                <div
                  key={pagina}
                  ref={(elemento) => {
                    paginasRef.current[i] = elemento;
                  }}
                  data-pagina={pagina}
                  className="relative bg-white shadow-lg shadow-black/40"
                  style={{ width: anchoVigente, minHeight: Math.round(anchoVigente * proporcion) }}
                >
                  {Math.abs(pagina - actual) <= VENTANA && (
                    <Page
                      pageNumber={pagina}
                      width={anchoVigente}
                      loading={null}
                      onGetTextSuccess={(contenido) => registrarTexto(pagina, contenido)}
                    />
                  )}
                </div>
              );
            })}
          </div>
        )}
      </Document>
    </div>
  );
}

/** Ancho de página para el hueco disponible. Oculto mide 0: no se pinta todo a tamaño sello. */
function anchoUtil(disponible: number): number | undefined {
  const util = disponible - MARGEN_LATERAL;
  return util > 200 ? Math.min(Math.floor(util), ANCHO_MAXIMO) : undefined;
}

/** Búsqueda binaria: la última página cuyo borde superior queda por encima de `y`. */
function paginaEn(paginas: (HTMLDivElement | null)[], y: number): number {
  let bajo = 0;
  let alto = paginas.length - 1;
  let encontrada = 0;
  while (bajo <= alto) {
    const medio = (bajo + alto) >> 1;
    if ((paginas[medio]?.offsetTop ?? 0) <= y) {
      encontrada = medio;
      bajo = medio + 1;
    } else {
      alto = medio - 1;
    }
  }
  return encontrada + 1;
}
