"use client";

import { useState } from "react";
import { Loader2 } from "lucide-react";
import { corpus } from "@/lib/api";
import { useAsync } from "@/lib/use-async";
import { EPOCHS, type Epoch } from "@/types/organon";
import { humanize } from "@/lib/vocabulario";
import { Button } from "@/components/ui/panel";
import { Avatar } from "@/components/ui/badges";
import { CAMPO, CAMPO_SERIF, Dialog, Field } from "@/components/ui/dialog";

/**
 * Los tres formularios de alta rápida.
 *
 * El cuaderno se llena leyendo, no configurando: cada uno pide lo mínimo para
 * que la entrada sea útil y deja el resto para después. Solo el nombre y el
 * título son obligatorios.
 */

function useEnvio(onDone: () => void) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string>();

  const enviar = async (accion: () => Promise<unknown>) => {
    setBusy(true);
    setError(undefined);
    try {
      await accion();
      onDone();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  };

  return { busy, error, enviar };
}

// ---------------------------------------------------------------------------
// Pensador
// ---------------------------------------------------------------------------

export function PhilosopherDialog({
  open,
  onClose,
  onCreated,
}: {
  open: boolean;
  onClose: () => void;
  onCreated: () => void;
}) {
  const [name, setName] = useState("");
  const [epoch, setEpoch] = useState<Epoch>("MODERNA");
  const [emoji, setEmoji] = useState("");
  const [summary, setSummary] = useState("");
  const { busy, error, enviar } = useEnvio(() => {
    setName("");
    setEmoji("");
    setSummary("");
    onCreated();
    onClose();
  });

  const puedeGuardar = name.trim().length > 0 && !busy;

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title="Nuevo pensador"
      description="Alguien a quien vas a leer o citar."
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancelar
          </Button>
          <Button
            variant="primary"
            disabled={!puedeGuardar}
            onClick={() =>
              enviar(() =>
                corpus.createPhilosopher({
                  name: name.trim(),
                  epoch,
                  school: null,
                  biographicalSummary: summary.trim() || null,
                  avatarEmoji: emoji.trim() || null,
                }),
              )
            }
          >
            {busy && <Loader2 className="size-3.5 animate-spin" />}
            Guardar
          </Button>
        </>
      }
    >
      <div className="space-y-3.5">
        <div className="flex items-end gap-3">
          <Field label="Nombre" className="flex-1">
            <input
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Immanuel Kant"
              className={CAMPO}
              autoFocus
            />
          </Field>
          <Field label="Icono" hint="Opcional">
            <input
              value={emoji}
              onChange={(e) => setEmoji(e.target.value)}
              placeholder="🧭"
              maxLength={4}
              className={`${CAMPO} w-16 text-center text-lg`}
            />
          </Field>
          <div className="pb-[1.35rem]">
            <Avatar name={name || "?"} emoji={emoji} size="lg" />
          </div>
        </div>

        <Field label="Época">
          <select
            value={epoch}
            onChange={(e) => setEpoch(e.target.value as Epoch)}
            className={CAMPO}
          >
            {EPOCHS.map((e) => (
              <option key={e} value={e}>
                {humanize(e)}
              </option>
            ))}
          </select>
        </Field>

        <Field
          label="¿Quién fue, en una línea?"
          hint="Como se lo contarías a un amigo. Opcional."
        >
          <textarea
            value={summary}
            onChange={(e) => setSummary(e.target.value)}
            rows={3}
            placeholder="Quiso averiguar hasta dónde puede llegar la razón por sí sola."
            className={`${CAMPO_SERIF} resize-y`}
          />
        </Field>

        {error && <p className="text-[12px] text-fallacy-300">{error}</p>}
      </div>
    </Dialog>
  );
}

// ---------------------------------------------------------------------------
// Libro
// ---------------------------------------------------------------------------

export function WorkDialog({
  open,
  onClose,
  onCreated,
}: {
  open: boolean;
  onClose: () => void;
  onCreated: () => void;
}) {
  const philosophers = useAsync(() => corpus.listPhilosophers(), [open], { enabled: open });
  const [philosopherId, setPhilosopherId] = useState<number | "">("");
  const [title, setTitle] = useState("");
  const [year, setYear] = useState("");
  const [question, setQuestion] = useState("");
  const { busy, error, enviar } = useEnvio(() => {
    setTitle("");
    setYear("");
    setQuestion("");
    onCreated();
    onClose();
  });

  const autor = philosopherId === "" ? philosophers.data?.[0]?.id : philosopherId;
  const puedeGuardar = title.trim().length > 0 && autor !== undefined && !busy;
  const sinPensadores = philosophers.data?.length === 0;

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title="Nuevo libro"
      description="Una obra que vas a leer y anotar."
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancelar
          </Button>
          <Button
            variant="primary"
            disabled={!puedeGuardar}
            onClick={() =>
              enviar(() =>
                corpus.createWork({
                  philosopherId: autor!,
                  title: title.trim(),
                  originalYear: year.trim() ? Number(year) : null,
                  philosophicalProblem: question.trim() || null,
                  coreThesis: null,
                  directAdversaryId: null,
                }),
              )
            }
          >
            {busy && <Loader2 className="size-3.5 animate-spin" />}
            Guardar
          </Button>
        </>
      }
    >
      <div className="space-y-3.5">
        {sinPensadores && (
          <p className="rounded border border-accent-500/40 bg-accent-900/40 px-3 py-2 text-[12px] text-accent-300">
            Primero necesitas un pensador. Créalo y vuelve aquí.
          </p>
        )}

        <Field label="Título">
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Crítica de la razón pura"
            className={CAMPO_SERIF}
            autoFocus
          />
        </Field>

        <div className="grid gap-3 sm:grid-cols-[2fr_1fr]">
          <Field label="Autor">
            <select
              value={autor ?? ""}
              onChange={(e) => setPhilosopherId(Number(e.target.value))}
              className={CAMPO}
              disabled={sinPensadores}
            >
              {philosophers.data?.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.avatarEmoji ? `${p.avatarEmoji} ` : ""}
                  {p.name}
                </option>
              ))}
            </select>
          </Field>

          <Field label="Año" hint="Aproximado vale">
            <input
              value={year}
              onChange={(e) => setYear(e.target.value.replace(/[^\d-]/g, ""))}
              placeholder="1781"
              inputMode="numeric"
              className={CAMPO}
            />
          </Field>
        </div>

        <Field
          label="¿Cuál es la gran pregunta que intenta responder este libro?"
          hint="Lo más útil que puedes anotar. Te lo recordará todo lo demás."
        >
          <textarea
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            rows={3}
            placeholder="¿Hasta dónde puede conocer la razón sin apoyarse en la experiencia?"
            className={`${CAMPO_SERIF} resize-y`}
          />
        </Field>

        {error && <p className="text-[12px] text-fallacy-300">{error}</p>}
      </div>
    </Dialog>
  );
}

// ---------------------------------------------------------------------------
// Idea o pasaje
// ---------------------------------------------------------------------------

export function PassageDialog({
  open,
  onClose,
  onCreated,
  defaultWorkId,
}: {
  open: boolean;
  onClose: () => void;
  onCreated: () => void;
  defaultWorkId?: number;
}) {
  const works = useAsync(() => corpus.listWorks(), [open], { enabled: open });
  const [workId, setWorkId] = useState<number | "">("");
  const [text, setText] = useState("");
  const [locator, setLocator] = useState("");
  const [notes, setNotes] = useState("");
  const { busy, error, enviar } = useEnvio(() => {
    setText("");
    setLocator("");
    setNotes("");
    onCreated();
    onClose();
  });

  const libro = workId === "" ? (defaultWorkId ?? works.data?.[0]?.id) : workId;
  const puedeGuardar =
    text.trim().length > 0 && locator.trim().length > 0 && libro !== undefined && !busy;
  const sinLibros = works.data?.length === 0;

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title="Nueva idea"
      description="Un fragmento que quieres guardar, con lo que te sugiere a ti."
      wide
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancelar
          </Button>
          <Button
            variant="primary"
            disabled={!puedeGuardar}
            onClick={() =>
              enviar(() =>
                corpus.createPassage({
                  workId: libro!,
                  locator: locator.trim(),
                  textContent: text.trim(),
                  pageNumber: /^\d+$/.test(locator.trim()) ? Number(locator.trim()) : null,
                  personalNotes: notes.trim() || null,
                }),
              )
            }
          >
            {busy && <Loader2 className="size-3.5 animate-spin" />}
            Guardar
          </Button>
        </>
      }
    >
      <div className="space-y-3.5">
        {sinLibros && (
          <p className="rounded border border-accent-500/40 bg-accent-900/40 px-3 py-2 text-[12px] text-accent-300">
            Primero necesitas un libro. Créalo y vuelve aquí.
          </p>
        )}

        <div className="grid gap-3 sm:grid-cols-[2fr_1fr]">
          <Field label="Libro">
            <select
              value={libro ?? ""}
              onChange={(e) => setWorkId(Number(e.target.value))}
              className={CAMPO}
              disabled={sinLibros}
            >
              {works.data?.map((w) => (
                <option key={w.id} value={w.id}>
                  {w.philosopherName} — {w.title}
                </option>
              ))}
            </select>
          </Field>

          <Field label="Página o capítulo" hint="«142», «Cap. III», «AT VII 40»">
            <input
              value={locator}
              onChange={(e) => setLocator(e.target.value)}
              placeholder="142"
              className={`${CAMPO} font-mono`}
            />
          </Field>
        </div>

        <Field label="Cita o resumen del párrafo" hint="Lo que dice el texto.">
          <textarea
            value={text}
            onChange={(e) => setText(e.target.value)}
            rows={5}
            placeholder="Copia la frase tal cual, o resúmela con tus palabras."
            className={`${CAMPO_SERIF} resize-y`}
            autoFocus
          />
        </Field>

        <Field
          label="Tus notas"
          hint="Lo que piensas tú. Se guarda aparte de la cita a propósito: mezclarlas es la vía rápida a atribuirle al autor cosas que no dijo."
        >
          <textarea
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            rows={3}
            placeholder="Esto me recuerda a…  /  No me convence porque…"
            className={`${CAMPO_SERIF} resize-y`}
          />
        </Field>

        {error && <p className="text-[12px] text-fallacy-300">{error}</p>}
      </div>
    </Dialog>
  );
}
