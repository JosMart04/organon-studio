"use client";

import { useMemo } from "react";
import katex from "katex";
import { cn } from "@/lib/cn";

interface LatexProps {
  expression: string;
  display?: boolean;
  className?: string;
}

/**
 * Renderiza una expresión con KaTeX. Un error de sintaxis no rompe la página:
 * se muestra la fuente en crudo y se marca en rojo, que es exactamente lo que
 * el investigador necesita ver mientras teclea una formalización a medias.
 */
export function Latex({ expression, display = false, className }: LatexProps) {
  const result = useMemo(() => {
    if (!expression?.trim()) {
      return { html: "", failed: false };
    }
    try {
      return {
        html: katex.renderToString(expression, {
          displayMode: display,
          throwOnError: true,
          strict: false,
          trust: false,
        }),
        failed: false,
      };
    } catch {
      return { html: "", failed: true };
    }
  }, [expression, display]);

  if (!expression?.trim()) {
    return null;
  }

  if (result.failed) {
    return (
      <code
        className={cn(
          "block rounded border border-fallacy-500/40 bg-fallacy-900/40 px-2.5 py-1.5 font-mono text-xs text-fallacy-300",
          className,
        )}
        title="La expresión no es LaTeX válido todavía"
      >
        {expression}
      </code>
    );
  }

  return (
    <span
      className={cn(display && "block", className)}
      dangerouslySetInnerHTML={{ __html: result.html }}
    />
  );
}
