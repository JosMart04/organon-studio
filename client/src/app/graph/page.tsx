"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
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
import { dialectic, semantics } from "@/lib/api";
import { useAsync } from "@/lib/use-async";
import { cn } from "@/lib/cn";
import { EPOCHS, type Epoch } from "@/types/organon";
import { RELATION_COLORS, humanize } from "@/components/ui/badges";
import { ErrorState, PageHeader } from "@/components/ui/panel";
import { NODE_TYPES } from "@/components/graph/graph-nodes";

const MINIMAP_COLORS: Record<string, string> = {
  philosopher: "#4e5a6b",
  work: "#38414f",
  argument: "#d99a2b",
};

export default function GraphPage() {
  const [epoch, setEpoch] = useState<Epoch | "">("");
  const [conceptId, setConceptId] = useState<number | "">("");

  const concepts = useAsync(() => semantics.listConcepts(), []);
  const graph = useAsync(
    () =>
      dialectic.graph({
        epoch: epoch || undefined,
        conceptId: conceptId === "" ? undefined : conceptId,
      }),
    [epoch, conceptId],
  );

  const [nodes, setNodes, onNodesChange] = useNodesState<Node>([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([]);

  const styledEdges = useMemo<Edge[]>(() => {
    if (!graph.data) return [];
    return graph.data.edges.map((edge) => {
      const dialectical = edge.type === "dialectical";
      const adversary = edge.type === "adversary";
      const color = dialectical
        ? (RELATION_COLORS[edge.label as keyof typeof RELATION_COLORS] ?? "#6f7d90")
        : adversary
          ? "#4e5a6b"
          : "#272e39";

      return {
        id: edge.id,
        source: edge.source,
        target: edge.target,
        // Las aristas dialécticas salen de lado; las estructurales, de arriba
        // abajo. Así el armazón no se confunde con el contenido filosófico.
        sourceHandle: dialectical ? "dialectic" : adversary ? "adversary" : undefined,
        targetHandle: dialectical ? "dialectic" : adversary ? "adversary" : undefined,
        type: "smoothstep",
        animated: edge.animated,
        label: dialectical || adversary ? humanize(edge.label) : undefined,
        labelStyle: { fill: color, fontSize: 10, fontWeight: 600 },
        labelBgStyle: { fill: "#12151a" },
        labelBgPadding: [4, 2] as [number, number],
        labelBgBorderRadius: 3,
        style: {
          stroke: color,
          strokeWidth: dialectical ? 2 : 1,
          strokeDasharray: edge.label === "PRESUPONE" ? "5 4" : undefined,
          opacity: dialectical ? 1 : 0.55,
        },
        markerEnd: dialectical
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

  const dialecticalCount = graph.data?.edges.filter((e) => e.type === "dialectical").length ?? 0;

  return (
    <div className="flex h-screen flex-col">
      <PageHeader
        title="Grafo dialéctico"
        subtitle="Quién refuta, presupone, extiende o radicaliza a quién. El debate como red navegable en vez de como bibliografía."
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
              title="Aísla a los autores que discuten un concepto concreto"
            >
              <option value="">Todos los conceptos</option>
              {concepts.data?.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.term}
                </option>
              ))}
            </select>
          </>
        }
      />

      {graph.error ? (
        <div className="p-6">
          <ErrorState message={graph.error} />
        </div>
      ) : (
        <div className="relative min-h-0 flex-1">
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            nodeTypes={NODE_TYPES}
            fitView
            fitViewOptions={{ padding: 0.15 }}
            minZoom={0.2}
            maxZoom={1.8}
            proOptions={{ hideAttribution: false }}
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

          <Legend
            nodeCount={graph.data?.nodes.length ?? 0}
            dialecticalCount={dialecticalCount}
            loading={graph.loading}
          />
        </div>
      )}
    </div>
  );
}

function Legend({
  nodeCount,
  dialecticalCount,
  loading,
}: {
  nodeCount: number;
  dialecticalCount: number;
  loading: boolean;
}) {
  // Arriba a la derecha: abajo a la izquierda estan los controles de zoom, abajo
  // a la derecha el minimapa, y arriba a la izquierda es justo donde fitView
  // deja la primera fila de nodos.
  return (
    <aside className="pointer-events-none absolute right-4 top-4 z-10 w-56 rounded-lg border border-ink-700 bg-ink-900/90 p-3 backdrop-blur">
      <h2 className="text-[10px] font-semibold uppercase tracking-[0.12em] text-ink-400">
        Relaciones
      </h2>
      <ul className="mt-2 space-y-1.5">
        {(Object.keys(RELATION_COLORS) as (keyof typeof RELATION_COLORS)[]).map((rel) => (
          <li key={rel} className="flex items-center gap-2">
            <span
              className={cn("h-0.5 w-6 shrink-0 rounded", rel === "PRESUPONE" && "opacity-70")}
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
      <p className="mt-3 border-t border-ink-800 pt-2 font-mono text-[10px] text-ink-500">
        {loading ? "cargando…" : `${nodeCount} nodos · ${dialecticalCount} aristas dialécticas`}
      </p>
    </aside>
  );
}
