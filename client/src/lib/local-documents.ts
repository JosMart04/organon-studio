/**
 * Documentos que el lector abre desde su ordenador: PDF, Markdown o texto.
 *
 * No pasan por el backend. Se guardan en IndexedDB del navegador para seguir
 * leyendo tras recargar, con la última página y el libro del cuaderno al que
 * corresponden. IndexedDB nativa, sin librería: son cuatro operaciones.
 */

export type TipoDocumento = "pdf" | "markdown" | "texto";

export interface DocumentoLocal {
  /** SHA-256 del contenido: el mismo fichero arrastrado dos veces es el mismo documento. */
  id: string;
  nombre: string;
  tipo: TipoDocumento;
  tamano: number;
  blob: Blob;
  /** Empieza en 1. Solo avanza en los PDF. */
  ultimaPagina: number;
  workId?: number;
  abiertoEn: number;
}

export type ResumenDocumento = Omit<DocumentoLocal, "blob">;

export interface Apertura {
  documento: DocumentoLocal;
  /** Falso si no se ha podido guardar: se lee igual, pero no volverá al recargar. */
  recordado: boolean;
  aviso?: string;
}

export const MAX_DOCUMENTOS = 5;
export const MAX_BYTES = 200 * 1024 * 1024;

const BASE = "organon-lector";
const ALMACEN = "documentos";

export function tipoDeFichero(fichero: File): TipoDocumento | null {
  const nombre = fichero.name.toLowerCase();
  if (nombre.endsWith(".pdf") || fichero.type === "application/pdf") return "pdf";
  if (nombre.endsWith(".md") || nombre.endsWith(".markdown") || fichero.type === "text/markdown") {
    return "markdown";
  }
  if (nombre.endsWith(".txt") || fichero.type === "text/plain") return "texto";
  return null;
}

export async function abrirFichero(fichero: File, tipo: TipoDocumento): Promise<Apertura> {
  const ahora = Date.now();
  const nuevo: DocumentoLocal = {
    id: await huella(fichero),
    nombre: fichero.name,
    tipo,
    tamano: fichero.size,
    blob: fichero,
    ultimaPagina: 1,
    abiertoEn: ahora,
  };

  if (fichero.size > MAX_BYTES) {
    return {
      documento: nuevo,
      recordado: false,
      aviso: "Es demasiado grande para recordarlo: se abre solo esta vez.",
    };
  }

  const db = await abrirBase();
  if (!db) {
    return {
      documento: nuevo,
      recordado: false,
      aviso:
        "Este navegador no deja guardar documentos (¿ventana privada?). Se abre, pero no lo recordaré al recargar.",
    };
  }

  try {
    // Volver a arrastrar un PDF ya leído conserva su página y su libro.
    const previo = await leer(db, nuevo.id);
    const documento = previo ? { ...previo, nombre: fichero.name, abiertoEn: ahora } : nuevo;
    await escribir(db, [documento]);
    await desalojar(db, documento.id);
    return { documento, recordado: true };
  } catch {
    return {
      documento: nuevo,
      recordado: false,
      aviso: "No queda espacio para recordarlo en este navegador: se abre solo esta vez.",
    };
  }
}

export async function cargarDocumento(id: string): Promise<DocumentoLocal | undefined> {
  const db = await abrirBase();
  return db ? leer(db, id) : undefined;
}

export async function listarRecientes(): Promise<ResumenDocumento[]> {
  const db = await abrirBase();
  if (!db) return [];
  const todos = await todosLosDocumentos(db);
  return todos.sort((a, b) => b.abiertoEn - a.abiertoEn).map(resumir);
}

/** Lee y escribe en la misma transacción: un cambio de página no pisa un cambio de libro. */
export async function actualizarDocumento(
  id: string,
  cambios: Partial<Pick<DocumentoLocal, "ultimaPagina" | "workId">>,
): Promise<void> {
  const db = await abrirBase();
  if (!db) return;
  await new Promise<void>((resolver, rechazar) => {
    const tx = db.transaction(ALMACEN, "readwrite");
    const almacen = tx.objectStore(ALMACEN);
    const peticion = almacen.get(id);
    peticion.onsuccess = () => {
      if (peticion.result) almacen.put({ ...peticion.result, ...cambios });
    };
    tx.oncomplete = () => resolver();
    tx.onerror = () => rechazar(tx.error);
  });
}

export async function borrarRecientes(): Promise<void> {
  const db = await abrirBase();
  if (!db) return;
  await new Promise<void>((resolver, rechazar) => {
    const tx = db.transaction(ALMACEN, "readwrite");
    tx.objectStore(ALMACEN).clear();
    tx.oncomplete = () => resolver();
    tx.onerror = () => rechazar(tx.error);
  });
}

/** UTF-8 si lo es; si no, Windows-1252, que es como guardaba el Bloc de notas. */
export async function leerTexto(blob: Blob): Promise<string> {
  const bytes = await blob.arrayBuffer();
  try {
    return new TextDecoder("utf-8", { fatal: true }).decode(bytes);
  } catch {
    return new TextDecoder("windows-1252").decode(bytes);
  }
}

// ---------------------------------------------------------------------------
// IndexedDB
// ---------------------------------------------------------------------------

let base: Promise<IDBDatabase | null> | null = null;

function abrirBase(): Promise<IDBDatabase | null> {
  base ??= new Promise((resolver) => {
    try {
      const peticion = indexedDB.open(BASE, 1);
      peticion.onupgradeneeded = () => {
        peticion.result.createObjectStore(ALMACEN, { keyPath: "id" });
      };
      peticion.onsuccess = () => resolver(peticion.result);
      peticion.onerror = () => resolver(null);
      peticion.onblocked = () => resolver(null);
    } catch {
      // Sin IndexedDB (algunas ventanas privadas): se sigue leyendo sin recordar.
      resolver(null);
    }
  });
  return base;
}

function leer(db: IDBDatabase, id: string): Promise<DocumentoLocal | undefined> {
  return new Promise((resolver, rechazar) => {
    const peticion = db.transaction(ALMACEN, "readonly").objectStore(ALMACEN).get(id);
    peticion.onsuccess = () => resolver(peticion.result as DocumentoLocal | undefined);
    peticion.onerror = () => rechazar(peticion.error);
  });
}

function todosLosDocumentos(db: IDBDatabase): Promise<DocumentoLocal[]> {
  return new Promise((resolver, rechazar) => {
    const peticion = db.transaction(ALMACEN, "readonly").objectStore(ALMACEN).getAll();
    peticion.onsuccess = () => resolver(peticion.result as DocumentoLocal[]);
    peticion.onerror = () => rechazar(peticion.error);
  });
}

/** Espera al final de la transacción: que la petición acabe no garantiza que se haya guardado. */
function escribir(db: IDBDatabase, documentos: DocumentoLocal[]): Promise<void> {
  return new Promise((resolver, rechazar) => {
    const tx = db.transaction(ALMACEN, "readwrite");
    for (const documento of documentos) tx.objectStore(ALMACEN).put(documento);
    tx.oncomplete = () => resolver();
    tx.onerror = () => rechazar(tx.error);
    tx.onabort = () => rechazar(tx.error);
  });
}

/** Deja los más recientes dentro de los límites de número y de tamaño. */
async function desalojar(db: IDBDatabase, conservar: string): Promise<void> {
  const todos = (await todosLosDocumentos(db)).sort((a, b) => b.abiertoEn - a.abiertoEn);
  let acumulado = 0;
  const sobran = todos.filter((documento, posicion) => {
    acumulado += documento.tamano;
    return documento.id !== conservar && (posicion >= MAX_DOCUMENTOS || acumulado > MAX_BYTES);
  });
  if (sobran.length === 0) return;

  await new Promise<void>((resolver, rechazar) => {
    const tx = db.transaction(ALMACEN, "readwrite");
    for (const documento of sobran) tx.objectStore(ALMACEN).delete(documento.id);
    tx.oncomplete = () => resolver();
    tx.onerror = () => rechazar(tx.error);
  });
}

async function huella(fichero: File): Promise<string> {
  // crypto.subtle solo existe en contextos seguros: https o localhost.
  if (globalThis.crypto?.subtle) {
    const resumen = await crypto.subtle.digest("SHA-256", await fichero.arrayBuffer());
    return Array.from(new Uint8Array(resumen), (b) => b.toString(16).padStart(2, "0")).join("");
  }
  return `${fichero.name}-${fichero.size}-${fichero.lastModified}`;
}

function resumir({ id, nombre, tipo, tamano, ultimaPagina, workId, abiertoEn }: DocumentoLocal) {
  return { id, nombre, tipo, tamano, ultimaPagina, workId, abiertoEn } satisfies ResumenDocumento;
}
