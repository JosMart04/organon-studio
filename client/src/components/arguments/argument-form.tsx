"use client";

import { useCallback, useMemo, useState } from "react";
import Link from "next/link";
import { Download, FileCheck2, GraduationCap, Plus, Save, Stethoscope } from "lucide-react";
import { argumentsApi, corpus, exports } from "@/lib/api";
import { useAsync } from "@/lib/use-async";
import { cn } from "@/lib/cn";
import {
  FORMAL_SCHEMES,
  type Argument,
  type AuditReport,
  type FormalScheme,
  type PremiseRequest,
  type Work,
} from "@/types/organon";
import { Button, EmptyState, ErrorState, PageHeader, Panel } from "@/components/ui/panel";
import { SeverityBadge, SoundStatusBadge } from "@/components/ui/badges";
import { TEXTOS, humanize, tituloHallazgo } from "@/lib/vocabulario";
import { Latex } from "@/components/ui/latex";
import { PremiseList, type DraftPremise } from "@/components/arguments/premise-list";
import { ObjectionPanel } from "@/components/arguments/objection-panel";
import { SocraticButton, SocraticPanel } from "@/components/ai/socratic-panel";
import type { ExtractedIdeas } from "@/types/organon";

export interface FormSeed {
  workId: number | undefined;
  passageId: number | null;
  name: string;
  scheme: FormalScheme;
  latex: string;
  premises: DraftPremise[];
}

let draftCounter = 0;

/**
 * Clave estable para una razón que aún no existe en el servidor.
 *
 * Muta un contador, así que **nunca** debe llamarse dentro de un updater de
 * `setState`: React los invoca dos veces en modo estricto para detectar
 * impurezas, y el resultado eran claves duplicadas. Se llama siempre antes,
 * y al updater le llega el objeto ya construido.
 */
const nextKey = () => `borrador-${++draftCounter}`;

export function ArgumentForm({
  works,
  argumentId,
  loaded,
  seed,
  onCreated,
  onReload,
}: {
  works: Work[];
  argumentId: number | null;
  loaded: Argument | undefined;
  seed: FormSeed;
  onCreated: (id: number) => void;
  onReload: () => void;
}) {
  const [workId, setWorkId] = useState(seed.workId);
  const [passageId, setPassageId] = useState(seed.passageId);
  const [name, setName] = useState(seed.name);
  const [scheme, setScheme] = useState(seed.scheme);
  const [latex, setLatex] = useState(seed.latex);
  const [premises, setPremises] = useState(seed.premises);

  const [saved, setSaved] = useState<Argument | undefined>(loaded);
  const [audit, setAudit] = useState<AuditReport>();
  const [busy, setBusy] = useState<"save" | "audit" | null>(null);
  const [error, setError] = useState<string>();
  const [asistenteAbierto, setAsistenteAbierto] = useState(false);

  const passages = useAsync(() => corpus.listPassages(workId!), [workId], {
    enabled: workId !== undefined,
  });

  const addPremise = useCallback(
    (premiseType: DraftPremise["premiseType"] = "EMPIRICA") => {
      const nueva: DraftPremise = {
        key: nextKey(),
        id: null,
        statement: "",
        enthymeme: false,
        premiseType,
      };
      setPremises((prev) => [...prev, nueva]);
    },
    [],
  );

  const hasConclusion = premises.some((p) => p.premiseType === "CONCLUSION");
  const canSave =
    workId !== undefined &&
    name.trim().length > 0 &&
    premises.length > 0 &&
    premises.every((p) => p.statement.trim().length > 0);

  const body = useMemo(
    () => ({
      workId: workId!,
      passageId,
      name: name.trim(),
      formalScheme: scheme,
      latexFormalization: latex.trim() || null,
      premises: premises.map<PremiseRequest>((p) => ({
        id: p.id,
        statement: p.statement.trim(),
        enthymeme: p.enthymeme,
        premiseType: p.premiseType,
      })),
    }),
    [workId, passageId, name, scheme, latex, premises],
  );

  const applyServerState = (result: Argument) => {
    setSaved(result);
    setPremises(
      result.premises.map((p) => ({
        key: `guardada-${p.id}`,
        id: p.id,
        statement: p.statement,
        enthymeme: p.enthymeme,
        premiseType: p.premiseType,
      })),
    );
  };

  const save = async () => {
    if (!canSave) return;
    setBusy("save");
    setError(undefined);
    try {
      const result = argumentId
        ? await argumentsApi.update(argumentId, body)
        : await argumentsApi.create(body);
      applyServerState(result);
      setAudit(undefined);
      if (!argumentId) onCreated(result.id);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(null);
    }
  };

  const runAudit = async () => {
    if (!argumentId) return;
    setBusy("audit");
    setError(undefined);
    try {
      const report = await argumentsApi.audit(argumentId);
      setAudit(report);
      setSaved((prev) => (prev ? { ...prev, soundStatus: report.soundStatus } : prev));
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(null);
    }
  };

  /**
   * Vuelca la propuesta del asistente en el formulario. No guarda: el lector
   * la edita y decide. Las razones existentes se conservan; la propuesta se
   * anade debajo, porque pisar lo que uno ya habia escrito seria hostil.
   */
  const usarPropuesta = (ideas: ExtractedIdeas) => {
    if (!name.trim()) setName(ideas.mainClaim);

    const nuevas: DraftPremise[] = [
      ...ideas.reasons.map((statement) => ({
        key: nextKey(),
        id: null,
        statement,
        enthymeme: false,
        premiseType: "EMPIRICA" as const,
      })),
      ...ideas.unstatedAssumptions.map((statement) => ({
        key: nextKey(),
        id: null,
        statement,
        enthymeme: true,
        premiseType: "AXIOMATICA" as const,
      })),
    ];

    // La clave se genera aquí, fuera del updater.
    const conclusionDeReserva: DraftPremise = {
      key: nextKey(),
      id: null,
      statement: ideas.mainClaim,
      enthymeme: false,
      premiseType: "CONCLUSION",
    };

    setPremises((prev) => {
      // La conclusión se mantiene al final aunque lleguen razones nuevas.
      const conclusion = prev.filter((p) => p.premiseType === "CONCLUSION");
      const resto = prev.filter((p) => p.premiseType !== "CONCLUSION");
      // Si ya había una conclusión escrita se respeta; si estaba en blanco se
      // rellena con la tesis, porque dejarla vacía bloquearía el guardado.
      const conclusionFinal =
        conclusion.length > 0
          ? conclusion.map((c) =>
              c.statement.trim() ? c : { ...c, statement: ideas.mainClaim },
            )
          : [conclusionDeReserva];
      return [...resto, ...nuevas, ...conclusionFinal];
    });
  };

  const refreshObjections = async () => {
    if (!argumentId) return;
    applyServerState(await argumentsApi.get(argumentId));
    onReload();
  };

  return (
    <div className="flex min-h-screen flex-col">
      <PageHeader
        title="Estructura de la idea"
        subtitle="Desmonta el razonamiento del autor: sus razones, a qué conclusión llega y qué da por supuesto sin decirlo."
        actions={
          <>
            {saved && <SoundStatusBadge status={saved.soundStatus} />}
            <SocraticButton onClick={() => setAsistenteAbierto(true)} />
            {argumentId && (
              <Link
                href={`/review?argumentId=${argumentId}`}
                title="Comprueba si has entendido esta idea: el asistente te hace una pregunta difícil"
                className="inline-flex items-center gap-1.5 rounded border border-ink-700 bg-ink-800 px-3 py-1.5 text-xs font-medium text-ink-200 transition-colors hover:bg-ink-700 hover:text-ink-50"
              >
                <GraduationCap className="size-3.5" />
                Desafíame
              </Link>
            )}
            <Button variant="primary" onClick={save} disabled={!canSave || busy !== null}>
              <Save className="size-3.5" />
              {busy === "save" ? "Guardando…" : argumentId ? "Guardar cambios" : "Crear"}
            </Button>
            <Button onClick={runAudit} disabled={!argumentId || busy !== null}>
              <Stethoscope className="size-3.5" />
              {busy === "audit" ? "Revisando…" : "Revisar"}
            </Button>
            {argumentId && (
              <a
                href={exports.latexUrl(argumentId)}
                className="inline-flex items-center gap-1.5 rounded border border-ink-700 bg-ink-800 px-3 py-1.5 text-xs font-medium text-ink-200 transition-colors hover:bg-ink-700 hover:text-ink-50"
              >
                <Download className="size-3.5" /> LaTeX
              </a>
            )}
          </>
        }
      />

      <div className="grid flex-1 gap-4 p-4 xl:grid-cols-[minmax(0,1fr)_24rem]">
        <div className="space-y-4">
          {error && <ErrorState message={error} />}

          <Panel title="De qué va">
            <div className="grid gap-3 sm:grid-cols-2">
              <label className="sm:col-span-2">
                <span className="mb-1 block text-[11px] text-ink-400">
                  ¿Qué idea estás desmontando?
                </span>
                <input
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="La prueba de Descartes de que Dios existe"
                  className="w-full rounded border border-ink-700 bg-ink-900 px-2.5 py-1.5 font-serif text-[13px] text-ink-100 outline-none placeholder:text-ink-600 focus:border-accent-500"
                />
              </label>

              <label>
                <span className="mb-1 block text-[11px] text-ink-400">Libro</span>
                <select
                  value={workId ?? ""}
                  onChange={(e) => {
                    setWorkId(Number(e.target.value));
                    setPassageId(null);
                  }}
                  className="w-full rounded border border-ink-700 bg-ink-900 px-2.5 py-1.5 text-xs text-ink-100 outline-none focus:border-accent-500"
                >
                  {works.map((w) => (
                    <option key={w.id} value={w.id}>
                      {w.philosopherName} — {w.title}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                <span className="mb-1 block text-[11px] text-ink-400">
                  Fragmento <span className="text-ink-600">(opcional)</span>
                </span>
                <select
                  value={passageId ?? ""}
                  onChange={(e) =>
                    setPassageId(e.target.value ? Number(e.target.value) : null)
                  }
                  className="w-full rounded border border-ink-700 bg-ink-900 px-2.5 py-1.5 font-mono text-xs text-ink-100 outline-none focus:border-accent-500"
                >
                  <option value="">Sin anclar a un fragmento</option>
                  {passages.data?.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.locator}
                    </option>
                  ))}
                </select>
              </label>

              <label className="sm:col-span-2">
                <span className="mb-1 block text-[11px] text-ink-400">¿Cómo encaja el razonamiento?</span>
                <select
                  value={scheme}
                  onChange={(e) => setScheme(e.target.value as FormalScheme)}
                  className="w-full rounded border border-ink-700 bg-ink-900 px-2.5 py-1.5 text-xs text-ink-100 outline-none focus:border-accent-500"
                >
                  {FORMAL_SCHEMES.map((s) => (
                    <option key={s} value={s}>
                      {humanize(s)}
                    </option>
                  ))}
                </select>
              </label>
            </div>
          </Panel>

          <Panel
            title={TEXTOS.razones}
            actions={
              <div className="flex gap-1.5">
                <Button size="sm" onClick={() => addPremise("EMPIRICA")}>
                  <Plus className="size-3" /> Razón
                </Button>
                <Button
                  size="sm"
                  variant="primary"
                  onClick={() => addPremise("CONCLUSION")}
                  disabled={hasConclusion}
                  title={
                    hasConclusion
                      ? "Una idea llega a una sola conclusión"
                      : undefined
                  }
                >
                  <Plus className="size-3" /> Conclusión
                </Button>
              </div>
            }
          >
            {premises.length === 0 ? (
              <EmptyState
                title="Todavía no has escrito ninguna razón."
                hint="Añade la primera, o selecciona una frase mientras lees para traerla aquí."
              />
            ) : (
              <PremiseList premises={premises} onChange={setPremises} />
            )}
          </Panel>

          {saved && <ObjectionPanel argument={saved} onChanged={refreshObjections} />}
        </div>

        <div className="space-y-4">
          <Panel title={TEXTOS.revision}>
            {!argumentId && (
              <p className="text-[12px] leading-relaxed text-ink-500">
                Guarda primero y luego pulsa «Revisar»: te dirá si el razonamiento se
                sostiene y dónde flaquea.
              </p>
            )}

            {argumentId && !audit && (
              <p className="text-[12px] leading-relaxed text-ink-500">
                Sin revisar todavía. Comprueba que llegues a una conclusión, que el
                orden se entienda y que hayas discutido lo que el autor da por
                supuesto sin decirlo.
              </p>
            )}

            {audit && (
              <div className="space-y-3">
                <div className="flex items-center gap-2">
                  <FileCheck2 className="size-4 text-ink-500" />
                  <SoundStatusBadge status={audit.soundStatus} />
                </div>

                <dl className="grid grid-cols-3 gap-2 text-center">
                  {(
                    [
                      ["Razones", audit.premiseCount],
                      ["Supuestos", audit.enthymemeCount],
                      ["Críticas", audit.objectionCount],
                    ] as const
                  ).map(([label, value]) => (
                    <div
                      key={label}
                      className="rounded border border-ink-800 bg-ink-950 px-2 py-1.5"
                    >
                      <dd className="font-mono text-sm text-ink-100">{value}</dd>
                      <dt className="text-[10px] uppercase tracking-wider text-ink-500">
                        {label}
                      </dt>
                    </div>
                  ))}
                </dl>

                <ul className="space-y-2">
                  {audit.findings.map((finding, i) => (
                    <li
                      key={`${finding.code}-${i}`}
                      className={cn(
                        "rounded border p-2.5",
                        finding.severity === "BLOQUEANTE"
                          ? "border-fallacy-500/30 bg-fallacy-900/25"
                          : finding.severity === "ADVERTENCIA"
                            ? "border-accent-500/25 bg-accent-900/25"
                            : "border-ink-800 bg-ink-950",
                      )}
                    >
                      <div className="flex items-center gap-2">
                        <SeverityBadge severity={finding.severity} />
                        <span className="text-[11px] font-medium text-ink-200">
                          {tituloHallazgo(finding.code)}
                        </span>
                      </div>
                      <p className="mt-1.5 text-[12px] leading-relaxed text-ink-400">
                        {finding.message}
                      </p>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </Panel>

          {/*
            La notación formal deja de estar en el centro. Quien lee filosofía
            por gusto no necesita ver LaTeX para desmontar un argumento; quien
            quiera curiosear, lo despliega. <details> nativo: accesible por
            teclado de fábrica y sin estado que gestionar.
          */}
          <details className="group rounded-lg border border-ink-800 bg-ink-900/60">
            <summary className="flex cursor-pointer list-none items-center gap-2 px-4 py-2.5 text-[11px] font-semibold uppercase tracking-[0.12em] text-ink-400 transition-colors hover:text-ink-200">
              <span aria-hidden>🔬</span>
              {TEXTOS.notacionFormal}
              <span className="ml-auto text-ink-600 transition-transform group-open:rotate-90">
                &rsaquo;
              </span>
            </summary>

            <div className="border-t border-ink-800 p-4">
              <p className="mb-2.5 text-[11px] leading-relaxed text-ink-500">
                Opcional, y solo para quien le interese. La idea se sostiene o se
                cae exactamente igual sin esto.
              </p>
              <textarea
                value={latex}
                onChange={(e) => setLatex(e.target.value)}
                rows={4}
                spellCheck={false}
                placeholder={"P \\to Q \\;\\wedge\\; P \\;\\vdash\\; Q"}
                className="w-full resize-y rounded border border-ink-700 bg-ink-900 px-2.5 py-2 font-mono text-[11.5px] leading-relaxed text-ink-100 outline-none placeholder:text-ink-600 focus:border-accent-500"
              />
              {latex.trim() && (
                <div className="mt-3 overflow-x-auto rounded border border-ink-800 bg-ink-950 px-3 py-3">
                  <Latex expression={latex} display className="text-ink-100" />
                </div>
              )}
            </div>
          </details>
        </div>
      </div>

      <SocraticPanel
        open={asistenteAbierto}
        onClose={() => setAsistenteAbierto(false)}
        // Lo que hay escrito es el material a analizar: las razones si las hay,
        // y si no, el nombre de la idea.
        text={
          premises.map((p) => p.statement).filter(Boolean).join(" ").trim() || name.trim()
        }
        author={works.find((w) => w.id === workId)?.philosopherName}
        workTitle={works.find((w) => w.id === workId)?.title}
        onUseIdeas={usarPropuesta}
      />
    </div>
  );
}
