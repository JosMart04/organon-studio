import { cn } from "@/lib/cn";
import type {
  AuditSeverity,
  ObjectionType,
  PremiseType,
  RelationType,
  SoundStatus,
} from "@/types/organon";

/** Convierte AXIOMATICA en "Axiomática" para mostrarlo sin gritar. */
export function humanize(value: string): string {
  const map: Record<string, string> = {
    AXIOMATICA: "Axiomática",
    EMPIRICA: "Empírica",
    DEFINICION: "Definición",
    INFERENCIA_INTERMEDIA: "Inferencia intermedia",
    CONCLUSION: "Conclusión",
    CONTRAEJEMPLO: "Contraejemplo",
    FALACIA_FORMAL: "Falacia formal",
    PETICION_DE_PRINCIPIO: "Petición de principio",
    REDEFINICION: "Redefinición",
    REGRESO_AL_INFINITO: "Regreso al infinito",
    FALSA_DICOTOMIA: "Falsa dicotomía",
    AMBIGUEDAD: "Ambigüedad",
    GENERALIZACION_APRESURADA: "Generalización apresurada",
    MODUS_PONENS: "Modus ponens",
    MODUS_TOLLENS: "Modus tollens",
    SILOGISMO_CATEGORICO: "Silogismo categórico",
    SILOGISMO_DISYUNTIVO: "Silogismo disyuntivo",
    SILOGISMO_HIPOTETICO: "Silogismo hipotético",
    REDUCTIO_AD_ABSURDUM: "Reductio ad absurdum",
    INDUCTIVE: "Inductivo",
    ABDUCTIVE: "Abductivo",
    TRASCENDENTAL: "Trascendental",
    ANALOGICO: "Analógico",
    NO_CLASIFICADO: "Sin clasificar",
    SOLIDO: "Sólido",
    FALAZ: "Falaz",
    PENDIENTE: "Pendiente",
    ANTIGUA: "Antigua",
    MEDIEVAL: "Medieval",
    MODERNA: "Moderna",
    CONTEMPORANEA: "Contemporánea",
    REFUTA: "Refuta",
    PRESUPONE: "Presupone",
    EXTIENDE: "Extiende",
    RADICALIZA: "Radicaliza",
    MATIZA: "Matiza",
    OBRA: "en esta obra",
    AUTOR: "en todo el autor",
    SIN_DEFINICION: "sin definición",
  };
  return map[value] ?? value;
}

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

/** El estado de solidez es el dato que más se escanea: color fuerte y estable. */
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
 * Las objeciones que invalidan la inferencia se pintan en rosa; las que solo
 * la debilitan, en ámbar. La distinción es la misma que aplica el auditor.
 */
export function ObjectionTypeBadge({ type }: { type: ObjectionType }) {
  const fatal: ObjectionType[] = [
    "FALACIA_FORMAL",
    "PETICION_DE_PRINCIPIO",
    "FALSA_DICOTOMIA",
  ];
  return (
    <Badge
      className={
        fatal.includes(type)
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
  return <Badge className={styles[severity]}>{severity}</Badge>;
}

export const RELATION_COLORS: Record<RelationType, string> = {
  REFUTA: "#c0556b",
  PRESUPONE: "#6d7fd4",
  EXTIENDE: "#2f9e6e",
  RADICALIZA: "#d99a2b",
  MATIZA: "#6f7d90",
};

export function EnthymemeBadge() {
  return (
    <Badge
      className="bg-accent-900 text-accent-300 border border-accent-500/40"
      title="Supuesto implícito: el autor no lo escribió, pero su inferencia lo necesita"
    >
      Entimema
    </Badge>
  );
}
