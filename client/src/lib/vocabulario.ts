/**
 * Todo el lenguaje que ve el lector, en un solo sitio.
 *
 * El backend habla en términos técnicos —`PREMISA`, `ENTIMEMA`, `FALAZ`— porque
 * ese vocabulario es preciso y no conviene tocarlo. Pero quien usa la
 * aplicación es alguien que lee filosofía por gusto, no un doctorando en
 * lógica, así que la interfaz traduce.
 *
 * Si mañana «Razón» no convence, se cambia aquí y cambia en toda la aplicación.
 */

// ---------------------------------------------------------------------------
// Traducción de los enums del dominio
// ---------------------------------------------------------------------------

const ETIQUETAS: Record<string, string> = {
  // Estatuto de cada razón
  AXIOMATICA: "Principio de partida",
  EMPIRICA: "Observación",
  DEFINICION: "Definición",
  INFERENCIA_INTERMEDIA: "Paso intermedio",
  CONCLUSION: "Conclusión",

  // Tipos de crítica
  CONTRAEJEMPLO: "Hay un caso que lo desmiente",
  FALACIA_FORMAL: "Salta un paso lógico",
  PETICION_DE_PRINCIPIO: "Da por hecho lo que quiere probar",
  REDEFINICION: "Cambia el significado a mitad",
  REGRESO_AL_INFINITO: "La explicación no termina nunca",
  FALSA_DICOTOMIA: "Presenta solo dos salidas cuando hay más",
  AMBIGUEDAD: "Usa una palabra en dos sentidos",
  GENERALIZACION_APRESURADA: "Generaliza desde muy pocos casos",

  // Cómo encaja el razonamiento
  MODUS_PONENS: "Si esto, entonces aquello",
  MODUS_TOLLENS: "Si no pasa aquello, tampoco esto",
  SILOGISMO_CATEGORICO: "Silogismo clásico",
  SILOGISMO_DISYUNTIVO: "O una cosa o la otra",
  SILOGISMO_HIPOTETICO: "Encadena condiciones",
  REDUCTIO_AD_ABSURDUM: "Lleva la idea contraria al absurdo",
  INDUCTIVE: "Generaliza desde ejemplos",
  ABDUCTIVE: "Busca la mejor explicación",
  TRASCENDENTAL: "Pregunta qué hace posible la experiencia",
  ANALOGICO: "Razona por comparación",
  NO_CLASIFICADO: "Sin clasificar",

  // Veredicto de la revisión
  SOLIDO: "Se sostiene",
  FALAZ: "Tiene un fallo",
  PENDIENTE: "Sin revisar",

  // Épocas
  ANTIGUA: "Antigua",
  MEDIEVAL: "Medieval",
  MODERNA: "Moderna",
  CONTEMPORANEA: "Contemporánea",

  // Relaciones del grafo
  REFUTA: "Refuta a",
  PRESUPONE: "Se basa en",
  EXTIENDE: "Amplía a",
  RADICALIZA: "Lleva más lejos a",
  MATIZA: "Matiza a",

  // Alcance de una definición
  OBRA: "en este libro",
  AUTOR: "en general",
  SIN_DEFINICION: "sin definir",

  // Gravedad de los hallazgos de la revisión
  BLOQUEANTE: "Problema serio",
  ADVERTENCIA: "Revisar",
  INFORMATIVA: "Nota",
};

/** Convierte un valor del backend en algo legible. Si no lo conoce, lo deja pasar. */
export function humanize(value: string): string {
  return ETIQUETAS[value] ?? value;
}

// ---------------------------------------------------------------------------
// Hallazgos de la revisión, explicados
// ---------------------------------------------------------------------------

/**
 * El backend devuelve códigos y un mensaje ya redactado, pero ese mensaje
 * arrastra vocabulario técnico. Aquí se les pone un titular corto y llano.
 */
const HALLAZGOS: Record<string, string> = {
  SIN_PREMISAS: "Todavía no has escrito ninguna razón",
  SIN_CONCLUSION: "Falta decir a qué conclusión llega",
  CONCLUSION_MULTIPLE: "Hay más de una conclusión",
  CONCLUSION_SIN_APOYO: "La conclusión no se apoya en nada",
  CONCLUSION_FUERA_DE_ORDEN: "La conclusión no está al final",
  PREMISAS_INSUFICIENTES: "Probablemente falte una razón por escribir",
  ESQUEMA_SIN_CLASIFICAR: "Sin clasificar cómo encaja el razonamiento",
  ORDEN_NO_CONTIGUO: "El orden tiene huecos",
  ENTIMEMA_SIN_EXAMINAR: "Hay un supuesto implícito sin discutir",
  ENTIMEMA_EXAMINADO: "El supuesto implícito ya tiene crítica",
};

export function tituloHallazgo(code: string): string {
  if (HALLAZGOS[code]) return HALLAZGOS[code];
  // Los códigos de objeción son OBJECION_<TIPO>
  if (code.startsWith("OBJECION_")) {
    return `Crítica: ${humanize(code.slice("OBJECION_".length)).toLowerCase()}`;
  }
  return code;
}

// ---------------------------------------------------------------------------
// Textos fijos de la interfaz
// ---------------------------------------------------------------------------

export const TEXTOS = {
  supuestoImplicito: "Supuesto implícito",
  supuestoImplicitoAyuda:
    "Una idea que el autor da por obvia sin mencionarla. Suele ser el punto por donde el razonamiento cede.",
  estructuraIdea: "Estructura de la idea",
  razones: "Razones del autor",
  critica: "Crítica",
  criticaPregunta: "¿Por qué podría estar equivocado?",
  revision: "Revisión",
  notacionFormal: "Ver notación lógica formal",
  conclusionFueraDeSitio: "La conclusión debería ir al final de tus razones",
} as const;

// ---------------------------------------------------------------------------
// Avatares
// ---------------------------------------------------------------------------

const COLORES_AVATAR = [
  { fondo: "bg-accent-900", texto: "text-accent-300", borde: "border-accent-500/40" },
  { fondo: "bg-valid-900", texto: "text-valid-300", borde: "border-valid-500/40" },
  { fondo: "bg-term-900", texto: "text-term-300", borde: "border-term-500/40" },
  { fondo: "bg-fallacy-900", texto: "text-fallacy-300", borde: "border-fallacy-500/40" },
  { fondo: "bg-ink-800", texto: "text-ink-200", borde: "border-ink-600" },
];

/** Iniciales de un nombre: «René Descartes» → «RD». */
export function iniciales(nombre: string): string {
  const partes = nombre.trim().split(/\s+/).filter(Boolean);
  if (partes.length === 0) return "?";
  if (partes.length === 1) return partes[0].slice(0, 2).toUpperCase();
  return (partes[0][0] + partes[partes.length - 1][0]).toUpperCase();
}

/**
 * Color estable derivado del nombre. Que sea determinista importa: el mismo
 * pensador debe verse igual en el lector, en el grafo y en los desplegables.
 */
export function colorAvatar(nombre: string) {
  let hash = 0;
  for (let i = 0; i < nombre.length; i++) {
    hash = (hash * 31 + nombre.charCodeAt(i)) | 0;
  }
  return COLORES_AVATAR[Math.abs(hash) % COLORES_AVATAR.length];
}

// ---------------------------------------------------------------------------
// Colores de las relaciones del grafo
// ---------------------------------------------------------------------------

export const RELATION_COLORS = {
  REFUTA: "#c0556b",
  PRESUPONE: "#2f9e6e",
  EXTIENDE: "#6d7fd4",
  RADICALIZA: "#d99a2b",
  MATIZA: "#6f7d90",
} as const;
