"use client";

import { useState } from "react";
import { useSearchParams } from "next/navigation";
import { corpus, argumentsApi } from "@/lib/api";
import { useAsync } from "@/lib/use-async";
import { useCorpusVersion } from "@/lib/corpus-refresh";
import type { Argument, PremiseType } from "@/types/organon";
import { ErrorState } from "@/components/ui/panel";
import { ArgumentForm, type FormSeed } from "@/components/arguments/argument-form";

/**
 * Carga lo que necesita el formulario y lo monta con una `key` estable.
 *
 * La separación no es ceremonial: montar el formulario ya sembrado evita el
 * efecto que volcaba el argumento cargado en media docena de estados, que es
 * justo el patrón de renders encadenados que React desaconseja.
 */
export function BuilderClient() {
  const params = useSearchParams();
  const argumentIdParam = params.get("argumentId");
  const workIdParam = params.get("workId");
  const passageIdParam = params.get("passageId");
  const statementParam = params.get("statement");

  const [argumentId, setArgumentId] = useState<number | null>(
    argumentIdParam ? Number(argumentIdParam) : null,
  );

  const corpusVersion = useCorpusVersion();
  const works = useAsync(() => corpus.listWorks(), [corpusVersion]);
  const existing = useAsync(() => argumentsApi.get(argumentId!), [argumentId], {
    enabled: argumentId !== null,
  });

  if (works.error) {
    return (
      <div className="p-6">
        <ErrorState message={works.error} />
      </div>
    );
  }
  if (existing.error) {
    return (
      <div className="p-6">
        <ErrorState message={existing.error} />
      </div>
    );
  }

  const loadedArgument =
    argumentId !== null && existing.data?.id === argumentId ? existing.data : undefined;

  if (!works.data || (argumentId !== null && !loadedArgument)) {
    return <p className="p-6 text-sm text-ink-500">Cargando el argumento…</p>;
  }

  const seed: FormSeed = loadedArgument
    ? fromArgument(loadedArgument)
    : {
        workId: workIdParam ? Number(workIdParam) : works.data[0]?.id,
        passageId: passageIdParam ? Number(passageIdParam) : null,
        name: "",
        scheme: "NO_CLASIFICADO",
        latex: "",
        premises: statementParam
          ? [
              {
                key: "seed-0",
                id: null,
                statement: statementParam,
                enthymeme: false,
                premiseType: "EMPIRICA" as PremiseType,
              },
            ]
          : [],
      };

  return (
    <ArgumentForm
      key={argumentId ?? "nuevo"}
      works={works.data}
      argumentId={argumentId}
      loaded={loadedArgument}
      seed={seed}
      onCreated={setArgumentId}
      onReload={existing.reload}
    />
  );
}

function fromArgument(argument: Argument): FormSeed {
  return {
    workId: argument.workId,
    passageId: argument.passageId,
    name: argument.name,
    scheme: argument.formalScheme,
    latex: argument.latexFormalization ?? "",
    premises: argument.premises.map((p) => ({
      key: `guardada-${p.id}`,
      id: p.id,
      statement: p.statement,
      enthymeme: p.enthymeme,
      premiseType: p.premiseType,
    })),
  };
}
