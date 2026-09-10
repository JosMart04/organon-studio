import Link from "next/link";
import {
  BookOpenText,
  Bot,
  Library,
  Network,
  SquareSigma,
  type LucideIcon,
} from "lucide-react";

const MODULOS: {
  href: string;
  icon: LucideIcon;
  title: string;
  body: string;
}[] = [
  {
    href: "/reader",
    icon: BookOpenText,
    title: "Leer y anotar",
    body: "El fragmento a la izquierda, lo que vas descubriendo a la derecha. Selecciona una frase y conviértela en una nota, o pregunta qué entiende ese autor por una palabra concreta.",
  },
  {
    href: "/arguments/builder",
    icon: SquareSigma,
    title: "Desmontar una idea",
    body: "¿Qué sostiene el autor, con qué razones, y qué está dando por obvio sin decirlo? Ordena las razones arrastrándolas y anota por qué podría estar equivocado.",
  },
  {
    href: "/glossary",
    icon: Library,
    title: "La misma palabra, otro significado",
    body: "«Sustancia» no quiere decir lo mismo en Descartes que en Spinoza. Muchas discusiones filosóficas son esto y nada más. Aquí se ve en columnas, de un vistazo.",
  },
  {
    href: "/graph",
    icon: Network,
    title: "Ver el debate entero",
    body: "Quién refuta a quién, quién se apoya en quién. El mapa se dibuja solo con lo que vas anotando.",
  },
];

export default function Home() {
  return (
    <div className="mx-auto max-w-4xl px-6 py-14">
      <p className="font-mono text-[11px] uppercase tracking-[0.18em] text-accent-400">
        Tu cuaderno de lectura filosófica
      </p>
      <h1 className="mt-3 font-serif text-4xl leading-tight text-ink-50">Organon Studio</h1>
      <p className="prose-source mt-4 max-w-2xl text-ink-300">
        Estás leyendo un libro de filosofía y quieres enterarte de verdad: qué
        sostiene el autor, en qué se apoya, dónde flaquea y con quién está
        discutiendo sin decirlo. Esto es para eso. No hace falta saber lógica
        formal.
      </p>

      <div className="mt-10 grid gap-3 sm:grid-cols-2">
        {MODULOS.map(({ href, icon: Icon, title, body }) => (
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
        <h2 className="flex items-center gap-2 text-[11px] font-semibold uppercase tracking-[0.12em] text-ink-400">
          <Bot className="size-3.5 text-accent-400" />
          Asistente en tu propio ordenador
        </h2>
        <p className="mt-2 text-[13px] leading-relaxed text-ink-300">
          Si tienes <span className="font-mono text-ink-100">Ollama</span> instalado,
          el botón <span className="text-accent-300">Asistente</span> te explica un
          pasaje denso en cristiano, propone cómo desmontar su argumento y sugiere
          quién le llevaría la contraria. Todo funciona sin conexión y nada sale de
          tu equipo. Y si no lo tienes encendido, la aplicación te lo dice y sigues
          tomando notas a mano.
        </p>
      </div>

      <div className="mt-4 rounded-lg border border-dashed border-ink-800 p-5">
        <h2 className="text-[11px] font-semibold uppercase tracking-[0.12em] text-ink-400">
          Empieza por aquí
        </h2>
        <p className="mt-2 text-[13px] leading-relaxed text-ink-400">
          El cuaderno arranca vacío a propósito. Usa{" "}
          <span className="text-ink-200">Añadir</span> en el menú de la izquierda:
          primero un pensador, luego un libro suyo, y después la primera idea que te
          llame la atención mientras lees.
        </p>
      </div>
    </div>
  );
}
