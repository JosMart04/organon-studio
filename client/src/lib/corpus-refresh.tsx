"use client";

import { createContext, useCallback, useContext, useMemo, useState } from "react";

/**
 * Un contador que sube cada vez que se añade algo al cuaderno.
 *
 * Las pantallas son componentes de cliente que cargan sus datos con `useAsync`,
 * así que `router.refresh()` de Next no les afecta: solo revalida componentes de
 * servidor. Incluir esta versión en las dependencias de la carga es lo que hace
 * que un pensador recién creado aparezca en el desplegable sin recargar.
 */
const CorpusRefreshContext = createContext<{
  version: number;
  refrescar: () => void;
}>({ version: 0, refrescar: () => {} });

export function CorpusRefreshProvider({ children }: { children: React.ReactNode }) {
  const [version, setVersion] = useState(0);
  const refrescar = useCallback(() => setVersion((v) => v + 1), []);
  const value = useMemo(() => ({ version, refrescar }), [version, refrescar]);

  return (
    <CorpusRefreshContext.Provider value={value}>{children}</CorpusRefreshContext.Provider>
  );
}

/** Añádelo a las dependencias de `useAsync` en cualquier vista que liste corpus. */
export function useCorpusVersion(): number {
  return useContext(CorpusRefreshContext).version;
}

/** Llámalo tras crear algo para que las vistas abiertas se enteren. */
export function useRefrescarCorpus(): () => void {
  return useContext(CorpusRefreshContext).refrescar;
}
