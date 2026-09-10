import { cn } from "@/lib/cn";
import { TEXTOS, colorAvatar, humanize, iniciales } from "@/lib/vocabulario";
import type {
  AuditSeverity,
  ObjectionType,
  PremiseType,
  SoundStatus,
} from "@/types/organon";

// Reexportado por comodidad: la mayoría de componentes que pintan un badge
// también necesitan traducir algún valor suelto.
export { humanize, RELATION_COLORS } from "@/lib/vocabulario";

const BASE =
  "inline-flex items-center gap-1 rounded px-1.5 py-0.5 text-[10px] font-medium uppercase tracking-wider whitespace-nowrap";

export function Badge({
  children,
  className,
  title,
}: {
  children: React.ReactNode;
  className?: string;
  title?: string;
}) {
  return (
    <span className={cn(BASE, className)} title={title}>
      {children}
    </span>
  );
}

/** El estado de la revisión es el dato que más se escanea: color fuerte y estable. */
export function SoundStatusBadge({ status }: { status: SoundStatus }) {
  const styles: Record<SoundStatus, string> = {
    SOLIDO: "bg-valid-900 text-valid-300 border border-valid-500/40",
    FALAZ: "bg-fallacy-900 text-fallacy-300 border border-fallacy-500/40",
    PENDIENTE: "bg-ink-800 text-ink-300 border border-ink-600",
  };
  return <Badge className={styles[status]}>{humanize(status)}</Badge>;
}

export function PremiseTypeBadge({ type }: { type: PremiseType }) {
  const styles: Record<PremiseType, string> = {
    AXIOMATICA: "bg-term-900 text-term-300 border border-term-500/40",
    EMPIRICA: "bg-ink-800 text-ink-300 border border-ink-600",
    DEFINICION: "bg-ink-800 text-ink-200 border border-ink-600",
    INFERENCIA_INTERMEDIA: "bg-ink-800 text-ink-300 border border-ink-600",
    CONCLUSION: "bg-accent-900 text-accent-300 border border-accent-500/40",
  };
  return <Badge className={styles[type]}>{humanize(type)}</Badge>;
}

/**
 * Las críticas que invalidan el razonamiento se pintan en rosa; las que solo lo
 * debilitan, en ámbar. Es la misma distinción que aplica la revisión.
 */
export function ObjectionTypeBadge({ type }: { type: ObjectionType }) {
  const graves: ObjectionType[] = [
    "FALACIA_FORMAL",
    "PETICION_DE_PRINCIPIO",
    "FALSA_DICOTOMIA",
  ];
  return (
    <Badge
      className={
        graves.includes(type)
          ? "bg-fallacy-900 text-fallacy-300 border border-fallacy-500/40"
          : "bg-accent-900 text-accent-300 border border-accent-500/40"
      }
    >
      {humanize(type)}
    </Badge>
  );
}

export function SeverityBadge({ severity }: { severity: AuditSeverity }) {
  const styles: Record<AuditSeverity, string> = {
    BLOQUEANTE: "bg-fallacy-900 text-fallacy-300 border border-fallacy-500/40",
    ADVERTENCIA: "bg-accent-900 text-accent-300 border border-accent-500/40",
    INFORMATIVA: "bg-ink-800 text-ink-400 border border-ink-600",
  };
  return <Badge className={styles[severity]}>{humanize(severity)}</Badge>;
}

export function EnthymemeBadge() {
  return (
    <Badge
      className="bg-accent-900 text-accent-300 border border-accent-500/40"
      title={TEXTOS.supuestoImplicitoAyuda}
    >
      {TEXTOS.supuestoImplicito}
    </Badge>
  );
}

/** Emoji del pensador, o sus iniciales sobre un color derivado del nombre. */
export function Avatar({
  name,
  emoji,
  size = "md",
}: {
  name: string;
  emoji?: string | null;
  size?: "sm" | "md" | "lg";
}) {
  const dims = {
    sm: "size-6 text-[11px]",
    md: "size-8 text-[13px]",
    lg: "size-11 text-base",
  }[size];

  if (emoji && emoji.trim()) {
    return (
      <span
        className={cn(
          "inline-flex shrink-0 items-center justify-center rounded-full border border-ink-700 bg-ink-850",
          dims,
        )}
        title={name}
      >
        {emoji}
      </span>
    );
  }

  const color = colorAvatar(name);
  return (
    <span
      className={cn(
        "inline-flex shrink-0 items-center justify-center rounded-full border font-medium",
        color.fondo,
        color.texto,
        color.borde,
        dims,
      )}
      title={name}
    >
      {iniciales(name)}
    </span>
  );
}
