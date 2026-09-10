/**
 * Espejo de los DTO del backend. Se mantiene a mano en vez de generarse:
 * el contrato es pequeño y estable, y tenerlo legible aquí documenta el
 * dominio para quien trabaje solo en el cliente.
 */

// ---------------------------------------------------------------------------
// Enumeraciones (idénticas a las del backend)
// ---------------------------------------------------------------------------

export const EPOCHS = ["ANTIGUA", "MEDIEVAL", "MODERNA", "CONTEMPORANEA"] as const;
export type Epoch = (typeof EPOCHS)[number];

export const FORMAL_SCHEMES = [
  "MODUS_PONENS",
  "MODUS_TOLLENS",
  "SILOGISMO_CATEGORICO",
  "SILOGISMO_DISYUNTIVO",
  "SILOGISMO_HIPOTETICO",
  "REDUCTIO_AD_ABSURDUM",
  "INDUCTIVE",
  "ABDUCTIVE",
  "TRASCENDENTAL",
  "ANALOGICO",
  "NO_CLASIFICADO",
] as const;
export type FormalScheme = (typeof FORMAL_SCHEMES)[number];

export const SOUND_STATUSES = ["PENDIENTE", "SOLIDO", "FALAZ"] as const;
export type SoundStatus = (typeof SOUND_STATUSES)[number];

export const PREMISE_TYPES = [
  "AXIOMATICA",
  "EMPIRICA",
  "DEFINICION",
  "INFERENCIA_INTERMEDIA",
  "CONCLUSION",
] as const;
export type PremiseType = (typeof PREMISE_TYPES)[number];

export const OBJECTION_TYPES = [
  "CONTRAEJEMPLO",
  "FALACIA_FORMAL",
  "PETICION_DE_PRINCIPIO",
  "REDEFINICION",
  "REGRESO_AL_INFINITO",
  "FALSA_DICOTOMIA",
  "AMBIGUEDAD",
  "GENERALIZACION_APRESURADA",
] as const;
export type ObjectionType = (typeof OBJECTION_TYPES)[number];

export const RELATION_TYPES = [
  "REFUTA",
  "PRESUPONE",
  "EXTIENDE",
  "RADICALIZA",
  "MATIZA",
] as const;
export type RelationType = (typeof RELATION_TYPES)[number];

export type DefinitionScope = "OBRA" | "AUTOR" | "SIN_DEFINICION";
export type AuditSeverity = "BLOQUEANTE" | "ADVERTENCIA" | "INFORMATIVA";

// ---------------------------------------------------------------------------
// Corpus
// ---------------------------------------------------------------------------

export interface Philosopher {
  id: number;
  name: string;
  epoch: Epoch;
  school: string | null;
  /** «¿Quién fue, en una línea?» */
  biographicalSummary: string | null;
  /** Emoji con el que se le reconoce. Si falta, la interfaz pinta sus iniciales. */
  avatarEmoji: string | null;
}

export interface Work {
  id: number;
  philosopherId: number;
  philosopherName: string;
  title: string;
  originalYear: number | null;
  philosophicalProblem: string | null;
  coreThesis: string | null;
  directAdversaryId: number | null;
  directAdversaryTitle: string | null;
}

export interface Passage {
  id: number;
  workId: number;
  workTitle: string;
  /** Página, capítulo o referencia canónica. */
  locator: string;
  /** Lo que dice el texto. */
  textContent: string;
  pageNumber: number | null;
  /** Lo que piensa el lector. Aparte a propósito. */
  personalNotes: string | null;
}

// ---------------------------------------------------------------------------
// Semántica
// ---------------------------------------------------------------------------

export interface SemanticConcept {
  id: number;
  term: string;
  description: string | null;
  definitionCount: number;
}

export interface TermDefinition {
  id: number;
  conceptId: number;
  term: string;
  philosopherId: number;
  philosopherName: string;
  workId: number | null;
  workTitle: string | null;
  operationalDefinition: string;
  notes: string | null;
}

export interface ResolvedTerm {
  term: string;
  definition: TermDefinition | null;
  scope: DefinitionScope;
  rivalReadings: TermDefinition[];
}

export interface ConceptComparison {
  conceptId: number;
  term: string;
  description: string | null;
  readings: TermDefinition[];
}

// ---------------------------------------------------------------------------
// Lógica
// ---------------------------------------------------------------------------

export interface Objection {
  id: number;
  premiseId: number;
  objectionType: ObjectionType;
  explanation: string;
}

export interface Premise {
  id: number;
  orderIndex: number;
  statement: string;
  enthymeme: boolean;
  premiseType: PremiseType;
  objections: Objection[];
}

export interface Argument {
  id: number;
  workId: number;
  workTitle: string;
  philosopherName: string;
  passageId: number | null;
  passageLocator: string | null;
  name: string;
  formalScheme: FormalScheme;
  latexFormalization: string | null;
  soundStatus: SoundStatus;
  premises: Premise[];
}

export interface AuditFinding {
  code: string;
  severity: AuditSeverity;
  message: string;
  premiseId: number | null;
}

export interface AuditReport {
  argumentId: number;
  argumentName: string;
  soundStatus: SoundStatus;
  premiseCount: number;
  enthymemeCount: number;
  objectionCount: number;
  findings: AuditFinding[];
}

// ---------------------------------------------------------------------------
// Red dialéctica
// ---------------------------------------------------------------------------

export interface DialecticalRelation {
  id: number;
  sourceArgumentId: number;
  sourceArgumentName: string;
  targetArgumentId: number;
  targetArgumentName: string;
  relationType: RelationType;
  description: string | null;
}

export interface GraphNode {
  id: string;
  type: "philosopher" | "work" | "argument";
  data: Record<string, unknown>;
  position: { x: number; y: number };
}

export interface GraphEdge {
  id: string;
  source: string;
  target: string;
  type: "structural" | "adversary" | "dialectical";
  label: string;
  animated: boolean;
  data: Record<string, unknown>;
}

export interface DialecticGraph {
  nodes: GraphNode[];
  edges: GraphEdge[];
}

// ---------------------------------------------------------------------------
// Cuerpos de petición
// ---------------------------------------------------------------------------

export interface PremiseRequest {
  id: number | null;
  statement: string;
  enthymeme: boolean;
  premiseType: PremiseType;
}

export interface ArgumentRequest {
  workId: number;
  passageId: number | null;
  name: string;
  formalScheme: FormalScheme;
  latexFormalization: string | null;
  premises: PremiseRequest[];
}

export interface PhilosopherRequest {
  name: string;
  epoch: Epoch;
  school: string | null;
  biographicalSummary: string | null;
  avatarEmoji: string | null;
}

export interface WorkRequest {
  philosopherId: number;
  title: string;
  originalYear: number | null;
  /** La gran pregunta que intenta responder el libro. */
  philosophicalProblem: string | null;
  coreThesis: string | null;
  directAdversaryId: number | null;
}

export interface PassageRequest {
  workId: number;
  locator: string;
  textContent: string;
  pageNumber: number | null;
  personalNotes: string | null;
}

export interface ObjectionRequest {
  objectionType: ObjectionType;
  explanation: string;
}

export interface DialecticalRelationRequest {
  sourceArgumentId: number;
  targetArgumentId: number;
  relationType: RelationType;
  description: string | null;
}

// ---------------------------------------------------------------------------
// Asistente socrático
// ---------------------------------------------------------------------------

export interface AiStatus {
  available: boolean;
  configuredModel: string;
  modelReady: boolean;
  installedModels: string[];
  /** Explicación ya redactada, lista para mostrar tal cual. */
  message: string;
}

export interface ExplainResponse {
  explanation: string;
}

/** Propuesta de desglose. Nada de esto está guardado todavía. */
export interface ExtractedIdeas {
  mainClaim: string;
  reasons: string[];
  unstatedAssumptions: string[];
}

export interface RivalSuggestion {
  thinker: string;
  work: string;
  relation: RelationType;
  explanation: string;
  /** Si ya existe en el cuaderno, la conexión se puede crear con un clic. */
  inNotebook: boolean;
}

export interface RivalSuggestions {
  suggestions: RivalSuggestion[];
}
