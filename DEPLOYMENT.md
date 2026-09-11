# Despliegue de Organon Studio

Guía paso a paso para pasar del entorno local en Windows a producción, usando
únicamente niveles gratuitos y **sin contenedores**.

| Pieza | Servicio | Coste |
|---|---|---|
| Base de datos | **Neon** (PostgreSQL gestionado) | Gratis |
| Backend | **Render** (Web Service, entorno nativo Java) | Gratis |
| Frontend | **Vercel** | Gratis |

> Alternativas equivalentes: Supabase en lugar de Neon, Railway o Fly.io en
> lugar de Render. Los pasos cambian poco: lo único que el backend necesita es
> que le pasen `DATABASE_URL`, `PORT` y `CORS_ALLOWED_ORIGINS`.

---

## Orden de despliegue

El orden importa, porque cada pieza necesita un dato de la anterior:

```
1. Base de datos  →  produce DATABASE_URL
2. Backend        →  consume DATABASE_URL, produce la URL de la API
3. Frontend       →  consume la URL de la API, produce el dominio público
4. Volver al backend  →  fijar CORS_ALLOWED_ORIGINS con ese dominio
```

El paso 4 se olvida con facilidad y su síntoma es desconcertante: la interfaz
carga perfectamente pero todos los paneles salen vacíos, porque el navegador
bloquea las peticiones por CORS mientras el servidor responde 200.

---

## 1. Base de datos en Neon

1. Crear cuenta en [neon.tech](https://neon.tech) y un proyecto nuevo
   (región cercana a la del backend: si Render va en Frankfurt, Neon en Europa).
2. Nombre de la base: `organon`.
3. En **Connection Details**, copiar la cadena en formato **`postgres://`**
   (no la de `psql`). Tiene esta forma:

   ```
   postgres://usuario:contraseña@ep-algo-123.eu-central-1.aws.neon.tech/organon?sslmode=require
   ```

**No hace falta crear tablas a mano.** Flyway aplica `V1__init_schema.sql` en el
primer arranque del backend. El perfil de producción lleva
`baseline-on-migrate: true` precisamente para que la primera migración sobre una
base vacía no falle.

### Comprobar la conexión desde Windows

```powershell
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' "postgres://usuario:contraseña@ep-algo-123.eu-central-1.aws.neon.tech/organon?sslmode=require" -c "SELECT version();"
```

---

## 2. Backend en Render

1. En [render.com](https://render.com) → **New** → **Web Service** → conectar el
   repositorio `organon-studio`.
2. Configuración:

   | Campo | Valor |
   |---|---|
   | Root Directory | `server` |
   | Runtime | **Java** |
   | Build Command | `./mvnw clean package -DskipTests` |
   | Start Command | `java -jar target/organon-server-0.0.1-SNAPSHOT.jar` |
   | Instance Type | Free |

3. Variables de entorno:

   | Variable | Valor |
   |---|---|
   | `SPRING_PROFILES_ACTIVE` | `prod` |
   | `DATABASE_URL` | la cadena `postgres://…` de Neon |
   | `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` *(provisional; se corrige en el paso 4)* |
   | `JAVA_VERSION` | `25` |

   El asistente queda **desactivado** en producción: Ollama corre en la máquina del lector, no en
   Render. El perfil `prod` lo apaga solo, y la interfaz lo comunica sin romperse.

   `PORT` lo inyecta Render solo; el perfil de producción ya lo lee.

**Sobre `DATABASE_URL`:** Neon y Render la entregan en el formato de `libpq`
(`postgres://…`), que el driver JDBC **no** entiende. La clase
[`CloudDatabaseUrl`](server/src/main/java/studio/organon/server/config/CloudDatabaseUrl.java)
la traduce a `jdbc:postgresql://…` más usuario y contraseña antes de que Spring
arranque, y fuerza `sslmode=require` si el proveedor no lo incluye. No hay que
partir la cadena a mano.

### Verificar

```bash
curl https://TU-SERVICIO.onrender.com/actuator/health
curl https://TU-SERVICIO.onrender.com/api/v1/corpus/philosophers
```

El primero debe devolver `{"status":"UP"}`. El segundo, `[]`: la base está
migrada pero vacía, que es el estado correcto — el cuaderno se llena leyendo.

### Cargar los datos de ejemplo (opcional)

Para poblar la instalación con el debate Descartes–Spinoza–Hume–Kant, poner
`ORGANON_SEED_ENABLED=true`, reiniciar el servicio, comprobar que los datos
están y **volver a ponerlo en `false`**. El cargador es idempotente (no hace
nada si ya hay pensadores), pero dejarlo activo no aporta nada.

> **El plan gratuito de Render duerme el servicio tras 15 minutos sin tráfico.**
> La primera petición después de dormir tarda entre 30 y 60 segundos en
> responder. No es un fallo del despliegue.

---

## 3. Frontend en Vercel

1. En [vercel.com](https://vercel.com) → **Add New** → **Project** → importar
   `organon-studio`.
2. Configuración:

   | Campo | Valor |
   |---|---|
   | Framework Preset | Next.js |
   | Root Directory | `client` |

   El resto lo detecta solo a partir de `client/vercel.json`.

3. Variable de entorno (para **Production**, **Preview** y **Development**):

   | Variable | Valor |
   |---|---|
   | `NEXT_PUBLIC_API_URL` | `https://TU-SERVICIO.onrender.com/api/v1` |

   Ojo con el sufijo `/api/v1`: el cliente concatena las rutas directamente
   sobre ese valor.

4. **Deploy**.

> `NEXT_PUBLIC_*` se incrusta en el bundle **en tiempo de build**, no se lee en
> ejecución. Si cambias la URL de la API después, hay que volver a desplegar; no
> basta con guardar la variable.

---

## 4. Cerrar el círculo del CORS

Volver a Render y poner en `CORS_ALLOWED_ORIGINS` el dominio real de Vercel:

```
https://organon-studio.vercel.app
```

Para admitir también los despliegues de vista previa, separar por comas:

```
https://organon-studio.vercel.app,https://organon-studio-git-main-tuusuario.vercel.app
```

Render reinicia el servicio al guardar. La configuración la lee
[`WebConfig`](server/src/main/java/studio/organon/server/config/WebConfig.java),
que parte la cadena por comas.

---

## 5. Verificación de extremo a extremo

1. Abrir el dominio de Vercel.
2. Con el cuaderno vacío, crear un pensador y un libro desde **Añadir** en el
   menú lateral. Deben guardarse y aparecer al instante en los desplegables.
3. Ir a **Leer**: el selector debe listar el libro recién creado. En la pestaña
   **Documento**, abrir un PDF: debe verse y su texto debe poder seleccionarse.
   El fichero no sale del navegador, así que tampoco depende del backend.
4. Seleccionar una palabra del texto y pulsar **Ver término**.
5. Ir a **El debate**: deben aparecer los nodos, y **Imagen PNG** debe descargar
   el mapa completo con su leyenda. La exportación ocurre en el navegador, así
   que no depende del backend.
6. Abrir el **Asistente**: en producción debe decir que está desactivado, sin
   romper nada. Lo mismo con **Buscar** (Ctrl+K): la pestaña por palabras debe
   encontrar al pensador del paso 2, y la de significado avisar de que en
   producción solo se busca por palabras, porque Ollama no corre en Render.
7. Abrir **Ajustes** (engranaje al pie del menú) y pulsar **Descargar copia de
   seguridad**: debe bajar un fichero `organon-copia-….json`.
8. En la consola del navegador no debe haber errores de CORS.

Si los paneles salen vacíos y la consola muestra
`No 'Access-Control-Allow-Origin' header`, es el paso 4.

---

## Llevar tus notas del ordenador a producción

La base local y la de Neon son independientes. Para subir lo que ya tienes
anotado en Windows:

1. En local (`http://localhost:3000`): **Ajustes → Descargar copia de
   seguridad**.
2. En el dominio de Vercel: **Ajustes → Restaurar notas desde una copia**, elegir
   el fichero y comprobar el resumen.
3. Elegir **Fusionar** si producción ya tiene notas que quieres conservar: lo que
   coincida por nombre se omite, así que repetir la operación no duplica nada.
   **Sobrescribir todo** vacía el cuaderno de producción antes de cargar la copia
   y no se puede deshacer; la interfaz ofrece descargar antes lo que hay.

La restauración es todo o nada: si el fichero está mal, la respuesta enumera los
problemas y la base queda como estaba. El mismo camino sirve a la inversa, para
traer a local lo anotado en producción.

---

## Integración continua

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) se ejecuta en cada push y
cada pull request contra `main`:

- **Backend**: JDK 25 (Temurin), caché de Maven, `mvnw clean verify` y
  publicación del jar como artefacto.
- **Frontend**: Node 24, `npm ci`, `npm run lint` y `npm run build`.

Las pruebas etiquetadas como `integracion` quedan **fuera** de CI porque exigen
un PostgreSQL vivo y el proyecto no usa contenedores. Se ejecutan en local
contra la base de datos de desarrollo:

```powershell
cd server; .\mvnw.cmd test "-Dexcluded.test.groups="
```

---

## Problemas frecuentes

| Síntoma | Causa | Solución |
|---|---|---|
| `No suitable driver found for postgres://…` | `DATABASE_URL` llegó al datasource sin traducir | Comprobar que `SPRING_PROFILES_ACTIVE=prod` y que se arranca por `main` (no por un runner propio) |
| `Schema-validation: missing table [philosopher]` | Flyway no llegó a migrar | Revisar los logs de arranque; suele ser que el usuario de la base no tiene permiso de `CREATE` |
| `permission denied to create extension "unaccent"` | La migración de la búsqueda la crea, y el usuario no tiene permiso | En Neon y Supabase el propietario de la base puede. Si no, ejecutar una vez `CREATE EXTENSION unaccent;` desde su consola SQL y volver a desplegar |
| La interfaz carga pero todo sale vacío | CORS | Paso 4 |
| `FATAL: too many connections for role` | Se agotó el pool del tier gratuito | Bajar `DB_POOL_MAX` a `3` |
| La primera petición tarda un minuto | El servicio gratuito de Render estaba dormido | Es el comportamiento del plan; un plan de pago lo elimina |
| Un cambio en `NEXT_PUBLIC_API_URL` no surte efecto | Se incrusta en tiempo de build | Volver a desplegar en Vercel |
| Un PDF escaneado sale en blanco, o la consola pide `wasmUrl` o `standardFontDataUrl` | Faltan los recursos de pdf.js en `public/pdfjs` | Comprobar que Vercel construye con `npm run build`: su `prebuild` los copia desde `pdfjs-dist` |
| Restaurar responde «Esta copia se hizo con una versión más nueva» | El fichero sale de un despliegue más reciente que el que lo recibe | Desplegar la misma versión en ambos lados y repetir |

---

## Nota sobre el entorno local

El desarrollo en Windows no usa nada de lo anterior: PostgreSQL 18 nativo,
`mvnw.cmd spring-boot:run` y `npm run dev`. Ver el [README](README.md).

Una particularidad de este equipo, ya resuelta en `server/pom.xml`: el JDK crea
el socket AF_UNIX de la tubería del selector de NIO en `java.io.tmpdir`, y en
esta máquina ese fichero queda inaccesible nada más crearse cuando nace bajo
`%LOCALAPPDATA%`, de modo que Tomcat no llega a levantar. La propiedad
`organon.uds.tmpdir` lo redirige a `${user.home}/.organon-uds`. Es inocua en
Linux y en macOS: si el directorio no existe, el `bind` falla y el JDK cae solo
al loopback TCP.
