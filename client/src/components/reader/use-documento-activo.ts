"use client";

import { useState } from "react";
import { useAsync } from "@/lib/use-async";
import {
  abrirFichero,
  actualizarDocumento,
  cargarDocumento,
  tipoDeFichero,
  type DocumentoLocal,
} from "@/lib/local-documents";

export interface DocumentoActivo {
  documento: DocumentoLocal | undefined;
  cargando: boolean;
  /** La URL apunta a un documento que este navegador ya no guarda. */
  perdido: boolean;
  aviso: string | undefined;
  error: string | undefined;
  abriendo: boolean;
  abrir: (fichero: File) => Promise<DocumentoLocal | undefined>;
  vincular: (workId: number | undefined) => void;
  recordarPagina: (pagina: number) => void;
}

/**
 * El documento que indica la URL. Vive en la página y no en el panel para que
 * cambiar de pestaña no pierda la página por la que se iba leyendo.
 */
export function useDocumentoActivo(documentoId: string | undefined): DocumentoActivo {
  // Recién abierto no hace falta releerlo de IndexedDB; y si no se pudo guardar,
  // es la única copia que hay.
  const [enMemoria, setEnMemoria] = useState<DocumentoLocal>();
  const [vinculo, setVinculo] = useState<{ id: string; workId?: number }>();
  const [pagina, setPagina] = useState<{ id: string; numero: number }>();
  const [aviso, setAviso] = useState<{ id: string; texto: string }>();
  const [error, setError] = useState<string>();
  const [abriendo, setAbriendo] = useState(false);

  const enMemoriaVigente = enMemoria?.id === documentoId ? enMemoria : undefined;
  const guardado = useAsync(() => cargarDocumento(documentoId!), [documentoId], {
    enabled: documentoId !== undefined && enMemoriaVigente === undefined,
  });

  const base =
    enMemoriaVigente ?? (guardado.data?.id === documentoId ? guardado.data : undefined);
  const documento = base && {
    ...base,
    workId: vinculo?.id === base.id ? vinculo.workId : base.workId,
    ultimaPagina: pagina?.id === base.id ? pagina.numero : base.ultimaPagina,
  };

  const abrir = async (fichero: File) => {
    const tipo = tipoDeFichero(fichero);
    if (!tipo) {
      setError("Solo puedo abrir PDF, Markdown (.md) o texto (.txt).");
      return undefined;
    }
    setAbriendo(true);
    setError(undefined);
    try {
      const apertura = await abrirFichero(fichero, tipo);
      setEnMemoria(apertura.documento);
      setAviso(apertura.aviso ? { id: apertura.documento.id, texto: apertura.aviso } : undefined);
      return apertura.documento;
    } catch {
      setError("No he podido abrir ese fichero.");
      return undefined;
    } finally {
      setAbriendo(false);
    }
  };

  const vincular = (workId: number | undefined) => {
    if (!documento) return;
    setVinculo({ id: documento.id, workId });
    void actualizarDocumento(documento.id, { workId });
  };

  const recordarPagina = (numero: number) => {
    if (!documento) return;
    setPagina({ id: documento.id, numero });
    void actualizarDocumento(documento.id, { ultimaPagina: numero });
  };

  return {
    documento,
    cargando: documentoId !== undefined && !base && guardado.loading,
    perdido: documentoId !== undefined && !base && !guardado.loading,
    aviso: aviso?.id === documento?.id ? aviso?.texto : undefined,
    error,
    abriendo,
    abrir,
    vincular,
    recordarPagina,
  };
}
