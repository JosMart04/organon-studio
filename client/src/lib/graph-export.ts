/**
 * Guarda el mapa del debate como imagen PNG o SVG.
 *
 * Sigue el ejemplo oficial de React Flow: se calcula el recuadro que ocupan
 * todos los nodos y se captura `.react-flow__viewport` con la transformación
 * que lo encaja. Así sale el mapa completo, no solo lo que se ve en pantalla.
 *
 * La leyenda de la pantalla es HTML fuera del viewport y no saldría en la
 * captura, de modo que se dibuja aparte: sobre un canvas en el PNG y con
 * elementos SVG nativos en el SVG. El grafo va dentro de un `<foreignObject>`;
 * la leyenda y la cabecera, no.
 */
import { getNodesBounds, getViewportForBounds, type Node } from "@xyflow/react";
import { getFontEmbedCSS, toPng, toSvg } from "html-to-image";
import { RELATION_COLORS, humanize } from "@/lib/vocabulario";

export type FormatoImagen = "png" | "svg";

type OpcionesCaptura = NonNullable<Parameters<typeof toPng>[1]>;
type Familia = "serif" | "sans";
type Familias = Record<Familia, string>;

/** Colores de globals.css repetidos a mano: ni el canvas ni un SVG suelto leen variables CSS. */
const TEMA = {
  fondo: "#0b0d10", // ink-950
  panel: "#12151a", // ink-900
  borde: "#272e39", // ink-700
  texto: "#94a1b2", // ink-300
  titulo: "#eef2f6", // ink-50
};

const MARGEN = 40;
const CABECERA = 72;
const SEPARACION = 28;
const LEYENDA_ANCHO = 250;
/** Aire alrededor de los nodos: las flechas laterales y sus etiquetas se salen del recuadro. */
const HOLGURA = 48;
const GRAFO_MIN_ANCHO = 640;
const GRAFO_MIN_ALTO = 420;
const GRAFO_MAX = 4000;
/** Por encima de este lado el PNG se genera a escala 1, que algunos navegadores no pintan canvas enormes. */
const LADO_MAX_PNG = 8192;

const INTRO_LEYENDA =
  "Un autor escribe un libro, y un libro contiene ideas. Las flechas de colores unen ideas que discuten entre sí.";

type Trazo =
  | {
      tipo: "rect";
      x: number;
      y: number;
      ancho: number;
      alto: number;
      radio: number;
      relleno?: string;
      borde?: string;
    }
  | {
      tipo: "linea";
      x1: number;
      y1: number;
      x2: number;
      y2: number;
      color: string;
      grosor: number;
      discontinua?: boolean;
    }
  | {
      tipo: "texto";
      x: number;
      y: number;
      texto: string;
      color: string;
      tamano: number;
      familia: Familia;
    };

interface Disposicion {
  ancho: number;
  alto: number;
  grafo: { x: number; y: number; ancho: number; alto: number };
  /** Lo que va por debajo de la captura del grafo: fondo y cabecera. */
  debajo: Trazo[];
  /** Lo que va por encima: el marco del grafo y la leyenda. */
  encima: Trazo[];
  familias: Familias;
}

export async function exportarGrafo({
  lienzo,
  nodos,
  formato,
  epoca,
  palabra,
}: {
  /** El elemento `.react-flow__viewport`. */
  lienzo: HTMLElement;
  nodos: Node[];
  formato: FormatoImagen;
  /** Filtros activos, para que la imagen diga que no es el mapa entero. */
  epoca?: string;
  palabra?: string;
}): Promise<void> {
  if (nodos.length === 0) throw new Error("No hay nada que exportar");

  // Medir y pintar texto con una fuente a medio cargar descuadra la leyenda.
  await document.fonts.ready;

  const limites = getNodesBounds(nodos);
  const grafoAncho = limitar(Math.ceil(limites.width) + HOLGURA * 2, GRAFO_MIN_ANCHO, GRAFO_MAX);
  const grafoAlto = limitar(Math.ceil(limites.height) + HOLGURA * 2, GRAFO_MIN_ALTO, GRAFO_MAX);
  // Zoom máximo 1: un mapa pequeño queda centrado a tamaño natural en vez de ampliado.
  const vista = getViewportForBounds(limites, grafoAncho, grafoAlto, 0.1, 1, `${HOLGURA}px`);

  const textoSubtitulo = subtitulo(epoca, palabra);
  const disposicion = disponer(grafoAncho, grafoAlto, textoSubtitulo, leerFamilias());

  const opciones: OpcionesCaptura = {
    width: grafoAncho,
    height: grafoAlto,
    backgroundColor: TEMA.fondo,
    fontEmbedCSS: await fuentesParaElMapa(lienzo, textoSubtitulo),
    // Los puntos de anclaje de las flechas solo sirven para editar.
    filter: (nodo) => !(nodo instanceof Element && nodo.classList.contains("react-flow__handle")),
    style: {
      width: `${grafoAncho}px`,
      height: `${grafoAlto}px`,
      transform: `translate(${vista.x}px, ${vista.y}px) scale(${vista.zoom})`,
    },
  };

  const blob =
    formato === "png"
      ? await componerPng(lienzo, opciones, disposicion)
      : await componerSvg(lienzo, opciones, disposicion);

  descargar(blob, `organon-debate-${fechaParaNombre()}.${formato}`);
}

// ---------------------------------------------------------------------------
// Composición
// ---------------------------------------------------------------------------

function disponer(
  grafoAncho: number,
  grafoAlto: number,
  textoSubtitulo: string,
  familias: Familias,
): Disposicion {
  const grafo = { x: MARGEN, y: MARGEN + CABECERA, ancho: grafoAncho, alto: grafoAlto };
  const leyenda = componerLeyenda(grafo.x + grafoAncho + SEPARACION, grafo.y, familias);

  const ancho = grafo.x + grafoAncho + SEPARACION + LEYENDA_ANCHO + MARGEN;
  const alto = grafo.y + Math.max(grafoAlto, leyenda.alto) + MARGEN;

  const debajo: Trazo[] = [
    { tipo: "rect", x: 0, y: 0, ancho, alto, radio: 0, relleno: TEMA.fondo },
    {
      tipo: "texto",
      x: MARGEN,
      y: MARGEN + 26,
      texto: "El debate",
      color: TEMA.titulo,
      tamano: 26,
      familia: "serif",
    },
    {
      tipo: "texto",
      x: MARGEN,
      y: MARGEN + 50,
      texto: textoSubtitulo,
      color: TEMA.texto,
      tamano: 13,
      familia: "sans",
    },
  ];

  const encima: Trazo[] = [
    { tipo: "rect", ...grafo, radio: 10, borde: TEMA.borde },
    ...leyenda.trazos,
  ];

  return { ancho, alto, grafo, debajo, encima, familias };
}

function componerLeyenda(x: number, y: number, familias: Familias) {
  const relleno = 18;
  const izquierda = x + relleno;
  const trazos: Trazo[] = [];
  let cursor = y + relleno;

  for (const linea of partirLineas(INTRO_LEYENDA, LEYENDA_ANCHO - relleno * 2, 12, familias.sans)) {
    cursor += 17;
    trazos.push({
      tipo: "texto",
      x: izquierda,
      y: cursor,
      texto: linea,
      color: TEMA.texto,
      tamano: 12,
      familia: "sans",
    });
  }

  cursor += 14;
  trazos.push({
    tipo: "linea",
    x1: izquierda,
    y1: cursor,
    x2: x + LEYENDA_ANCHO - relleno,
    y2: cursor,
    color: TEMA.borde,
    grosor: 1,
  });
  cursor += 4;

  for (const relacion of Object.keys(RELATION_COLORS) as (keyof typeof RELATION_COLORS)[]) {
    cursor += 24;
    const color = RELATION_COLORS[relacion];
    trazos.push({
      tipo: "linea",
      x1: izquierda,
      y1: cursor - 4,
      x2: izquierda + 28,
      y2: cursor - 4,
      color,
      grosor: 2,
      // Igual que en el mapa: «se basa en» es la única relación discontinua.
      discontinua: relacion === "PRESUPONE",
    });
    trazos.push({
      tipo: "texto",
      x: izquierda + 40,
      y: cursor,
      texto: humanize(relacion),
      color: TEMA.texto,
      tamano: 12,
      familia: "sans",
    });
  }

  const alto = cursor + relleno - y;
  const panel: Trazo = {
    tipo: "rect",
    x,
    y,
    ancho: LEYENDA_ANCHO,
    alto,
    radio: 10,
    relleno: TEMA.panel,
    borde: TEMA.borde,
  };
  return { alto, trazos: [panel, ...trazos] };
}

async function componerPng(
  lienzo: HTMLElement,
  opciones: OpcionesCaptura,
  d: Disposicion,
): Promise<Blob> {
  const escala = Math.max(d.ancho, d.alto) * 2 > LADO_MAX_PNG ? 1 : 2;
  const captura = await cargarImagen(await toPng(lienzo, { ...opciones, pixelRatio: escala }));

  const canvas = document.createElement("canvas");
  canvas.width = Math.round(d.ancho * escala);
  canvas.height = Math.round(d.alto * escala);
  const ctx = canvas.getContext("2d");
  if (!ctx) throw new Error("El navegador no permite dibujar en canvas");

  ctx.scale(escala, escala);
  pintar(ctx, d.debajo, d.familias);
  ctx.drawImage(captura, d.grafo.x, d.grafo.y, d.grafo.ancho, d.grafo.alto);
  pintar(ctx, d.encima, d.familias);

  return new Promise((resolver, rechazar) =>
    canvas.toBlob(
      (blob) => (blob ? resolver(blob) : rechazar(new Error("No se pudo generar el PNG"))),
      "image/png",
    ),
  );
}

async function componerSvg(
  lienzo: HTMLElement,
  opciones: OpcionesCaptura,
  d: Disposicion,
): Promise<Blob> {
  // html-to-image devuelve «data:image/svg+xml;charset=utf-8,<svg codificado>».
  const url = await toSvg(lienzo, opciones);
  const xml = decodeURIComponent(url.slice(url.indexOf(",") + 1));
  const documento = new DOMParser().parseFromString(xml, "image/svg+xml");
  if (documento.getElementsByTagName("parsererror").length > 0) {
    throw new Error("La captura del grafo no es un SVG válido");
  }

  // Un <svg> anidado con x/y: el grafo conserva su viewBox y se coloca bajo la cabecera.
  const grafo = documento.documentElement;
  grafo.setAttribute("x", String(d.grafo.x));
  grafo.setAttribute("y", String(d.grafo.y));

  const partes = [
    '<?xml version="1.0" encoding="UTF-8"?>',
    `<svg xmlns="http://www.w3.org/2000/svg" width="${d.ancho}" height="${d.alto}" viewBox="0 0 ${d.ancho} ${d.alto}">`,
    ...d.debajo.map((t) => trazoSvg(t, d.familias)),
    new XMLSerializer().serializeToString(grafo),
    ...d.encima.map((t) => trazoSvg(t, d.familias)),
    "</svg>",
  ];
  return new Blob([partes.join("\n")], { type: "image/svg+xml;charset=utf-8" });
}

// ---------------------------------------------------------------------------
// Dibujo
// ---------------------------------------------------------------------------

function pintar(ctx: CanvasRenderingContext2D, trazos: Trazo[], familias: Familias) {
  for (const t of trazos) {
    switch (t.tipo) {
      case "rect":
        ctx.beginPath();
        ctx.roundRect(t.x, t.y, t.ancho, t.alto, t.radio);
        if (t.relleno) {
          ctx.fillStyle = t.relleno;
          ctx.fill();
        }
        if (t.borde) {
          ctx.strokeStyle = t.borde;
          ctx.lineWidth = 1;
          ctx.stroke();
        }
        break;
      case "linea":
        ctx.beginPath();
        ctx.setLineDash(t.discontinua ? [5, 4] : []);
        ctx.moveTo(t.x1, t.y1);
        ctx.lineTo(t.x2, t.y2);
        ctx.strokeStyle = t.color;
        ctx.lineWidth = t.grosor;
        ctx.stroke();
        ctx.setLineDash([]);
        break;
      case "texto":
        ctx.font = `${t.tamano}px ${familias[t.familia]}`;
        ctx.fillStyle = t.color;
        ctx.fillText(t.texto, t.x, t.y);
        break;
    }
  }
}

function trazoSvg(t: Trazo, familias: Familias): string {
  switch (t.tipo) {
    case "rect": {
      const borde = t.borde ? ` stroke="${t.borde}" stroke-width="1"` : "";
      return `<rect x="${t.x}" y="${t.y}" width="${t.ancho}" height="${t.alto}" rx="${t.radio}" fill="${t.relleno ?? "none"}"${borde}/>`;
    }
    case "linea": {
      const trazo = t.discontinua ? ' stroke-dasharray="5 4"' : "";
      return `<line x1="${t.x1}" y1="${t.y1}" x2="${t.x2}" y2="${t.y2}" stroke="${t.color}" stroke-width="${t.grosor}"${trazo}/>`;
    }
    case "texto":
      return `<text x="${t.x}" y="${t.y}" fill="${t.color}" font-size="${t.tamano}" font-family="${escapar(familias[t.familia])}">${escapar(t.texto)}</text>`;
  }
}

// ---------------------------------------------------------------------------
// Utilidades
// ---------------------------------------------------------------------------

/** Variables CSS en las que next/font deja las familias del tema. */
const VARIABLES_FUENTE = {
  serif: "--font-source-serif",
  sans: "--font-inter",
  mono: "--font-jetbrains-mono",
} as const;

let cssFuentes: Promise<string> | null = null;

/**
 * Sin `fontEmbedCSS`, html-to-image vuelve a descargar e incrustar las fuentes
 * en cada captura. Se descargan una vez; lo que cambia de un mapa a otro es qué
 * parte de ellas hace falta.
 */
function cssFuentesCompleto(lienzo: HTMLElement): Promise<string> {
  cssFuentes ??= getFontEmbedCSS(lienzo).catch((error: unknown) => {
    cssFuentes = null;
    throw error;
  });
  return cssFuentes;
}

/**
 * html-to-image 1.11.11 incrusta en base64 todas las fuentes de la página: las
 * de KaTeX, las de la capa de desarrollo de Next y cada subconjunto de
 * next/font. Un mapa de seis nodos pesaba 3 MB, casi dos en fuentes que no
 * aparecen en él. Se quedan las familias del tema con el latín básico, y el
 * latín extendido solo si el mapa lo usa (Łukasiewicz, Nāgārjuna).
 */
async function fuentesParaElMapa(lienzo: HTMLElement, textoExtra: string): Promise<string> {
  const css = await cssFuentesCompleto(lienzo);
  const texto = `${lienzo.textContent ?? ""} ${textoExtra}`;
  const hasta = /[Ā-ɏ]/.test(texto) ? 0x024f : 0x00ff;
  return filtrarFuentes(css, familiasDelTema(), hasta);
}

function filtrarFuentes(css: string, familias: Set<string>, hasta: number): string {
  const reglas = css.match(/@font-face\s*\{[^}]*\}/g);
  if (!reglas) return css;
  const utiles = reglas.filter((regla) => {
    const familia = /font-family\s*:\s*([^;}]+)/i.exec(regla)?.[1];
    if (!familia || !familias.has(sinComillas(familia))) return false;
    const rango = /unicode-range\s*:\s*([^;}]+)/i.exec(regla)?.[1];
    return !rango || rango.split(",").some((tramo) => empiezaAntesDe(tramo.trim(), hasta));
  });
  // Si el filtro no deja nada es que el CSS tiene otra forma: mejor pesado que sin fuentes.
  return utiles.length > 0 ? utiles.join("\n") : css;
}

/** Un tramo `U+0-FF`, `U+131` o `U+4??` toca el intervalo [0, hasta] si empieza dentro. */
function empiezaAntesDe(tramo: string, hasta: number): boolean {
  const inicio = /^U\+([0-9a-f?]+)/i.exec(tramo)?.[1];
  return inicio !== undefined && parseInt(inicio.replace(/\?/g, "0"), 16) <= hasta;
}

function familiasDelTema(): Set<string> {
  const estilo = getComputedStyle(document.body);
  return new Set(
    Object.values(VARIABLES_FUENTE).flatMap((variable) =>
      estilo.getPropertyValue(variable).split(",").map(sinComillas).filter(Boolean),
    ),
  );
}

function sinComillas(nombre: string): string {
  return nombre.trim().replace(/^['"]|['"]$/g, "");
}

/** next/font genera nombres de familia propios; se leen de sus variables CSS. */
function leerFamilias(): Familias {
  const estilo = getComputedStyle(document.body);
  const conRespaldo = (variable: string, respaldo: string) => {
    const valor = estilo.getPropertyValue(variable).trim();
    return valor ? `${valor}, ${respaldo}` : respaldo;
  };
  return {
    serif: conRespaldo(VARIABLES_FUENTE.serif, 'Georgia, "Times New Roman", serif'),
    sans: conRespaldo(VARIABLES_FUENTE.sans, '"Segoe UI", system-ui, sans-serif'),
  };
}

let medidor: CanvasRenderingContext2D | null = null;

/** Ni el canvas ni SVG parten líneas solos: se mide palabra a palabra. */
function partirLineas(texto: string, anchoMax: number, tamano: number, familia: string): string[] {
  medidor ??= document.createElement("canvas").getContext("2d");
  if (!medidor) return [texto];
  medidor.font = `${tamano}px ${familia}`;

  const lineas: string[] = [];
  let actual = "";
  for (const palabra of texto.split(/\s+/)) {
    const prueba = actual ? `${actual} ${palabra}` : palabra;
    if (actual && medidor.measureText(prueba).width > anchoMax) {
      lineas.push(actual);
      actual = palabra;
    } else {
      actual = prueba;
    }
  }
  if (actual) lineas.push(actual);
  return lineas;
}

function subtitulo(epoca?: string, palabra?: string): string {
  const fecha = new Date().toLocaleDateString("es-ES", {
    day: "numeric",
    month: "long",
    year: "numeric",
  });
  return [
    "Organon Studio",
    fecha,
    epoca && `solo época ${humanize(epoca).toLowerCase()}`,
    palabra && `solo quienes discuten «${palabra}»`,
  ]
    .filter(Boolean)
    .join(" · ");
}

function cargarImagen(src: string): Promise<HTMLImageElement> {
  return new Promise((resolver, rechazar) => {
    const imagen = new Image();
    imagen.onload = () => resolver(imagen);
    imagen.onerror = () => rechazar(new Error("No se pudo cargar la captura del grafo"));
    imagen.src = src;
  });
}

function descargar(blob: Blob, nombre: string) {
  const url = URL.createObjectURL(blob);
  const enlace = document.createElement("a");
  enlace.href = url;
  enlace.download = nombre;
  document.body.appendChild(enlace);
  enlace.click();
  enlace.remove();
  // Revocar la URL en el mismo instante del clic puede cancelar la descarga.
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

function fechaParaNombre(): string {
  const hoy = new Date();
  const dos = (n: number) => String(n).padStart(2, "0");
  return `${hoy.getFullYear()}-${dos(hoy.getMonth() + 1)}-${dos(hoy.getDate())}`;
}

function limitar(valor: number, minimo: number, maximo: number): number {
  return Math.min(Math.max(valor, minimo), maximo);
}

function escapar(texto: string): string {
  return texto
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}
