import type {
  Argument,
  ArgumentRequest,
  AuditReport,
  ConceptComparison,
  DialecticGraph,
  DialecticalRelation,
  DialecticalRelationRequest,
  Epoch,
  Objection,
  ObjectionRequest,
  Passage,
  Philosopher,
  Premise,
  PremiseRequest,
  ResolvedTerm,
  SemanticConcept,
  TermDefinition,
  Work,
} from "@/types/organon";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";

/** Error de API que conserva el estado HTTP y el detalle del ProblemDetail. */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
    readonly detail?: unknown,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

type QueryValue = string | number | boolean | null | undefined;

function buildUrl(path: string, query?: Record<string, QueryValue>): string {
  const url = new URL(`${API_URL}${path}`);
  for (const [key, value] of Object.entries(query ?? {})) {
    if (value !== null && value !== undefined && value !== "") {
      url.searchParams.set(key, String(value));
    }
  }
  return url.toString();
}

async function request<T>(
  path: string,
  init?: RequestInit & { query?: Record<string, QueryValue> },
): Promise<T> {
  const { query, ...rest } = init ?? {};
  const response = await fetch(buildUrl(path, query), {
    ...rest,
    headers: {
      "Content-Type": "application/json",
      ...(rest.headers ?? {}),
    },
    // El corpus cambia mientras se trabaja: nunca servir una vista rancia.
    cache: "no-store",
  });

  if (!response.ok) {
    let detail: unknown;
    let message = `${response.status} ${response.statusText}`;
    try {
      detail = await response.json();
      const problem = detail as { detail?: string; title?: string };
      message = problem.detail ?? problem.title ?? message;
    } catch {
      // El backend no siempre responde JSON (por ejemplo, un 502 del proxy).
    }
    throw new ApiError(response.status, message, detail);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const contentType = response.headers.get("content-type") ?? "";
  if (contentType.includes("application/json")) {
    return (await response.json()) as T;
  }
  return (await response.text()) as T;
}

const json = (body: unknown): RequestInit => ({ body: JSON.stringify(body) });

// ---------------------------------------------------------------------------
// Corpus
// ---------------------------------------------------------------------------

export const corpus = {
  listPhilosophers: (epoch?: Epoch) =>
    request<Philosopher[]>("/corpus/philosophers", { query: { epoch } }),

  getPhilosopher: (id: number) => request<Philosopher>(`/corpus/philosophers/${id}`),

  listWorks: (philosopherId?: number) =>
    request<Work[]>("/corpus/works", { query: { philosopherId } }),

  getWork: (id: number) => request<Work>(`/corpus/works/${id}`),

  listPassages: (workId: number) => request<Passage[]>(`/corpus/works/${workId}/passages`),

  getPassage: (id: number) => request<Passage>(`/corpus/passages/${id}`),
};

// ---------------------------------------------------------------------------
// Semántica
// ---------------------------------------------------------------------------

export const semantics = {
  listConcepts: () => request<SemanticConcept[]>("/semantics/concepts"),

  compareConcept: (conceptId: number) =>
    request<ConceptComparison>(`/semantics/concepts/${conceptId}/comparison`),

  listDefinitions: (params?: { conceptId?: number; philosopherId?: number }) =>
    request<TermDefinition[]>("/semantics/definitions", { query: params }),

  /** Qué entiende este autor por este término, y qué entienden los demás. */
  resolve: (term: string, philosopherId: number, workId?: number) =>
    request<ResolvedTerm>("/semantics/resolve", { query: { term, philosopherId, workId } }),

  /** Glosario del autor para el panel derecho del lector. */
  readingContext: (philosopherId: number, workId?: number) =>
    request<TermDefinition[]>("/semantics/reading-context", {
      query: { philosopherId, workId },
    }),
};

// ---------------------------------------------------------------------------
// Argumentos
// ---------------------------------------------------------------------------

export const argumentsApi = {
  list: (params?: { workId?: number; passageId?: number }) =>
    request<Argument[]>("/arguments", { query: params }),

  get: (id: number) => request<Argument>(`/arguments/${id}`),

  create: (body: ArgumentRequest) =>
    request<Argument>("/arguments", { method: "POST", ...json(body) }),

  update: (id: number, body: ArgumentRequest) =>
    request<Argument>(`/arguments/${id}`, { method: "PUT", ...json(body) }),

  remove: (id: number) => request<void>(`/arguments/${id}`, { method: "DELETE" }),

  /** Envía el resultado del drag-and-drop: la lista completa en su orden final. */
  reorderPremises: (argumentId: number, premises: PremiseRequest[]) =>
    request<Premise[]>(`/arguments/${argumentId}/premises`, {
      method: "PUT",
      ...json(premises),
    }),

  toggleEnthymeme: (premiseId: number) =>
    request<Premise>(`/premises/${premiseId}/enthymeme`, { method: "PATCH" }),

  addObjection: (premiseId: number, body: ObjectionRequest) =>
    request<Objection>(`/premises/${premiseId}/objections`, { method: "POST", ...json(body) }),

  removeObjection: (objectionId: number) =>
    request<void>(`/objections/${objectionId}`, { method: "DELETE" }),

  /** Audita y persiste el veredicto. */
  audit: (id: number) => request<AuditReport>(`/arguments/${id}/audit`, { method: "POST" }),

  /** Audita sin escribir, para previsualizar en el constructor. */
  previewAudit: (id: number) => request<AuditReport>(`/arguments/${id}/audit`),
};

// ---------------------------------------------------------------------------
// Red dialéctica
// ---------------------------------------------------------------------------

export const dialectic = {
  graph: (params?: { epoch?: Epoch; conceptId?: number }) =>
    request<DialecticGraph>("/dialectic-graph", { query: params }),

  listRelations: () => request<DialecticalRelation[]>("/dialectic-relations"),

  createRelation: (body: DialecticalRelationRequest) =>
    request<DialecticalRelation>("/dialectic-relations", { method: "POST", ...json(body) }),

  removeRelation: (id: number) =>
    request<void>(`/dialectic-relations/${id}`, { method: "DELETE" }),
};

// ---------------------------------------------------------------------------
// Exportación
// ---------------------------------------------------------------------------

export const exports = {
  markdownUrl: (workId: number) => buildUrl(`/export/markdown/${workId}`),
  latexUrl: (argumentId: number) => buildUrl(`/export/latex/${argumentId}`),
  markdown: (workId: number) => request<string>(`/export/markdown/${workId}`),
  latex: (argumentId: number) => request<string>(`/export/latex/${argumentId}`),
};
