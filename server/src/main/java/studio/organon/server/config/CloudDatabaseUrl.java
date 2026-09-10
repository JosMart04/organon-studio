package studio.organon.server.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.util.Optional;

/**
 * Traduce la variable {@code DATABASE_URL} que inyectan Render, Railway, Neon y
 * Supabase al trio de propiedades que espera Spring.
 *
 * <p>Esos proveedores usan la forma que entiende {@code libpq}:
 * {@code postgres://usuario:clave@host:5432/base?sslmode=require}. El driver
 * JDBC no la reconoce — necesita {@code jdbc:postgresql://host:5432/base} con
 * usuario y contrasena aparte —, asi que sin esta conversion el despliegue
 * falla nada mas arrancar con un "No suitable driver".
 *
 * <p>Se invoca desde {@code main} y no como {@code EnvironmentPostProcessor}
 * porque el datasource se configura muy pronto y porque un metodo estatico
 * explicito es mas facil de seguir que un gancho registrado por descubrimiento.
 */
public final class CloudDatabaseUrl {

    private static final String ENV_VAR = "DATABASE_URL";

    private CloudDatabaseUrl() {
    }

    /**
     * Si existe {@code DATABASE_URL} en formato postgres:// y no se han fijado
     * ya las propiedades de Spring, las deriva de ella. No pisa nada configurado
     * a mano: en local mandan las de {@code server/.env}.
     */
    public static void applyIfPresent() {
        applyIfPresent(System.getenv(ENV_VAR));
    }

    static void applyIfPresent(String databaseUrl) {
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }
        if (databaseUrl.startsWith("jdbc:")) {
            // Ya viene en formato JDBC: se usa tal cual.
            setIfAbsent("spring.datasource.url", databaseUrl);
            return;
        }
        parse(databaseUrl).ifPresent(parsed -> {
            setIfAbsent("spring.datasource.url", parsed.jdbcUrl());
            if (parsed.username() != null) {
                setIfAbsent("spring.datasource.username", parsed.username());
            }
            if (parsed.password() != null) {
                setIfAbsent("spring.datasource.password", parsed.password());
            }
        });
    }

    record Parsed(String jdbcUrl, String username, String password) {
    }

    static Optional<Parsed> parse(String databaseUrl) {
        final URI uri;
        try {
            uri = new URI(databaseUrl.trim());
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
        if (uri.getHost() == null) {
            return Optional.empty();
        }

        int port = uri.getPort() > 0 ? uri.getPort() : 5432;
        String database = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");

        StringBuilder jdbc = new StringBuilder("jdbc:postgresql://")
                .append(uri.getHost()).append(':').append(port).append('/').append(database);

        // Los tiers gestionados exigen TLS; si la URL no lo dice, se impone.
        String query = uri.getQuery();
        if (query == null || query.isBlank()) {
            jdbc.append("?sslmode=require");
        } else {
            jdbc.append('?').append(query);
            if (!query.contains("sslmode=")) {
                jdbc.append("&sslmode=require");
            }
        }

        String username = null;
        String password = null;
        String userInfo = uri.getUserInfo();
        if (userInfo != null && !userInfo.isBlank()) {
            int separator = userInfo.indexOf(':');
            if (separator < 0) {
                username = decode(userInfo);
            } else {
                username = decode(userInfo.substring(0, separator));
                password = decode(userInfo.substring(separator + 1));
            }
        }

        return Optional.of(new Parsed(jdbc.toString(), username, password));
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static void setIfAbsent(String key, String value) {
        if (System.getProperty(key) == null) {
            System.setProperty(key, value);
        }
    }
}
