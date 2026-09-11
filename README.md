# Organon Studio

[![CI](https://github.com/JosMart04/organon-studio/actions/workflows/ci.yml/badge.svg)](https://github.com/JosMart04/organon-studio/actions/workflows/ci.yml)

**Un cuaderno para leer filosofía en serio**, pensado para quien la lee por gusto y no para
especialistas en lógica formal.

Estás leyendo un libro de filosofía y quieres enterarte de verdad: qué sostiene el autor, en qué se
apoya, dónde flaquea y con quién está discutiendo sin decirlo. Organon Studio es para eso. No pide
saber qué es un modus tollens, y la notación lógica está escondida tras un desplegable para quien
quiera curiosear.

## Qué resuelve

| Módulo | Para qué sirve |
|---|---|
| **Leer** | El fragmento a la izquierda, lo que vas descubriendo a la derecha. Selecciona una frase y conviértela en nota, o pregunta qué entiende ese autor por una palabra concreta. En la pestaña **Documento** abres el PDF, Markdown o texto que estás leyendo: se queda en tu navegador (no se sube a ningún sitio), recuerda la página y lo que selecciones se guarda con su número de página. |
| **Desmontar** | Qué sostiene el autor, con qué razones y qué está dando por obvio sin decirlo. Se ordenan arrastrando, y cada crítica se ancla a la razón exacta que falla. |
| **Buscar** | Ctrl+K desde cualquier pantalla. Por palabras, al instante y sin preocuparse de las tildes («razon» encuentra «razón»); o por significado, con un modelo que corre en tu ordenador: «lo efímero de la existencia» encuentra la carta de Séneca sobre el tiempo sin compartir una sola palabra. |
| **Palabras** | «Sustancia» no quiere decir lo mismo en Descartes que en Spinoza. Muchas discusiones filosóficas son solo eso. Aquí se ven en columnas. |
| **El debate** | Quién refuta a quién y quién se apoya en quién, como un mapa navegable que se dibuja con lo que vas anotando. Se guarda entero, con su leyenda, como imagen PNG o como SVG. |
| **Asistente** | Un modelo que corre en tu propio ordenador: explica un pasaje denso, propone cómo desmontarlo y sugiere quién le llevaría la contraria. Opcional. |
| **Exportación** | Tus notas a Markdown (Obsidian) y, si te hace falta, bloques LaTeX para un trabajo. |
| **Copia de seguridad** | Todo el cuaderno en un único fichero `.json`, desde **Ajustes** (el engranaje al pie del menú). Se restaura fusionando con lo que ya tienes, sin duplicar, o sustituyéndolo entero. Antes de restaurar ves qué contiene, y si el fichero tiene algún problema no se toca nada. |

## Stack

- **Backend** — Java 25 · Spring Boot 4.1.1 · Spring Data JPA · Flyway · PostgreSQL 18 · Spring AI (Ollama)
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

Flyway crea el esquema. **El cuaderno arranca vacío a propósito**: se llena leyendo, con los botones
«Añadir» del menú lateral. Si quieres datos de ejemplo —el debate sobre causalidad entre Descartes,
Spinoza, Hume y Kant—, arranca una vez con `ORGANON_SEED_ENABLED=true`; la carga es idempotente.

**5. Asistente (opcional)**

```powershell
ollama serve
ollama pull gemma4:12b       # el asistente; o el modelo que prefieras
ollama pull embeddinggemma   # la búsqueda por significado (622 MB)
```

El modelo se elige con `OLLAMA_MODEL` (por defecto `gemma4:12b`). Uno de 12 000 millones de
parámetros puede tardar más de un minuto por respuesta en CPU; `gemma4:e2b` responde en unos
segundos con menos finura. **Si Ollama no está encendido la aplicación funciona igual**: el panel lo
detecta, lo dice y se sigue tomando notas a mano.

La búsqueda por significado usa `OLLAMA_EMBEDDING_MODEL` (por defecto `embeddinggemma`,
multilingüe). El backend indexa en segundo plano al arrancar y cada vez que guardas algo, y solo
recalcula lo que cambió. Sin Ollama, o sin ese modelo, la búsqueda sigue funcionando por palabras y
lo avisa.

## Pruebas

```powershell
cd server; .\mvnw.cmd test                            # unitarias, sin base de datos
cd server; .\mvnw.cmd test "-Dexcluded.test.groups="  # + integración contra PostgreSQL
cd client; npm run lint; npm run build
```

Las pruebas etiquetadas como `integracion` levantan el contexto de Spring contra la base de datos
local y comprueban que Flyway migre y que Hibernate valide el mapeo. Quedan fuera de la ejecución
por defecto y de CI, porque exigen un PostgreSQL vivo y el proyecto no usa contenedores.

## Una particularidad de Windows

El JDK crea el socket AF_UNIX de la tubería del selector de NIO en `java.io.tmpdir`. En algunas
máquinas Windows ese fichero queda inaccesible nada más crearse si nace bajo `%LOCALAPPDATA%`: el
`bind` funciona, el `connect` falla con `EINVAL` y **Tomcat no llega a levantar**, con un error que
habla de red y no del antivirus. `server/pom.xml` redirige el directorio a `${user.home}/.organon-uds`
mediante la propiedad `organon.uds.tmpdir`. Es inocuo en Linux y macOS: si el directorio no existe,
el JDK cae solo al loopback TCP.

## Despliegue

Ver [`DEPLOYMENT.md`](DEPLOYMENT.md).

## Licencia

Proyecto privado.
