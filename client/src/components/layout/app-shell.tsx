"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  BookOpenText,
  GitBranchPlus,
  Library,
  Network,
  SquareSigma,
} from "lucide-react";
import { cn } from "@/lib/cn";

const NAV = [
  {
    href: "/reader",
    label: "Lector",
    hint: "Fuente primaria y aparato crítico",
    icon: BookOpenText,
  },
  {
    href: "/arguments/builder",
    label: "Constructor",
    hint: "Reconstrucción en forma estándar",
    icon: SquareSigma,
  },
  {
    href: "/glossary",
    label: "Glosario",
    hint: "Sobrecarga semántica por autor",
    icon: Library,
  },
  {
    href: "/graph",
    label: "Grafo",
    hint: "Red dialéctica del debate",
    icon: Network,
  },
] as const;

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();

  return (
    <div className="flex min-h-screen">
      <aside className="hidden w-60 shrink-0 flex-col border-r border-ink-800 bg-ink-900 md:flex">
        <Link
          href="/"
          className="flex items-center gap-2.5 border-b border-ink-800 px-5 py-4 transition-colors hover:bg-ink-850"
        >
          <GitBranchPlus className="size-5 text-accent-400" strokeWidth={1.75} />
          <div className="leading-tight">
            <div className="font-serif text-[15px] font-semibold text-ink-50">
              Organon
            </div>
            <div className="text-[10px] uppercase tracking-[0.14em] text-ink-400">
              Studio
            </div>
          </div>
        </Link>

        <nav className="flex flex-1 flex-col gap-0.5 p-2.5">
          {NAV.map(({ href, label, hint, icon: Icon }) => {
            const active = pathname === href || pathname.startsWith(`${href}/`);
            return (
              <Link
                key={href}
                href={href}
                className={cn(
                  "group flex items-start gap-2.5 rounded-md px-3 py-2.5 transition-colors",
                  active
                    ? "bg-ink-800 text-ink-50"
                    : "text-ink-300 hover:bg-ink-850 hover:text-ink-100",
                )}
              >
                <Icon
                  className={cn(
                    "mt-0.5 size-4 shrink-0",
                    active ? "text-accent-400" : "text-ink-500 group-hover:text-ink-300",
                  )}
                  strokeWidth={1.75}
                />
                <span className="min-w-0">
                  <span className="block text-[13px] font-medium">{label}</span>
                  <span className="block text-[11px] leading-snug text-ink-500">
                    {hint}
                  </span>
                </span>
              </Link>
            );
          })}
        </nav>

        <p className="border-t border-ink-800 px-5 py-3 text-[10px] leading-relaxed text-ink-600">
          Entorno Integrado de Lectura y Análisis Crítico
        </p>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        {/* Navegación compacta para viewport estrecho. */}
        <nav className="flex items-center gap-1 overflow-x-auto border-b border-ink-800 bg-ink-900 px-3 py-2 md:hidden">
          <Link href="/" className="mr-2 shrink-0 font-serif text-sm font-semibold text-ink-50">
            Organon
          </Link>
          {NAV.map(({ href, label }) => {
            const active = pathname === href || pathname.startsWith(`${href}/`);
            return (
              <Link
                key={href}
                href={href}
                className={cn(
                  "shrink-0 rounded px-2.5 py-1 text-xs transition-colors",
                  active ? "bg-ink-800 text-ink-50" : "text-ink-400 hover:text-ink-200",
                )}
              >
                {label}
              </Link>
            );
          })}
        </nav>

        <main className="min-w-0 flex-1">{children}</main>
      </div>
    </div>
  );
}
