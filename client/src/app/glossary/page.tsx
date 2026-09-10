import { Suspense } from "react";
import type { Metadata } from "next";
import { GlossaryClient } from "@/components/glossary/glossary-client";

export const metadata: Metadata = {
  title: "Glosario",
};

export default function GlossaryPage() {
  return (
    <Suspense fallback={<p className="p-6 text-sm text-ink-500">Abriendo el glosario…</p>}>
      <GlossaryClient />
    </Suspense>
  );
}
