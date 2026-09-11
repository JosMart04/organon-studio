/**
 * Copia a public/pdfjs los recursos que pdf.js descarga en tiempo de ejecución
 * para PDF reales: wasm para escaneos (JBIG2, JPEG 2000), fuentes estándar no
 * incrustadas, CMaps de alfabetos no latinos y perfiles de color.
 *
 * Se ejecuta antes de `dev` y de `build`, así que siempre coinciden con la
 * versión instalada de pdfjs-dist. El resultado no se versiona. Servirlos desde
 * la propia aplicación, y no desde un CDN, mantiene el lector sin conexiones
 * a terceros.
 */
import { cp, mkdir, rm } from "node:fs/promises";
import { createRequire } from "node:module";
import path from "node:path";
import { fileURLToPath } from "node:url";

const require = createRequire(import.meta.url);
const origen = path.dirname(require.resolve("pdfjs-dist/package.json"));
const destino = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "public", "pdfjs");

await rm(destino, { recursive: true, force: true });
await mkdir(destino, { recursive: true });
for (const carpeta of ["cmaps", "standard_fonts", "wasm", "iccs"]) {
  await cp(path.join(origen, carpeta), path.join(destino, carpeta), { recursive: true });
}

console.log(`Recursos de pdf.js copiados en ${path.relative(process.cwd(), destino)}`);
