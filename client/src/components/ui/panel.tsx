import { cn } from "@/lib/cn";

export function PageHeader({
  title,
  subtitle,
  actions,
}: {
  title: string;
  subtitle?: string;
  actions?: React.ReactNode;
}) {
  return (
    <header className="flex flex-wrap items-end justify-between gap-3 border-b border-ink-800 px-6 py-4">
      <div className="min-w-0">
        <h1 className="font-serif text-xl text-ink-50">{title}</h1>
        {subtitle && (
          <p className="mt-0.5 max-w-2xl text-[13px] leading-relaxed text-ink-400">
            {subtitle}
          </p>
        )}
      </div>
      {actions && <div className="flex shrink-0 items-center gap-2">{actions}</div>}
    </header>
  );
}

export function Panel({
  title,
  children,
  className,
  actions,
}: {
  title?: string;
  children: React.ReactNode;
  className?: string;
  actions?: React.ReactNode;
}) {
  return (
    <section
      className={cn(
        "rounded-lg border border-ink-800 bg-ink-900/60 overflow-hidden",
        className,
      )}
    >
      {title && (
        <div className="flex items-center justify-between gap-2 border-b border-ink-800 bg-ink-850/60 px-4 py-2.5">
          <h2 className="text-[11px] font-semibold uppercase tracking-[0.12em] text-ink-400">
            {title}
          </h2>
          {actions}
        </div>
      )}
      <div className="p-4">{children}</div>
    </section>
  );
}

export function EmptyState({
  title,
  hint,
}: {
  title: string;
  hint?: string;
}) {
  return (
    <div className="rounded-md border border-dashed border-ink-700 px-4 py-8 text-center">
      <p className="text-[13px] text-ink-300">{title}</p>
      {hint && <p className="mt-1 text-xs text-ink-500">{hint}</p>}
    </div>
  );
}

export function ErrorState({ message }: { message: string }) {
  return (
    <div className="rounded-md border border-fallacy-500/40 bg-fallacy-900/30 px-4 py-3">
      <p className="text-[13px] text-fallacy-300">{message}</p>
      <p className="mt-1 text-xs text-ink-500">
        Comprueba que el backend esté corriendo en{" "}
        <code className="font-mono">http://localhost:8080</code>.
      </p>
    </div>
  );
}

export function Button({
  children,
  onClick,
  variant = "default",
  size = "md",
  disabled,
  type = "button",
  className,
  title,
}: {
  children: React.ReactNode;
  onClick?: () => void;
  variant?: "default" | "primary" | "ghost" | "danger";
  size?: "sm" | "md";
  disabled?: boolean;
  type?: "button" | "submit";
  className?: string;
  title?: string;
}) {
  const variants = {
    default:
      "border border-ink-700 bg-ink-800 text-ink-200 hover:bg-ink-700 hover:text-ink-50",
    primary:
      "border border-accent-500/50 bg-accent-900 text-accent-300 hover:bg-accent-500/20",
    ghost: "border border-transparent text-ink-400 hover:bg-ink-800 hover:text-ink-100",
    danger:
      "border border-fallacy-500/40 bg-fallacy-900/60 text-fallacy-300 hover:bg-fallacy-900",
  };
  const sizes = {
    sm: "px-2 py-1 text-[11px]",
    md: "px-3 py-1.5 text-xs",
  };
  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled}
      title={title}
      className={cn(
        "inline-flex items-center gap-1.5 rounded font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-40",
        variants[variant],
        sizes[size],
        className,
      )}
    >
      {children}
    </button>
  );
}
