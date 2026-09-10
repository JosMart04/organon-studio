import type { NextConfig } from "next";

/**
 * El cliente habla con el backend desde el navegador, no desde el servidor de
 * Next, así que la única variable que importa es NEXT_PUBLIC_API_URL, que se
 * inyecta en el bundle en tiempo de build. En Vercel se define en el proyecto;
 * en local sale de client/.env.local.
 */
const nextConfig: NextConfig = {
  // Un fallo de tipos no debe colarse en un despliegue. En Next 16 el lint ya
  // no forma parte de `next build`: corre aparte, y CI lo ejecuta como paso
  // propio antes del build.
  typescript: { ignoreBuildErrors: false },

  // Silencia la advertencia de raíz cuando se instala desde el monorepo.
  outputFileTracingRoot: __dirname,

  async headers() {
    return [
      {
        source: "/:path*",
        headers: [
          { key: "X-Content-Type-Options", value: "nosniff" },
          { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
          { key: "X-Frame-Options", value: "DENY" },
          {
            key: "Permissions-Policy",
            value: "camera=(), microphone=(), geolocation=()",
          },
        ],
      },
    ];
  },
};

export default nextConfig;
