"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import {
  Background,
  BackgroundVariant,
  Controls,
  MarkerType,
  MiniMap,
  ReactFlow,
  useEdgesState,
  useNodesState,
  type Edge,
  type Node,
} from "@xyflow/react";
import { X } from "lucide-react";
import { dialectic, semantics } from "@/lib/api";
import { useAsync } from "@/lib/use-async";
import { useCorpusVersion } from "@/lib/corpus-refresh";
import { cn } from "@/lib/cn";
import { EPOCHS, type Epoch, type GraphEdge, type GraphNode } from "@/types/organon";
import { RELATION_COLORS, humanize } from "@/lib/vocabulario";
import { EmptyState, ErrorState, PageHeader } from "@/components/ui/panel";
import { Avatar } from "@/components/ui/badges";
import { NODE_TYPES } from "@/components/graph/graph-nodes";
import { ExportarMapa } from "@/components/graph/export-buttons";

const MINIMAP_COLORS: Record<string, string> = {
  philosopher: "#4e5a6b",
  work: "#38414f",
  argument: "#d99a2b",
};

export default function GraphPage() {
  const [epoch, setEpoch] = useState<Epoch | "">("");
  const [conceptId, setConceptId] = useState<number | "">("");
  const [seleccionado, setSeleccionado] = useState<Node | null>(null);

  const corpusVersion = useCorpusVersion();
  const concepts = useAsync(() => semantics.listConcepts(), [corpusVersion]);
  const graph = useAsync(
    () =>
      dialectic.graph({
        epoch: epoch || undefined,
        conceptId: conceptId === "" ? undefined : conceptId,
      }),
    [epoch, conceptId, corpusVersion],
  );

  const [nodes, setNodes, onNodesChange] = useNodesState<Node>([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([]);

  const styledEdges = useMemo<Edge[]>(() => {
    if (!graph.data) return [];
    return graph.data.edges.map((edge) => {
      const dialectica = edge.type === "dialectical";
      const adversaria = edge.type === "adversary";
      const color = dialectica
        ? (RELATION_COLORS[edge.label as keyof typeof RELATION_COLORS] ?? "#6f7d90")
        : adversaria
          ? "#4e5a6b"
          : "#272e39";

      return {
        id: edge.id,
        source: edge.source,
        target: edge.target,
        // Las aristas del debate salen de lado; las de estructura, de arriba
        // abajo. Así el armazón no se confunde con lo que de verdad se discute.
        sourceHandle: dialectica ? "dialectic" : adversaria ? "adversary" : undefined,
        targetHandle: dialectica ? "dialectic" : adversaria ? "adversary" : undefined,
        type: "smoothstep",
        animated: edge.animated,
        label: dialectica ? humanize(edge.label) : adversaria ? "escribe contra" : undefined,
        labelStyle: { fill: color, fontSize: 10, fontWeight: 600 },
        labelBgStyle: { fill: "#12151a" },
        labelBgPadding: [4, 2] as [number, number],
        labelBgBorderRadius: 3,
        style: {
          stroke: color,
          strokeWidth: dialectica ? 2 : 1,
          strokeDasharray: edge.label === "PRESUPONE" ? "5 4" : undefined,
          opacity: dialectica ? 1 : 0.55,
        },
        markerEnd: dialectica
          ? { type: MarkerType.ArrowClosed, color, width: 16, height: 16 }
          : undefined,
      };
    });
  }, [graph.data]);

  useEffect(() => {
    if (!graph.data) return;
    setNodes(
      graph.data.nodes.map((n) => ({
        id: n.id,
        type: n.type,
        position: n.position,
        data: n.data,
      })),
    );
    setEdges(styledEdges);
  }, [graph.data, styledEdges, setNodes, setEdges]);

  const nodeColor = useCallback(
    (node: Node) => MINIMAP_COLORS[node.type ?? ""] ?? "#38414f",
    [],
  );

  const vacio = !graph.loading && (graph.data?.nodes.length ?? 0) === 0;

  return (
    <div className="flex h-screen flex-col">
      <PageHeader
        title="El debate"
        subtitle="Quién discute con quién, y en qué. Cada flecha de color es un desacuerdo concreto entre dos ideas."
        actions={
          <>
            <select
              value={epoch}
              onChange={(e) => setEpoch(e.target.value as Epoch | "")}
              className="rounded border border-ink-700 bg-ink-850 px-2.5 py-1.5 text-xs text-ink-100 outline-none focus:border-accent-500"
            >
              <option value="">Todas las épocas</option>
              {EPOCHS.map((e) => (
                <option key={e} value={e}>
                  {humanize(e)}
                </option>
              ))}
            </select>
            <select
              value={conceptId}
              onChange={(e) => setConceptId(e.target.value ? Number(e.target.value) : "")}
              className="rounded border border-ink-700 bg-ink-850 px-2.5 py-1.5 text-xs text-ink-100 outline-none focus:border-accent-500"
              title="Deja solo a los autores que discuten esta palabra"
            >
              <option value="">Todas las palabras</option>
              {concepts.data?.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.term}
                </option>
              ))}
            </select>
            <ExportarMapa
              nodos={nodes}
              epoca={epoch || undefined}
              palabra={concepts.data?.find((c) => c.id === conceptId)?.term}
              disabled={graph.loading || Boolean(graph.error) || vacio}
            />
          </>
        }
      />

      {graph.error ? (
        <div className="p-6">
          <ErrorState message={graph.error} />
        </div>
      ) : vacio ? (
        <div className="p-6">
          <EmptyState
            title="Tu cuaderno todavía está vacío."
            hint="Añade un pensador y un libro desde el menú de la izquierda, y desmonta una idea. El mapa se dibuja solo."
          />
        </div>
      ) : (
        <div className="relative min-h-0 flex-1">
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onNodeClick={(_, node) => setSeleccionado(node)}
            onPaneClick={() => setSeleccionado(null)}
            nodeTypes={NODE_TYPES}
            fitView
            fitViewOptions={{ padding: 0.15 }}
            minZoom={0.2}
            maxZoom={1.8}
            className="bg-ink-950"
          >
            <Background variant={BackgroundVariant.Dots} gap={22} size={1} color="#272e39" />
            <Controls
              className="!border !border-ink-700 !bg-ink-850 !shadow-lg"
              showInteractive={false}
            />
            <MiniMap
              nodeColor={nodeColor}
              maskColor="rgba(11, 13, 16, 0.75)"
              className="!border !border-ink-700"
              pannable
              zoomable
            />
          </ReactFlow>

          <Leyenda />

          {seleccionado && (
            <TarjetaNodo
              node={seleccionado}
              edges={graph.data?.edges ?? []}
              nodes={graph.data?.nodes ?? []}
              onClose={() => setSeleccionado(null)}
            />
          )}
        </div>
      )}
    </div>
  );
}

function Leyenda() {
  // Arriba a la derecha: abajo a la izquierda están los controles de zoom, abajo
  // a la derecha el minimapa, y arriba a la izquierda es justo donde fitView
  // deja la primera fila de nodos.
  return (
    <aside className="pointer-events-none absolute right-4 top-4 z-10 w-60 rounded-lg border border-ink-700 bg-ink-900/90 p-3 backdrop-blur">
      <p className="text-[11px] leading-relaxed text-ink-300">
        Un <span className="text-ink-100">autor</span> escribe un{" "}
        <span className="text-ink-100">libro</span>, y un libro contiene{" "}
        <span className="text-accent-300">ideas</span>. Las flechas de colores unen
        ideas que discuten entre sí.
      </p>

      <ul className="mt-2.5 space-y-1.5 border-t border-ink-800 pt-2.5">
        {(Object.keys(RELATION_COLORS) as (keyof typeof RELATION_COLORS)[]).map((rel) => (
          <li key={rel} className="flex items-center gap-2">
            <span
              className="h-0.5 w-6 shrink-0 rounded"
              style={{
                backgroundColor: RELATION_COLORS[rel],
                backgroundImage:
                  rel === "PRESUPONE"
                    ? `repeating-linear-gradient(90deg, ${RELATION_COLORS[rel]} 0 5px, transparent 5px 9px)`
                    : undefined,
              }}
            />
            <span className="text-[11px] text-ink-300">{humanize(rel)}</span>
          </li>
        ))}
      </ul>

      <p className="mt-2.5 border-t border-ink-800 pt-2 text-[10px] text-ink-600">
        Pulsa un nodo para ver el detalle.
      </p>
    </aside>
  );
}

/** Tarjeta lateral con la disputa en la que participa el nodo, explicada. */
function TarjetaNodo({
  node,
  edges,
  nodes,
  onClose,
}: {
  node: Node;
  edges: GraphEdge[];
  nodes: GraphNode[];
  onClose: () => void;
}) {
  const data = node.data as Record<string, unknown>;
  const texto = (clave: string) => {
    const valor = data[clave];
    return typeof valor === "string" || typeof valor === "number" ? String(valor) : "";
  };

  const etiqueta = (id: string) => String(nodes.find((n) => n.id === id)?.data?.label ?? id);

  const disputas = edges.filter(
    (e) => e.type === "dialectical" && (e.source === node.id || e.target === node.id),
  );

  const esIdea = node.type === "argument";
  const argumentId = esIdea ? node.id.replace("argument-", "") : null;
  const esLibro = node.type === "work";

  return (
    <aside className="absolute bottom-4 left-4 z-20 max-h-[calc(100%-2rem)] w-[min(88vw,24rem)] overflow-y-auto rounded-lg border border-ink-700 bg-ink-900/95 shadow-2xl shadow-black/60 backdrop-blur">
      <header className="flex items-start justify-between gap-2 border-b border-ink-800 px-4 py-3">
        <div className="flex min-w-0 items-center gap-2.5">
          {node.type === "philosopher" && (
            <Avatar name={texto("label")} emoji={texto("avatarEmoji")} />
          )}
          <div className="min-w-0">
            <p className="font-serif text-[15px] leading-snug text-ink-50">
              {texto("label")}
            </p>
            <p className="text-[10px] uppercase tracking-wider text-ink-500">
              {node.type === "philosopher" ? "Pensador" : esLibro ? "Libro" : "Idea"}
            </p>
          </div>
        </div>
        <button
          type="button"
          onClick={onClose}
          aria-label="Cerrar"
          className="shrink-0 rounded p-1 text-ink-500 transition-colors hover:bg-ink-800 hover:text-ink-200"
        >
          <X className="size-4" />
        </button>
      </header>

      <div className="space-y-3 px-4 py-3">
        {node.type === "philosopher" && texto("school") && (
          <p className="text-[12px] text-ink-400">
            {humanize(texto("epoch"))} · {texto("school")}
          </p>
        )}

        {esLibro && texto("coreThesis") && (
          <p className="prose-source text-[12.5px] text-ink-200">{texto("coreThesis")}</p>
        )}

        {esIdea && (
          <p className="text-[12px] text-ink-400">
            {texto("premiseCount")} razones
            {Number(data.enthymemeCount) > 0 &&
              ` · ${texto("enthymemeCount")} supuesto${Number(data.enthymemeCount) === 1 ? "" : "s"} implícito${Number(data.enthymemeCount) === 1 ? "" : "s"}`}
          </p>
        )}

        {disputas.length > 0 ? (
          <div className="space-y-2 border-t border-ink-800 pt-3">
            <p className="text-[10px] font-semibold uppercase tracking-[0.12em] text-ink-400">
              En disputa
            </p>
            {disputas.map((e) => {
              const saliente = e.source === node.id;
              const otro = saliente ? e.target : e.source;
              const color =
                RELATION_COLORS[e.label as keyof typeof RELATION_COLORS] ?? "#6f7d90";
              const verbo = humanize(e.label).toLowerCase();
              const descripcion =
                typeof e.data?.description === "string" ? e.data.description : "";
              return (
                <div key={e.id} className="rounded border border-ink-800 bg-ink-950 p-2.5">
                  <p className="text-[12px] leading-relaxed text-ink-200">
                    {saliente ? (
                      <>
                        Esta idea{" "}
                        <span style={{ color }} className="font-medium">
                          {verbo}
                        </span>{" "}
                        <span className="text-ink-100">{etiqueta(otro)}</span>
                      </>
                    ) : (
                      <>
                        <span className="text-ink-100">{etiqueta(otro)}</span>{" "}
                        <span style={{ color }} className="font-medium">
                          {verbo}
                        </span>{" "}
                        esta idea
                      </>
                    )}
                  </p>
                  {descripcion && (
                    <p className="mt-1.5 text-[12px] leading-relaxed text-ink-400">
                      {descripcion}
                    </p>
                  )}
                </div>
              );
            })}
          </div>
        ) : (
          esIdea && (
            <p className="border-t border-ink-800 pt-3 text-[12px] text-ink-500">
              Esta idea todavía no discute con ninguna otra. Pregúntale al asistente
              quién le llevaría la contraria.
            </p>
          )
        )}

        {argumentId && (
          <div className="border-t border-ink-800 pt-3">
            <Link
              href={`/arguments/builder?argumentId=${argumentId}`}
              className={cn(
                "inline-block rounded border border-ink-700 bg-ink-800 px-2.5 py-1 text-[11px] text-ink-200",
                "transition-colors hover:bg-ink-700 hover:text-ink-50",
              )}
            >
              Ver sus razones →
            </Link>
          </div>
        )}
      </div>
    </aside>
  );
}
