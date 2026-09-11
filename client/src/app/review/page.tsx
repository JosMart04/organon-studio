import { Suspense } from "react";
import type { Metadata } from "next";
import { ReviewClient } from "@/components/review/review-client";

export const metadata: Metadata = {
  title: "Repasar",
};

export default function ReviewPage() {
  // useSearchParams (se llega con ?argumentId= desde el constructor o el mapa)
  // exige un límite de Suspense en una ruta prerenderizada.
  return (
    <Suspense fallback={<p className="p-6 text-sm text-ink-500">Cargando…</p>}>
      <ReviewClient />
    </Suspense>
  );
}
