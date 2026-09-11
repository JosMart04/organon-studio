/**
 * Deja presentable el texto seleccionado en un documento antes de guardarlo.
 *
 * La capa de texto de pdf.js reproduce la maquetación de la página: cada línea
 * acaba en un salto y las palabras partidas a final de línea conservan el guion
 * («la fa-⏎ma»). Se unen esas palabras y el resto de saltos pasa a espacio.
 *
 * Un compuesto partido justo en su guion («franco-⏎alemán») acabaría unido; es
 * raro, y el diálogo de guardado deja corregirlo antes de confirmar.
 */
export function limpiarSeleccion(texto: string): string {
  return (
    texto
      // Guion blando a final de línea: la palabra continúa sin más.
      .replace(/­\s*\n\s*/g, "")
      .replace(/­/g, "")
      // Guion visible a final de línea seguido de minúscula: palabra partida.
      .replace(/(\p{L})[-‐]\s*\n\s*(\p{Ll})/gu, "$1$2")
      .replace(/\s*\n\s*/g, " ")
      .replace(/[ \t ]+/g, " ")
      .trim()
  );
}
