"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  BookOpenText,
  GitBranchPlus,
  Library,
  Network,
  NotebookPen,
  Plus,
  Search,
  Settings,
  SquareSigma,
  User,
} from "lucide-react";
import { cn } from "@/lib/cn";
import {
  PassageDialog,
  PhilosopherDialog,
  WorkDialog,
} from "@/components/create/create-dialogs";
import { SettingsDialog } from "@/components/settings/settings-dialog";
import { CommandPalette } from "@/components/search/command-palette";
import { CorpusRefreshProvider, useRefrescarCorpus } from "@/lib/corpus-refresh";

const NAV = [
  {
    href: "/reader",
    label: "Leer",
    hint: "El texto y tus notas, lado a lado",
    icon: BookOpenText,
  },
  {
    href: "/arguments/builder",
    label: "Desmontar",
    hint: "Las razones de una idea",
    icon: SquareSigma,
  },
  {
    href: "/glossary",
    label: "Palabras",
    hint: "La misma palabra, sentidos distintos",
    icon: Library,
  },
  {
    href: "/graph",
    label: "Debate",
    hint: "Quién discute con quién",
    icon: Network,
  },
] as const;

type Alta = "pensador" | "libro" | "idea" | null;

const ALTAS = [
  { id: "pensador" as const, label: "Pensador", icon: User },
  { id: "libro" as const, label: "Libro", icon: BookOpenText },
  { id: "idea" as const, label: "Idea", icon: NotebookPen },
];

export function AppShell({ children }: { children: React.ReactNode }) {
  return (
    <CorpusRefreshProvider>
      <Shell>{children}</Shell>
    </CorpusRefreshProvider>
  );
}

function Shell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const [alta, setAlta] = useState<Alta>(null);
  const [ajustesAbiertos, setAjustesAbiertos] = useState(false);
  const [buscando, setBuscando] = useState(false);

  // Tras crear algo, las vistas abiertas recargan sus datos.
  const refrescar = useRefrescarCorpus();

  // Ctrl+K (⌘K en Mac) abre la búsqueda desde cualquier pantalla. Los navegadores
  // dejan que la página se quede con el atajo si lo reclama con preventDefault.
  useEffect(() => {
    const alPulsar = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k") {
        e.preventDefault();
        setBuscando((abierta) => !abierta);
      }
    };
    window.addEventListener("keydown", alPulsar);
    return () => window.removeEventListener("keydown", alPulsar);
  }, []);

  return (
    <div className="flex min-h-screen">
      <aside className="hidden w-60 shrink-0 flex-col border-r border-ink-800 bg-ink-900 md:flex">
        <Link
          href="/"
          className="flex items-center gap-2.5 border-b border-ink-800 px-5 py-4 transition-colors hover:bg-ink-850"
        >
          <GitBranchPlus className="size-5 text-accent-400" strokeWidth={1.75} />
          <div className="leading-tight">
            <div className="font-serif text-[15px] font-semibold text-ink-50">Organon</div>
            <div className="text-[10px] uppercase tracking-[0.14em] text-ink-400">
              Studio
            </div>
          </div>
        </Link>

        <div className="border-b border-ink-800 p-2.5">
          <button
            type="button"
            onClick={() => setBuscando(true)}
            title="Buscar en tu cuaderno (Ctrl+K, ⌘K en Mac)"
            className="flex w-full items-center gap-2 rounded-md border border-ink-800 bg-ink-950 px-2.5 py-1.5 text-[12px] text-ink-500 transition-colors hover:border-ink-700 hover:text-ink-300"
          >
            <Search className="size-3.5" strokeWidth={1.75} />
            Buscar…
            <kbd className="ml-auto rounded border border-ink-700 px-1 font-sans text-[10px] text-ink-500">
              Ctrl K
            </kbd>
          </button>
        </div>

        {/* Añadir al cuaderno está disponible desde cualquier pantalla: lo que
            se descubre leyendo se apunta en el momento o se pierde. */}
        <div className="border-b border-ink-800 p-2.5">
          <p className="mb-1.5 px-1 text-[10px] uppercase tracking-[0.12em] text-ink-500">
            Añadir
          </p>
          <div className="grid grid-cols-3 gap-1">
            {ALTAS.map(({ id, label, icon: Icon }) => (
              <button
                key={id}
                type="button"
                onClick={() => setAlta(id)}
                className="flex flex-col items-center gap-1 rounded-md border border-ink-800 bg-ink-850/60 px-1 py-2 text-[10px] text-ink-300 transition-colors hover:border-accent-500/50 hover:bg-accent-900/40 hover:text-accent-300"
              >
                <span className="relative">
                  <Icon className="size-4" strokeWidth={1.75} />
                  <Plus className="absolute -right-1.5 -top-1 size-2.5" strokeWidth={3} />
                </span>
                {label}
              </button>
            ))}
          </div>
        </div>

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
                  <span className="block text-[11px] leading-snug text-ink-500">{hint}</span>
                </span>
              </Link>
            );
          })}
        </nav>

        <div className="flex items-center justify-between gap-2 border-t border-ink-800 py-2 pl-5 pr-2.5">
          <p className="text-[10px] leading-relaxed text-ink-600">
            Tu cuaderno de lectura filosófica
          </p>
          <button
            type="button"
            onClick={() => setAjustesAbiertos(true)}
            aria-label="Ajustes"
            title="Ajustes: copia de seguridad y restauración"
            className="shrink-0 rounded p-1.5 text-ink-500 transition-colors hover:bg-ink-800 hover:text-ink-200"
          >
            <Settings className="size-4" strokeWidth={1.75} />
          </button>
        </div>
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
          <button
            type="button"
            onClick={() => setBuscando(true)}
            aria-label="Buscar"
            className="ml-auto shrink-0 rounded border border-ink-700 px-2 py-1 text-xs text-ink-300"
          >
            <Search className="size-3.5" />
          </button>
          <button
            type="button"
            onClick={() => setAjustesAbiertos(true)}
            aria-label="Ajustes"
            className="shrink-0 rounded border border-ink-700 px-2 py-1 text-xs text-ink-300"
          >
            <Settings className="size-3.5" />
          </button>
          <button
            type="button"
            onClick={() => setAlta("idea")}
            aria-label="Añadir idea"
            className="shrink-0 rounded border border-ink-700 px-2 py-1 text-xs text-ink-300"
          >
            <Plus className="size-3.5" />
          </button>
        </nav>

        <main className="min-w-0 flex-1">{children}</main>
      </div>

      <PhilosopherDialog
        open={alta === "pensador"}
        onClose={() => setAlta(null)}
        onCreated={refrescar}
      />
      <WorkDialog open={alta === "libro"} onClose={() => setAlta(null)} onCreated={refrescar} />
      <PassageDialog open={alta === "idea"} onClose={() => setAlta(null)} onCreated={refrescar} />
      <SettingsDialog open={ajustesAbiertos} onClose={() => setAjustesAbiertos(false)} />
      {/* Montada solo mientras está abierta: cada búsqueda empieza en blanco. */}
      {buscando && <CommandPalette onClose={() => setBuscando(false)} />}
    </div>
  );
}
