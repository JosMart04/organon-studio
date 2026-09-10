import Link from "next/link";
import {
  BookOpenText,
  Library,
  Network,
  SquareSigma,
  type LucideIcon,
} from "lucide-react";

const MODULES: {
  href: string;
  icon: LucideIcon;
  title: string;
  body: string;
}[] = [
  {
    href: "/reader",
    icon: BookOpenText,
    title: "Lector de doble panel",
    body: "La fuente primaria a la izquierda, el aparato crítico a la derecha. Glosario del autor, argumentos del fragmento y objeciones, sincronizados con el pasaje que tienes delante.",
  },
  {
    href: "/arguments/builder",
    icon: SquareSigma,
    title: "Constructor de argumentos",
    body: "Reconstrucción en forma estándar. Ordena premisas arrastrándolas, marca los entimemas que el autor dio por supuestos y formaliza la inferencia en LaTeX.",
  },
  {
    href: "/glossary",
    icon: Library,
    title: "Sobrecarga semántica",
    body: "«Sustancia» no significa lo mismo en Descartes que en Spinoza. Cada definición queda delimitada por autor y obra, y el comparador las pone en columnas paralelas.",
  },
  {
    href: "/graph",
    icon: Network,
    title: "Grafo dialéctico",
    body: "Quién refuta, presupone, extiende o radicaliza a quién. El debate como red navegable en vez de como bibliografía.",
  },
];

export default function Home() {
  return (
    <div className="mx-auto max-w-4xl px-6 py-14">
      <p className="font-mono text-[11px] uppercase tracking-[0.18em] text-accent-400">
        Entorno Integrado de Lectura y Análisis Crítico
      </p>
      <h1 className="mt-3 font-serif text-4xl leading-tight text-ink-50">
        Organon Studio
      </h1>
      <p className="prose-source mt-4 max-w-2xl text-ink-300">
        Un banco de trabajo para la operación central del oficio filosófico:
        tomar un texto fuente, extraer su argumento, reconstruirlo en forma
        estándar, someterlo a estrés y situarlo en la red de debate a la que
        pertenece.
      </p>

      <div className="mt-10 grid gap-3 sm:grid-cols-2">
        {MODULES.map(({ href, icon: Icon, title, body }) => (
          <Link
            key={href}
            href={href}
            className="group rounded-lg border border-ink-800 bg-ink-900/60 p-5 transition-colors hover:border-ink-600 hover:bg-ink-850"
          >
            <Icon
              className="size-5 text-accent-400 transition-transform group-hover:scale-110"
              strokeWidth={1.75}
            />
            <h2 className="mt-3 font-serif text-lg text-ink-100">{title}</h2>
            <p className="mt-1.5 text-[13px] leading-relaxed text-ink-400">{body}</p>
          </Link>
        ))}
      </div>

      <div className="mt-10 rounded-lg border border-ink-800 bg-ink-900/40 p-5">
        <h2 className="text-[11px] font-semibold uppercase tracking-[0.12em] text-ink-400">
          Corpus cargado
        </h2>
        <p className="mt-2 text-[13px] leading-relaxed text-ink-300">
          El debate moderno sobre causalidad y sustancia:{" "}
          <span className="text-ink-100">Descartes</span> deduce a Dios de la
          idea de infinito, <span className="text-ink-100">Spinoza</span>{" "}
          radicaliza su definición de sustancia hasta el monismo,{" "}
          <span className="text-ink-100">Hume</span> demuele la conexión
          necesaria y <span className="text-ink-100">Kant</span> responde
          convirtiendo la causalidad en condición de posibilidad de la
          experiencia.
        </p>
      </div>
    </div>
  );
}
