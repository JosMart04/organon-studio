import { Suspense } from "react";
import type { Metadata } from "next";
import { BuilderClient } from "@/components/arguments/builder-client";

export const metadata: Metadata = {
  title: "Constructor de argumentos",
};

export default function ArgumentBuilderPage() {
  // El constructor lee argumentId/workId/statement de la query, y
  // useSearchParams obliga a un límite de Suspense o el build estático falla.
  return (
    <Suspense
      fallback={<p className="p-6 text-sm text-ink-500">Abriendo el constructor…</p>}
    >
      <BuilderClient />
    </Suspense>
  );
}
