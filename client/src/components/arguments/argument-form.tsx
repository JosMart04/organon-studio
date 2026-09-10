"use client";

import { useCallback, useMemo, useState } from "react";
import { Download, FileCheck2, Plus, Save, Stethoscope } from "lucide-react";
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
import { SeverityBadge, SoundStatusBadge, humanize } from "@/components/ui/badges";
import { Latex } from "@/components/ui/latex";
import { PremiseList, type DraftPremise } from "@/components/arguments/premise-list";
import { ObjectionPanel } from "@/components/arguments/objection-panel";

export interface FormSeed {
  workId: number | undefined;
  passageId: number | null;
  name: string;
  scheme: FormalScheme;
  latex: string;
  premises: DraftPremise[];
}

let draftCounter = 0;
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

  const passages = useAsync(() => corpus.listPassages(workId!), [workId], {
    enabled: workId !== undefined,
  });

  const addPremise = useCallback(
    (premiseType: DraftPremise["premiseType"] = "EMPIRICA") =>
      setPremises((prev) => [
        ...prev,
        { key: nextKey(), id: null, statement: "", enthymeme: false, premiseType },
      ]),
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

  const refreshObjections = async () => {
    if (!argumentId) return;
    applyServerState(await argumentsApi.get(argumentId));
    onReload();
  };

  return (
    <div className="flex min-h-screen flex-col">
      <PageHeader
        title="Constructor de argumentos"
        subtitle="Reconstrucción en forma estándar. Arrastra para ordenar, marca los supuestos implícitos y formaliza la inferencia."
        actions={
          <>
            {saved && <SoundStatusBadge status={saved.soundStatus} />}
            <Button variant="primary" onClick={save} disabled={!canSave || busy !== null}>
              <Save className="size-3.5" />
              {busy === "save" ? "Guardando…" : argumentId ? "Guardar cambios" : "Crear"}
            </Button>
            <Button onClick={runAudit} disabled={!argumentId || busy !== null}>
              <Stethoscope className="size-3.5" />
              {busy === "audit" ? "Auditando…" : "Auditar"}
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

          <Panel title="Identificación">
            <div className="grid gap-3 sm:grid-cols-2">
              <label className="sm:col-span-2">
                <span className="mb-1 block text-[11px] text-ink-400">
                  Nombre del argumento
                </span>
                <input
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="Prueba de Dios por la realidad objetiva de la idea de infinito"
                  className="w-full rounded border border-ink-700 bg-ink-900 px-2.5 py-1.5 font-serif text-[13px] text-ink-100 outline-none placeholder:text-ink-600 focus:border-accent-500"
                />
              </label>

              <label>
                <span className="mb-1 block text-[11px] text-ink-400">Obra</span>
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
                  Pasaje <span className="text-ink-600">(opcional)</span>
                </span>
                <select
                  value={passageId ?? ""}
                  onChange={(e) =>
                    setPassageId(e.target.value ? Number(e.target.value) : null)
                  }
                  className="w-full rounded border border-ink-700 bg-ink-900 px-2.5 py-1.5 font-mono text-xs text-ink-100 outline-none focus:border-accent-500"
                >
                  <option value="">Sin anclar a un pasaje</option>
                  {passages.data?.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.locator}
                    </option>
                  ))}
                </select>
              </label>

              <label className="sm:col-span-2">
                <span className="mb-1 block text-[11px] text-ink-400">Esquema formal</span>
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
            title="Forma estándar"
            actions={
              <div className="flex gap-1.5">
                <Button size="sm" onClick={() => addPremise("EMPIRICA")}>
                  <Plus className="size-3" /> Premisa
                </Button>
                <Button
                  size="sm"
                  variant="primary"
                  onClick={() => addPremise("CONCLUSION")}
                  disabled={hasConclusion}
                  title={
                    hasConclusion
                      ? "Un argumento en forma estándar sostiene una sola conclusión"
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
                title="Sin premisas todavía."
                hint="Añade la primera, o selecciona una frase en el lector para traerla aquí."
              />
            ) : (
              <PremiseList premises={premises} onChange={setPremises} />
            )}
          </Panel>

          {saved && <ObjectionPanel argument={saved} onChanged={refreshObjections} />}
        </div>

        <div className="space-y-4">
          <Panel title="Formalización">
            <textarea
              value={latex}
              onChange={(e) => setLatex(e.target.value)}
              rows={5}
              spellCheck={false}
              placeholder={"P \\to Q \\;\\wedge\\; P \\;\\vdash\\; Q"}
              className="w-full resize-y rounded border border-ink-700 bg-ink-900 px-2.5 py-2 font-mono text-[11.5px] leading-relaxed text-ink-100 outline-none placeholder:text-ink-600 focus:border-accent-500"
            />
            <div className="mt-3 min-h-[3.5rem] overflow-x-auto rounded border border-ink-800 bg-ink-950 px-3 py-3">
              {latex.trim() ? (
                <Latex expression={latex} display className="text-ink-100" />
              ) : (
                <p className="text-center text-[11px] text-ink-600">
                  La fórmula se previsualiza aquí mientras escribes
                </p>
              )}
            </div>
          </Panel>

          <Panel title="Auditoría">
            {!argumentId && (
              <p className="text-[12px] leading-relaxed text-ink-500">
                Guarda el argumento para poder someterlo a las pruebas de estrés.
              </p>
            )}

            {argumentId && !audit && (
              <p className="text-[12px] leading-relaxed text-ink-500">
                Sin auditar en esta sesión. La auditoría comprueba que la
                reconstrucción cierre, que el orden sea legible y que los supuestos
                implícitos hayan sido examinados.
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
                      ["Premisas", audit.premiseCount],
                      ["Entimemas", audit.enthymemeCount],
                      ["Objeciones", audit.objectionCount],
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
                        <code className="font-mono text-[10px] text-ink-500">
                          {finding.code}
                        </code>
                      </div>
                      <p className="mt-1.5 text-[12px] leading-relaxed text-ink-200">
                        {finding.message}
                      </p>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </Panel>
        </div>
      </div>
    </div>
  );
}
