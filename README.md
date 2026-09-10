# Organon Studio

**Entorno Integrado de Lectura y Análisis Crítico (IDRE)** para estudiantes e investigadores de filosofía.

Organon Studio no es un lector de PDF ni un gestor de notas. Es un banco de trabajo para la
operación central del oficio filosófico: tomar un texto fuente, extraer su argumento, reconstruirlo
en forma estándar, someterlo a estrés y situarlo en la red de debate a la que pertenece.

## Qué resuelve

| Módulo | Problema que ataca |
|---|---|
| **Lector de doble panel** | Leer la fuente primaria sin perder de vista el aparato analítico. Texto a la izquierda, glosario/argumentos/objeciones sincronizados a la derecha. |
| **Reconstrucción formal** | Pasar de la prosa al esquema estándar: premisas ordenadas, entimemas explicitados, conclusión, formalización en LaTeX. |
| **Auditoría de validez** | Detectar supuestos implícitos y anclar objeciones tipificadas (contraejemplo, petición de principio, falacia formal…) a la premisa exacta que las merece. |
| **Sobrecarga semántica** | «Sustancia» no significa lo mismo en Descartes que en Spinoza. El glosario define cada término **por autor y por obra**, y los compara lado a lado. |
| **Grafo dialéctico** | Ver quién refuta, presupone, extiende o radicaliza a quién, como un grafo navegable en vez de una bibliografía plana. |
| **Exportación** | Sacar el trabajo a Markdown Zettelkasten (Obsidian) y a bloques LaTeX listos para un artículo. |

## Stack

- **Backend** — Java 25 · Spring Boot 4.1.1 · Spring Data JPA · Flyway · PostgreSQL 18
- **Frontend** — Next.js 16 (App Router) · TypeScript · Tailwind CSS v4 · `@xyflow/react` · KaTeX · `@dnd-kit`
- **Infra** — Windows nativo en desarrollo (sin Docker) · Vercel + Render + Neon en producción

## Puesta en marcha (Windows)

Requisitos ya instalados: JDK 25, Node 24, PostgreSQL 18 corriendo como servicio.

**1. Base de datos**

```powershell
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -U postgres -h localhost -c "CREATE ROLE organon_admin WITH LOGIN CREATEDB PASSWORD 'tu_password';" -c "CREATE DATABASE organon_local_db WITH OWNER organon_admin ENCODING 'UTF8' TEMPLATE template0;"
```

**2. Variables de entorno**

```powershell
Copy-Item server\.env.example server\.env      # y edita la contraseña
Copy-Item client\.env.example client\.env.local
```

**3. Backend** (`http://localhost:8080`)

```powershell
cd server; .\mvnw.cmd spring-boot:run
```

**4. Frontend** (`http://localhost:3000`)

```powershell
cd client; npm install; npm run dev
```

Flyway crea el esquema y el `DataInitializer` carga la semilla filosófica en el primer arranque:
el debate moderno sobre causalidad y sustancia entre **Descartes, Spinoza, Hume y Kant**.

## Despliegue

Ver [`DEPLOYMENT.md`](DEPLOYMENT.md).

## Licencia

Proyecto privado.
