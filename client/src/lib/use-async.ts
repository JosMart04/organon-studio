"use client";

import { useCallback, useEffect, useState } from "react";

export interface AsyncState<T> {
  data: T | undefined;
  error: string | undefined;
  loading: boolean;
  reload: () => void;
}

interface Snapshot<T> {
  key: string;
  data?: T;
  error?: string;
}

/**
 * Carga un recurso y lo recarga cuando cambian sus dependencias.
 *
 * `loading` se deriva comparando la petición vigente con la última respuesta
 * aceptada, en vez de guardarse en un estado propio: así el efecto solo escribe
 * desde los callbacks asíncronos y no encadena renders.
 *
 * La bandera de cancelación descarta respuestas de peticiones ya superadas. En
 * el lector se cambia de pasaje más rápido de lo que responde el servidor, y sin
 * esto una respuesta lenta pisaría el contenido del pasaje actual.
 */
export function useAsync<T>(
  loader: () => Promise<T>,
  deps: React.DependencyList,
  options: { enabled?: boolean } = {},
): AsyncState<T> {
  const enabled = options.enabled ?? true;
  const [nonce, setNonce] = useState(0);
  const [snapshot, setSnapshot] = useState<Snapshot<T>>({ key: "" });

  // Identidad de la petición vigente. Sirve de dependencia del efecto y de
  // referencia contra la que se compara la última respuesta. Se calcula en el
  // render: serializar un puñado de escalares es más barato que memorizarlo.
  const key = JSON.stringify([deps, enabled, nonce]);

  useEffect(() => {
    if (!enabled) return;
    let cancelled = false;

    loader()
      .then((data) => {
        if (!cancelled) setSnapshot({ key, data });
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setSnapshot({ key, error: err instanceof Error ? err.message : String(err) });
        }
      });

    return () => {
      cancelled = true;
    };
    // El loader se recrea en cada render; `key` resume las deps declaradas.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key, enabled]);

  const reload = useCallback(() => setNonce((n) => n + 1), []);

  return {
    // Se conserva el último dato bueno mientras llega el siguiente: evita que
    // el panel parpadee a vacío en cada recarga.
    data: snapshot.data,
    error: snapshot.key === key ? snapshot.error : undefined,
    loading: enabled && snapshot.key !== key,
    reload,
  };
}
