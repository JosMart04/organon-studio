<div align="center">

# Organon Studio

**Integrated Dialectical Reading Environment (IDRE) & Philosophical Second Brain**

*Un entorno para leer filosofía en serio: reconstruye argumentos, sigue quién discute con quién
y ponte a prueba, con toda la inteligencia corriendo en tu propio ordenador.*

[![CI](https://github.com/JosMart04/organon-studio/actions/workflows/ci.yml/badge.svg)](https://github.com/JosMart04/organon-studio/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/25/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Next.js](https://img.shields.io/badge/Next.js-16.3-000000?logo=nextdotjs&logoColor=white)](https://nextjs.org)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org)
[![Ollama](https://img.shields.io/badge/Ollama-gemma4%20·%20embeddinggemma-000000?logo=ollama&logoColor=white)](https://ollama.com)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-v4-38BDF8?logo=tailwindcss&logoColor=white)](https://tailwindcss.com)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

</div>

---

## Por qué existe

Estás leyendo a Séneca, a Hume o a Kant y quieres enterarte **de verdad**: qué sostiene el autor, en
qué se apoya, qué da por obvio sin decirlo y con quién está discutiendo aunque no lo nombre. Las
herramientas de notas no ayudan con eso, porque no fueron pensadas para ello:

| Lo que necesita la lectura filosófica | Notion / Obsidian | Organon Studio |
|---|---|---|
| **Sobrecarga semántica**: «sustancia» no significa lo mismo en Descartes que en Spinoza | Una nota por término, o enlaces que mezclan los dos sentidos | Una definición **por autor y por obra**, y un comparador que las pone en columnas |
| **Supuestos implícitos**: la premisa que el autor nunca escribió pero que su inferencia necesita | No existe el concepto | Cada razón puede marcarse como supuesto, y es justo por donde pregunta el tutor |
| **Controversia**: quién refuta, quién presupone, quién radicaliza a quién | Enlaces sin tipo: A ↔ B | Aristas **tipadas** entre ideas, con su mapa navegable y exportable |
| **Crítica anclada**: la objeción no es a la idea entera, sino a una razón concreta | Comentario al margen | La objeción se ancla **a la premisa exacta** que falla |
| **Comprobar que lo has entendido** | Releer y subrayar | *Active recall*: el sistema te desafía y valora tu razonamiento |
| **Privacidad** | Tus notas en la nube ajena | PostgreSQL y el modelo **en tu máquina**; sin cuentas ni telemetría |

### Filosofía de diseño: divulgación progresiva

El usuario es alguien que lee filosofía por gusto —un ingeniero, un estudiante, un curioso—, no un
doctorando en lógica. De ahí tres reglas que gobiernan toda la interfaz:

1. **Lenguaje natural por defecto.** La pantalla dice «razones», «supuesto implícito» o «se basa en»,
   no «premisas», «entimema» ni «PRESUPONE». El vocabulario vive en un único fichero
   ([`vocabulario.ts`](client/src/lib/vocabulario.ts)): cambiar una palabra la cambia en toda la app.
2. **El rigor está, pero no estorba.** La formalización en KaTeX, el esquema silogístico y el aparato
   crítico existen y son exactos; viven tras un desplegable para quien quiera curiosear.
3. **Nada se guarda sin que lo apruebes.** El asistente propone; tú decides. Ninguna respuesta del
   modelo llega a la base de datos sin pasar por tus manos.

---

## Módulos y capacidades

### 📖 Lector a doble panel

El texto a la izquierda, lo que vas descubriendo a la derecha (glosario del autor, ideas del libro,
críticas). En la pestaña **Documento** abres el PDF, Markdown o `.txt` que estás leyendo:

- **Renderizado nativo en el navegador** con `react-pdf` / pdf.js. El fichero **no se sube a ningún
  sitio**: se guarda en IndexedDB con su última página, para seguir donde lo dejaste.
- **Selección con sentido**: al marcar una frase aparece un menú para guardarla como nota, convertirla
  en razón de una idea, mirar qué entiende ese autor por esa palabra o preguntarle al asistente.
- **Numeración de página automática**: lo que guardas llega con su `pág. N`, y las palabras partidas
  a final de línea (`la fa-` / `ma`) se unen solas antes de guardarse.
- Un PDF escaneado sin capa de texto se detecta y se avisa, en vez de dejarte seleccionar en vano.

### 💡 Disección de ideas

Reconstruye el argumento sin obligarte a saber lógica formal:

- **Razones** en el orden que quieras (se reordenan arrastrando), **supuestos implícitos** marcados con
  un clic y **conclusión** señalada como tal.
- **Críticas ancladas**: cada objeción apunta a la razón exacta que falla, con su tipo (contraejemplo,
  petición de principio, falsa dicotomía…).
- **Revisión**: somete la idea a pruebas de estrés y deja un veredicto — *se sostiene*, *tiene un
  fallo* o *sin revisar* — con los hallazgos explicados en cristiano.
- La notación lógica (KaTeX) es opcional y va plegada.

### 🕸️ Grafo dialéctico interactivo

- Mapa navegable en [`@xyflow/react`](https://reactflow.dev): autores → libros → ideas, y entre ideas
  **aristas tipadas** — *Refuta a*, *Se basa en*, *Amplía a*, *Lleva más lejos a*, *Matiza a* — cada
  una con su color y su etiqueta.
- Filtros por época y por palabra del glosario, para quedarte solo con quienes discuten un concepto.
- **Exportación en alta resolución a PNG y SVG**: sale el lienzo completo, no lo que se ve en
  pantalla, con leyenda, título, fecha y los filtros aplicados. El PNG se compone a doble densidad.

### 🤖 Tutor socrático local (Ollama)

Cuatro gestos, todos contra un modelo que corre en tu CPU:

| Gesto | Qué hace |
|---|---|
| **Explícamelo sencillo** | Dos párrafos: qué dice el autor y por qué le importaba |
| **Desglosar razones** | Propone tesis, razones y **lo que da por supuesto sin decirlo** |
| **¿Quién le lleva la contraria?** | Sugiere pensadores rivales y el tipo de disputa, prefiriendo los que ya tienes |
| **Desafíame** (*active recall*) | Te pregunta por un supuesto implícito de **tu** idea, propone un caso que la pone en aprietos y valora tu respuesta |

La valoración mide **cómo razonas, no si coincides con el autor**: discrepar con buenas razones es una
respuesta sólida. Queda historial, y el repaso vuelve antes sobre lo que salió flojo.

> **Si Ollama no está encendido, la aplicación funciona igual.** Lo detecta, lo dice con claridad y
> todo lo demás sigue en pie.

### 🔍 Búsqueda híbrida (`Ctrl` + `K`)

- **Por palabras**: instantánea mientras escribes, insensible a tildes (`razon` encuentra «razón»),
  con prefijos (`razo`), resaltado de coincidencias y pesos por campo (lo que dice el texto pesa más
  que tus notas al margen). Nunca necesita Ollama.
- **Por significado**: vectores de `embeddinggemma` calculados en tu máquina. «Lo efímero de la
  existencia» encuentra la carta de Séneca sobre el tiempo **sin compartir una sola palabra**.
- Ambas listas se fusionan por posición (*reciprocal rank fusion*), porque `ts_rank` y el coseno no
  están en la misma escala. Sin modelo, degrada a solo palabras y lo avisa.

### 🛡️ Portabilidad y respaldo

- **Copia de seguridad en un clic**: todo el cuaderno —incluidos los repasos— en un único `.json`
  portable, con referencias internas en vez de ids de base de datos.
- **Restauración transaccional**: valida el fichero entero antes de escribir nada y te enumera todos
  los problemas si los hay. **Fusionar** no duplica; **sobrescribir** exige confirmación explícita.
  Todo o nada: cualquier fallo deja la base exactamente como estaba.
- **Exportación a Zettelkasten y LaTeX**: tus notas de un libro a Markdown (listo para Obsidian) y la
  formalización de una idea a un bloque LaTeX para un trabajo.

---

## Arquitectura del sistema

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│  NAVEGADOR                                client/ · Next.js 16 · React 19    │
│                                                                              │
│   ┌────────┬───────────┬──────────┬────────┬─────────┬──────────────────┐    │
│   │  Leer  │ Desmontar │ Palabras │ Debate │ Repasar │  Buscar (Ctrl+K) │    │
│   └────────┴───────────┴──────────┴────────┴─────────┴──────────────────┘    │
│                                                                              │
│   PDF · Markdown · última página leída  ->  IndexedDB (nunca sale de aquí)   │
└───────────────────────────────┬──────────────────────────────────────────────┘
                                │  HTTP/JSON · /api/v1/* · CORS por origen
┌───────────────────────────────┴──────────────────────────────────────────────┐
│  API REST                           server/ · Spring Boot 4.1 · Java 25      │
│                                                                              │
│   web -> service -> domain    (corpus · semantics · logic · dialectic)       │
│                                                                              │
│   backup/   copia y restauración todo-o-nada (formato 2)                     │
│   search/   texto completo en español + coseno sobre vectores locales        │
│   review/   qué toca repasar, su historial y el progreso                     │
│   ai/       SocraticService · degradación elegante si no hay modelo          │
└───────────┬────────────────────────────────────────┬─────────────────────────┘
            │ JDBC                                   │ HTTP · localhost:11434
┌───────────┴────────────────────────┐   ┌───────────┴─────────────────────────┐
│  PostgreSQL 18 · nativo            │   │  Ollama · CPU local                 │
│  Flyway V1…V4 · ddl-auto validate  │   │  gemma4:e2b      -> tutor socrático │
│  tsvector GIN · es_organon         │   │  embeddinggemma  -> significado     │
│  semantic_embedding REAL[]         │   │  sin salida a internet              │
└────────────────────────────────────┘   └─────────────────────────────────────┘
```

**Decisiones que explican el diagrama**

- **Sin Docker, sin contenedores.** Todo corre nativo en Windows: PostgreSQL como servicio, el backend
  con `mvnw.cmd`, el frontend con `npm`, Ollama como proceso propio.
- **El esquema lo posee Flyway**; Hibernate arranca en `ddl-auto: validate`, de modo que una entidad
  desalineada con su tabla falla al arrancar, no a mitad de una sesión de lectura.
- **pgvector no está disponible de forma nativa en Windows** (exige compilar con Visual Studio): los
  vectores se guardan como `REAL[]` normalizados y el coseno se calcula en Java. Para un cuaderno
  personal son milisegundos.
- **El modelo nunca bloquea una transacción.** Las llamadas a Ollama ocurren fuera de la base: se lee,
  se pregunta, se guarda.

### Stack y por qué

| Capa | Tecnología | Por qué esta |
|---|---|---|
| Lenguaje backend | **Java 25** | LTS reciente: *records*, *pattern matching* y texto en bloque hacen los DTO y los prompts legibles |
| Framework | **Spring Boot 4.1.1** | Autoconfiguración, validación y manejo de errores RFC 9457 sin escribir infraestructura |
| Persistencia | **Spring Data JPA + Hibernate 7** | El dominio filosófico es relacional puro: obras, pasajes, premisas, relaciones |
| Migraciones | **Flyway** | Historial versionado y reproducible del esquema (V1…V4) |
| Base de datos | **PostgreSQL 18** | `tsvector` + GIN, `unaccent`, `NULLS NOT DISTINCT`, `TRUNCATE` transaccional y arrays nativos |
| IA | **Spring AI 2.0.1 + Ollama** | Cliente tipado con salida estructurada a *records*; el modelo es local y sustituible |
| Frontend | **Next.js 16 (App Router) + React 19** | Rutas por fichero, componentes de cliente y una interfaz que corre entera en tu navegador |
| Tipos | **TypeScript** | El contrato de la API se refleja en `types/organon.ts` y el compilador lo vigila |
| Estilos | **Tailwind CSS v4** | Tema en CSS (`@theme`) y clases de utilidad: una sola paleta para toda la app |
| Grafo | **@xyflow/react 12** | Nodos personalizados, aristas tipadas, minimapa y control total del lienzo |
| PDF | **react-pdf 11 / pdf.js** | Capa de texto seleccionable en el navegador, sin subir el fichero |
| Markdown | **react-markdown 10** | No interpreta HTML incrustado: un `<script>` en tus apuntes nunca se ejecuta |
| Fórmulas | **KaTeX** | Notación lógica nítida y rápida, plegada por defecto |
| Interacción | **@dnd-kit** | Reordenar razones arrastrando, accesible con teclado |
| Exportación | **html-to-image 1.11.11** | Versión fijada a propósito: las posteriores exportan mal el lienzo |
| CI | **GitHub Actions** | `verify` del backend y `lint` + `build` del frontend en cada push |

### Modelo de datos

```text
philosopher ──< work ──< passage
                 │         └──< argument ──< premise ──< objection
                 │                  │            └── is_enthymeme  (supuesto implícito)
                 │                  └──< review_attempt            (repaso y su valoración)
                 └──< term_definition >── semantic_concept         (una acepción por autor y obra)

argument >──< dialectical_relation ──< argument     (REFUTA · PRESUPONE · EXTIENDE · RADICALIZA · MATIZA)
semantic_embedding (entity_type, entity_id, model)  (derivada: se regenera sola)
```

| Migración | Qué añade |
|---|---|
| `V1__init_schema.sql` | Corpus, semántica, lógica y red dialéctica |
| `V2__reader_friendly_fields.sql` | Emoji del pensador y notas personales del pasaje |
| `V3__search_index.sql` | `unaccent`, configuración `es_organon`, columnas `tsvector` generadas y tabla de vectores |
| `V4__review_attempts.sql` | Historial de repasos con su valoración |

---

## Puesta en marcha (Windows nativo)

### Requisitos

| Requisito | Versión | Comprobación |
|---|---|---|
| JDK | **25** | `java -version` |
| Node.js | **24** | `node -v` |
| PostgreSQL | **18**, como servicio | `Get-Service *postgres*` |
| Ollama | cualquiera reciente *(opcional)* | `ollama --version` |
| Git | cualquiera | `git --version` |

> No hace falta instalar Maven: el repositorio trae el *wrapper* (`mvnw.cmd`).

### 1. Base de datos

```powershell
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -U postgres -h localhost `
  -c "CREATE ROLE organon_admin WITH LOGIN CREATEDB PASSWORD 'tu_password';" `
  -c "CREATE DATABASE organon_local_db WITH OWNER organon_admin ENCODING 'UTF8' TEMPLATE template0;"
```

No hay que crear tablas: Flyway aplica `V1`…`V4` en el primer arranque.

### 2. Variables de entorno

```powershell
Copy-Item server\.env.example server\.env        # y edita SPRING_DATASOURCE_PASSWORD
Copy-Item client\.env.example client\.env.local
```

`server/.env` (lo carga Spring con `spring.config.import`; está en `.gitignore`):

| Variable | Por defecto | Para qué |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/organon_local_db` | Base local |
| `SPRING_DATASOURCE_PASSWORD` | — | La contraseña del rol `organon_admin` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Origen del frontend |
| `OLLAMA_MODEL` | `gemma4:12b` | Modelo del tutor (`gemma4:e2b` responde en segundos) |
| `OLLAMA_EMBEDDING_MODEL` | `embeddinggemma` | Modelo de la búsqueda por significado |
| `ORGANON_AI_TIMEOUT` | `300s` | Espera máxima: un modelo en CPU tarda |
| `ORGANON_SEED_ENABLED` | `false` | Datos de ejemplo (debate Descartes–Spinoza–Hume–Kant) |

### 3. Arrancar los dos servidores (terminales en paralelo)

```powershell
# Terminal 1 — backend en http://localhost:8080
cd server; .\mvnw.cmd spring-boot:run
```

```powershell
# Terminal 2 — frontend en http://localhost:3000
cd client; npm install; npm run dev
```

`npm run dev` ejecuta antes `scripts/copiar-recursos-pdf.mjs`, que copia a `public/pdfjs/` los
recursos que pdf.js necesita para PDF reales (fuentes estándar, CMaps y los decodificadores wasm de
los escaneos).

### 4. Asistente local (opcional)

```powershell
# Terminal 3
ollama serve
ollama pull gemma4:e2b       # tutor socrático: respuestas en segundos en CPU
ollama pull embeddinggemma   # búsqueda por significado (622 MB, multilingüe)
```

El backend indexa por significado en segundo plano al arrancar y cada vez que guardas algo, y solo
recalcula lo que cambió.

### 5. Comprobación rápida

```powershell
# El backend responde y la base está migrada (lista vacía = correcto)
curl.exe http://localhost:8080/actuator/health
curl.exe http://localhost:8080/api/v1/corpus/philosophers

# Estado del asistente y del índice de significado
curl.exe http://localhost:8080/api/v1/ai/status
curl.exe http://localhost:8080/api/v1/search/status

# Búsqueda por palabras (sin tildes) y copia de seguridad completa
curl.exe "http://localhost:8080/api/v1/search/text?q=razon"
curl.exe -o copia.json http://localhost:8080/api/v1/backup/export
```

```powershell
# Lo mismo, con PowerShell nativo y salida legible
Invoke-RestMethod http://localhost:8080/api/v1/ai/status | Format-List
```

Después, abre **http://localhost:3000**. El cuaderno arranca vacío a propósito: se llena leyendo, con
los botones **Añadir** del menú lateral.

### Pruebas

```powershell
cd server; .\mvnw.cmd test                            # unitarias, sin base de datos ni Ollama
cd server; .\mvnw.cmd test "-Dexcluded.test.groups="  # + integración contra PostgreSQL
cd server; .\mvnw.cmd clean verify                    # lo que ejecuta CI
cd client; npm run lint; npx tsc --noEmit; npm run build
```

Las pruebas etiquetadas como `integracion` levantan el contexto de Spring contra la base local, cada
una dentro de una transacción que se revierte al terminar. Quedan fuera de CI porque exigen un
PostgreSQL vivo y el proyecto no usa contenedores.

### Una particularidad de Windows

El JDK crea el socket AF_UNIX de la tubería del selector de NIO en `java.io.tmpdir`. En algunas
máquinas ese fichero queda inaccesible nada más crearse si nace bajo `%LOCALAPPDATA%`: el `bind`
funciona, el `connect` falla con `EINVAL` y **Tomcat no llega a levantar**, con un error que habla de
red y no del antivirus. `server/pom.xml` redirige ese directorio a `${user.home}/.organon-uds`. Es
inocuo en Linux y macOS.

---

## Estructura del repositorio

```text
organon-studio/
├── server/                                   # API REST (Spring Boot 4.1 · Java 25)
│   ├── src/main/java/studio/organon/server/
│   │   ├── domain/          corpus · semantics · logic · dialectic · review   (entidades JPA)
│   │   ├── repository/      Spring Data JPA
│   │   ├── service/         corpus, semántica, argumentos, auditoría, exportación
│   │   ├── web/             controladores REST + DTO
│   │   ├── ai/              SocraticService, sonda de Ollama, DTO de la IA
│   │   ├── search/          indexador de vectores, fusión de rankings, consultas FTS
│   │   ├── review/          planificador del repaso, servicio y controlador
│   │   ├── backup/          formato portable, validador e importador transaccional
│   │   ├── config/          CORS y ajustes de arranque
│   │   └── error/           ProblemDetail con mensajes para el lector
│   ├── src/main/resources/
│   │   ├── application.yml           configuración de la aplicación
│   │   └── db/migration/             V1…V4 (Flyway)
│   ├── src/test/java/…               unitarias + integración (`@Tag("integracion")`)
│   └── mvnw.cmd                      wrapper de Maven
│
├── client/                                   # Interfaz (Next.js 16 · React 19)
│   ├── src/app/             rutas: /reader /arguments/builder /glossary /graph /review
│   ├── src/components/
│   │   ├── reader/          visor de PDF, Markdown, menú de selección, documentos locales
│   │   ├── arguments/       constructor de ideas, premisas arrastrables, objeciones
│   │   ├── graph/           nodos del mapa y exportación PNG/SVG
│   │   ├── review/          tarjeta de repaso
│   │   ├── search/          paleta Ctrl+K
│   │   ├── settings/        copia de seguridad y restauración
│   │   ├── ai/              panel del asistente
│   │   └── ui/              diálogo, panel, insignias, KaTeX
│   ├── src/lib/             cliente de la API, vocabulario, IndexedDB, exportación del grafo
│   ├── src/types/           espejo tipado del contrato de la API
│   └── scripts/             copia de recursos de pdf.js a public/
│
├── .github/workflows/ci.yml                  backend `verify` + frontend `lint`/`build`
└── README.md
```

---

## Atajos de teclado

| Atajo | Dónde | Qué hace |
|---|---|---|
| `Ctrl` + `K` · `⌘` + `K` | Cualquier pantalla | Abre la búsqueda del cuaderno |
| `↑` `↓` | Buscador | Recorre los resultados |
| `Enter` | Buscador | Abre el resultado; en «Por significado», lanza la búsqueda |
| `Esc` | Diálogos, buscador, menú de selección | Cierra sin guardar |
| Arrastrar | Razones de una idea | Reordena las premisas |
| Selección de texto | Lector | Menú flotante: guardar, crear razón, ver término, preguntar |

## Buenas prácticas de lectura

1. **Separa lo que dice el autor de lo que piensas tú.** La cita va en el fragmento; tus notas, en su
   campo aparte. Mezclarlas es la vía rápida a atribuirle cosas que no dijo.
2. **Anota la pregunta del libro antes que sus tesis.** «¿Qué problema intenta resolver?» ordena todo
   lo demás.
3. **Marca los supuestos implícitos en cuanto los sospeches.** Suelen ser el punto por donde el
   razonamiento cede, y son el material del repaso.
4. **Una idea, un argumento.** Si necesitas dos conclusiones, son dos ideas.
5. **Conecta en cuanto veas la disputa.** El mapa solo es útil si las aristas se anotan mientras la
   discusión está fresca.
6. **Repasa antes de releer.** Si no puedes defender una idea ante un contraejemplo, subrayarla otra
   vez no lo arregla.
7. **Haz una copia antes de cualquier operación grande.** *Ajustes → Descargar copia de seguridad*
   tarda un segundo.

---

## Licencia

[MIT](LICENSE).
