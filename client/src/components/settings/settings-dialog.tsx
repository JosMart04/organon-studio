"use client";

import { useRef, useState } from "react";
import { CircleCheck, Download, FileUp, Loader2, TriangleAlert } from "lucide-react";
import { ApiError, backup } from "@/lib/api";
import { useRefrescarCorpus } from "@/lib/corpus-refresh";
import { TIPOS_CUADERNO } from "@/lib/vocabulario";
import { cn } from "@/lib/cn";
import type { BackupDocument, ImportMode, ImportReport } from "@/types/organon";
import { Button } from "@/components/ui/panel";
import { Dialog } from "@/components/ui/dialog";

interface FicheroLeido {
  nombre: string;
  /** Tal cual se leyó: se envía sin volver a serializar. */
  texto: string;
  copia: BackupDocument;
}

interface Fallo {
  mensaje: string;
  problemas: string[];
}

const ENLACE =
  "inline-flex items-center gap-1.5 rounded border px-3 py-1.5 text-xs font-medium transition-colors";

function fechaLegible(iso: string | undefined): string {
  if (!iso) return "fecha desconocida";
  const fecha = new Date(iso);
  return Number.isNaN(fecha.getTime())
    ? "fecha desconocida"
    : fecha.toLocaleString("es-ES", { dateStyle: "long", timeStyle: "short" });
}

/**
 * Sacar las notas a un fichero y volver a meterlas.
 *
 * Nada se restaura sin enseñar antes qué contiene la copia, y sobrescribir
 * pide dos confirmaciones: tras el commit en el servidor no hay vuelta atrás.
 */
export function SettingsDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const refrescar = useRefrescarCorpus();
  const selector = useRef<HTMLInputElement>(null);

  const [fichero, setFichero] = useState<FicheroLeido | null>(null);
  const [errorLectura, setErrorLectura] = useState<string>();
  const [modo, setModo] = useState<ImportMode>("MERGE");
  const [aceptoBorrar, setAceptoBorrar] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [informe, setInforme] = useState<ImportReport>();
  const [fallo, setFallo] = useState<Fallo>();

  const reiniciar = () => {
    setFichero(null);
    setErrorLectura(undefined);
    setModo("MERGE");
    setAceptoBorrar(false);
    setInforme(undefined);
    setFallo(undefined);
    if (selector.current) selector.current.value = "";
  };

  const cerrar = () => {
    // Mientras se restaura no se deja cerrar: parecería que se ha cancelado.
    if (enviando) return;
    reiniciar();
    onClose();
  };

  const leer = async (file: File | undefined) => {
    if (!file) return;
    reiniciar();
    const texto = await file.text();

    let contenido: unknown;
    try {
      contenido = JSON.parse(texto);
    } catch {
      setErrorLectura("Este fichero no es JSON válido. ¿Seguro que es una copia de Organon?");
      return;
    }

    const copia = contenido as Partial<BackupDocument> | null;
    if (!copia || copia.format !== "organon-backup" || typeof copia.data !== "object" || !copia.data) {
      setErrorLectura("Este fichero no es una copia de seguridad de Organon Studio.");
      return;
    }
    setFichero({ nombre: file.name, texto, copia: copia as BackupDocument });
  };

  const restaurar = async () => {
    if (!fichero) return;
    setEnviando(true);
    setFallo(undefined);
    try {
      setInforme(await backup.restore(modo, fichero.texto));
      refrescar();
    } catch (e) {
      const detalle = e instanceof ApiError ? (e.detail as { problems?: unknown } | undefined) : undefined;
      const problemas = detalle && Array.isArray(detalle.problems) ? detalle.problems.map(String) : [];
      setFallo({ mensaje: e instanceof Error ? e.message : String(e), problemas });
    } finally {
      setEnviando(false);
    }
  };

  const elegirModo = (valor: ImportMode) => {
    setModo(valor);
    // Volver a elegir sobrescribir exige volver a confirmarlo.
    setAceptoBorrar(false);
  };

  const puedeRestaurar =
    !!fichero && !informe && !enviando && (modo === "MERGE" || aceptoBorrar);

  const resumen = fichero
    ? TIPOS_CUADERNO.map(([clave, etiqueta]) => {
        const lista = fichero.copia.data[clave];
        return { clave, etiqueta, total: Array.isArray(lista) ? lista.length : 0 };
      })
    : [];

  return (
    <Dialog
      open={open}
      onClose={cerrar}
      title="Ajustes"
      description="Tus notas viven en tu ordenador. Desde aquí puedes sacarlas a un fichero y volver a meterlas."
      wide
      footer={
        informe ? (
          <Button variant="primary" onClick={cerrar}>
            Listo
          </Button>
        ) : (
          <>
            <Button variant="ghost" onClick={cerrar} disabled={enviando}>
              Cerrar
            </Button>
            {fichero && (
              <Button
                variant={modo === "REPLACE" ? "danger" : "primary"}
                onClick={restaurar}
                disabled={!puedeRestaurar}
              >
                {enviando && <Loader2 className="size-3.5 animate-spin" />}
                {modo === "REPLACE" ? "Sobrescribir todo" : "Fusionar con mis notas"}
              </Button>
            )}
          </>
        )
      }
    >
      <div className="space-y-5">
        <section>
          <h3 className="font-serif text-[15px] text-ink-50">Copia de seguridad</h3>
          <p className="mt-1 text-[12.5px] leading-relaxed text-ink-400">
            Un único fichero con todo tu cuaderno: pensadores, libros, fragmentos, ideas con sus
            razones y críticas, palabras y las conexiones del debate. Guárdalo donde quieras.
          </p>
          <a
            href={backup.exportUrl()}
            className={cn(
              ENLACE,
              "mt-3 border-accent-500/50 bg-accent-900 text-accent-300 hover:bg-accent-500/20",
            )}
          >
            <Download className="size-3.5" />
            Descargar copia de seguridad (.json)
          </a>
        </section>

        <section className="border-t border-ink-800 pt-5">
          <h3 className="font-serif text-[15px] text-ink-50">Restaurar notas desde una copia</h3>
          <p className="mt-1 text-[12.5px] leading-relaxed text-ink-400">
            Elige un fichero que hayas descargado antes. Verás qué contiene antes de confirmar nada.
          </p>

          <input
            ref={selector}
            type="file"
            accept=".json,application/json"
            hidden
            onChange={(e) => leer(e.target.files?.[0])}
          />
          {!informe && (
            <Button
              className="mt-3"
              onClick={() => selector.current?.click()}
              disabled={enviando}
            >
              <FileUp className="size-3.5" />
              {fichero ? "Elegir otro fichero…" : "Elegir fichero…"}
            </Button>
          )}

          {errorLectura && (
            <p className="mt-3 rounded border border-fallacy-500/40 bg-fallacy-900/30 px-3 py-2 text-[12px] text-fallacy-300">
              {errorLectura}
            </p>
          )}

          {fichero && !informe && (
            <div className="mt-4 space-y-4">
              <div className="rounded-md border border-ink-800 bg-ink-950 p-3">
                <p className="truncate font-mono text-[11px] text-ink-400">{fichero.nombre}</p>
                <p className="mt-0.5 text-[12px] text-ink-300">
                  Hecha el {fechaLegible(fichero.copia.exportedAt)}
                </p>
                <dl className="mt-2.5 grid grid-cols-2 gap-x-4 gap-y-1 sm:grid-cols-3">
                  {resumen.map(({ clave, etiqueta, total }) => (
                    <div key={clave} className="flex items-baseline justify-between gap-2 text-[12px]">
                      <dt className="text-ink-500">{etiqueta}</dt>
                      <dd className="font-mono text-ink-100">{total}</dd>
                    </div>
                  ))}
                </dl>
              </div>

              <fieldset className="grid gap-2 sm:grid-cols-2">
                <legend className="mb-1.5 text-[11px] font-medium text-ink-300">
                  ¿Qué hago con lo que ya tienes?
                </legend>
                <OpcionModo
                  valor="MERGE"
                  actual={modo}
                  onChange={elegirModo}
                  titulo="Fusionar"
                  descripcion="Añade lo que falta y no toca lo que ya tienes. Lo que coincida, se omite."
                />
                <OpcionModo
                  valor="REPLACE"
                  actual={modo}
                  onChange={elegirModo}
                  titulo="Sobrescribir todo"
                  descripcion="Vacía el cuaderno y lo deja exactamente como está en la copia."
                  peligro
                />
              </fieldset>

              {modo === "REPLACE" && (
                <div className="rounded-md border border-fallacy-500/40 bg-fallacy-900/25 p-3">
                  <p className="flex items-start gap-2 text-[12.5px] leading-relaxed text-fallacy-300">
                    <TriangleAlert className="mt-0.5 size-4 shrink-0" />
                    Se borrará todo lo que tienes ahora y se sustituirá por el contenido de la copia.
                    No se puede deshacer.
                  </p>
                  <a
                    href={backup.exportUrl()}
                    className={cn(
                      ENLACE,
                      "mt-2.5 border-ink-700 bg-ink-800 text-ink-200 hover:bg-ink-700",
                    )}
                  >
                    <Download className="size-3.5" />
                    Descargar primero lo que tengo ahora
                  </a>
                  <label className="mt-3 flex cursor-pointer items-start gap-2 text-[12px] text-ink-200">
                    <input
                      type="checkbox"
                      checked={aceptoBorrar}
                      onChange={(e) => setAceptoBorrar(e.target.checked)}
                      className="mt-0.5 accent-fallacy-500"
                    />
                    Entiendo que se borrarán mis notas actuales.
                  </label>
                </div>
              )}
            </div>
          )}

          {fallo && (
            <div className="mt-4 rounded-md border border-fallacy-500/40 bg-fallacy-900/30 p-3">
              <p className="text-[12.5px] text-fallacy-300">{fallo.mensaje}</p>
              {fallo.problemas.length > 0 && (
                <ul className="mt-2 list-disc space-y-0.5 pl-5 text-[12px] text-ink-200">
                  {fallo.problemas.map((problema, i) => (
                    <li key={i}>{problema}</li>
                  ))}
                </ul>
              )}
              <p className="mt-2 text-[11px] text-ink-500">No se ha cambiado nada en tu cuaderno.</p>
            </div>
          )}

          {informe && <Informe informe={informe} />}
        </section>
      </div>
    </Dialog>
  );
}

function OpcionModo({
  valor,
  actual,
  onChange,
  titulo,
  descripcion,
  peligro,
}: {
  valor: ImportMode;
  actual: ImportMode;
  onChange: (valor: ImportMode) => void;
  titulo: string;
  descripcion: string;
  peligro?: boolean;
}) {
  const activo = valor === actual;
  return (
    <label
      className={cn(
        "flex cursor-pointer gap-2.5 rounded-md border p-3 transition-colors",
        activo
          ? peligro
            ? "border-fallacy-500/60 bg-fallacy-900/30"
            : "border-accent-500/60 bg-accent-900/30"
          : "border-ink-800 bg-ink-950 hover:border-ink-600",
      )}
    >
      <input
        type="radio"
        name="modo-restauracion"
        value={valor}
        checked={activo}
        onChange={() => onChange(valor)}
        className={cn("mt-0.5", peligro ? "accent-fallacy-500" : "accent-accent-500")}
      />
      <span>
        <span className="block text-[12.5px] font-medium text-ink-100">{titulo}</span>
        <span className="mt-0.5 block text-[11.5px] leading-snug text-ink-400">{descripcion}</span>
      </span>
    </label>
  );
}

function Informe({ informe }: { informe: ImportReport }) {
  return (
    <div className="mt-4 rounded-md border border-valid-500/40 bg-valid-900/25 p-3">
      <p className="flex items-center gap-2 text-[13px] text-valid-300">
        <CircleCheck className="size-4 shrink-0" />
        {informe.mode === "REPLACE"
          ? "Tu cuaderno se ha sustituido por la copia."
          : "La copia se ha fusionado con tus notas."}
      </p>
      <table className="mt-3 w-full text-[12px]">
        <thead>
          <tr className="text-left text-[10px] uppercase tracking-wider text-ink-500">
            <th className="pb-1 font-medium" />
            <th className="pb-1 text-right font-medium">Añadidos</th>
            <th className="pb-1 text-right font-medium">Ya estaban</th>
          </tr>
        </thead>
        <tbody>
          {TIPOS_CUADERNO.map(([clave, etiqueta]) => (
            <tr key={clave} className="border-t border-ink-800/60">
              <td className="py-1 text-ink-300">{etiqueta}</td>
              <td className="py-1 text-right font-mono text-ink-100">{informe.created[clave] ?? 0}</td>
              <td className="py-1 text-right font-mono text-ink-500">{informe.skipped[clave] ?? 0}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {informe.warnings.length > 0 && (
        <ul className="mt-3 space-y-1 text-[11.5px] text-accent-300">
          {informe.warnings.map((aviso, i) => (
            <li key={i}>· {aviso}</li>
          ))}
        </ul>
      )}
    </div>
  );
}
