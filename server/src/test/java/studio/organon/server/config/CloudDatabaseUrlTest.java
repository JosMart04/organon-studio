package studio.organon.server.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Si esta traduccion falla, el despliegue muere al arrancar con un mensaje que
 * no dice nada del problema real. Merece la pena fijarla.
 */
class CloudDatabaseUrlTest {

    @Test
    @DisplayName("Convierte la URL de Neon a JDBC conservando usuario, clave y TLS")
    void convierteUrlDeNeon() {
        var parsed = CloudDatabaseUrl.parse(
                "postgres://organon:s3cr3t@ep-cool-1.eu-central-1.aws.neon.tech/organon_db?sslmode=require")
                .orElseThrow();

        assertThat(parsed.jdbcUrl())
                .isEqualTo("jdbc:postgresql://ep-cool-1.eu-central-1.aws.neon.tech:5432/organon_db?sslmode=require");
        assertThat(parsed.username()).isEqualTo("organon");
        assertThat(parsed.password()).isEqualTo("s3cr3t");
    }

    @Test
    @DisplayName("Respeta el puerto explicito")
    void respetaPuertoExplicito() {
        var parsed = CloudDatabaseUrl.parse("postgresql://u:p@db.interno:6543/organon").orElseThrow();

        assertThat(parsed.jdbcUrl()).startsWith("jdbc:postgresql://db.interno:6543/organon");
    }

    @Test
    @DisplayName("Impone TLS cuando el proveedor no lo declara")
    void imponeTlsSiFalta() {
        var parsed = CloudDatabaseUrl.parse("postgres://u:p@host/base").orElseThrow();

        assertThat(parsed.jdbcUrl()).endsWith("?sslmode=require");
    }

    @Test
    @DisplayName("No pisa otros parametros de la cadena")
    void conservaOtrosParametros() {
        var parsed = CloudDatabaseUrl.parse(
                "postgres://u:p@host/base?application_name=organon").orElseThrow();

        assertThat(parsed.jdbcUrl()).contains("application_name=organon").endsWith("&sslmode=require");
    }

    @Test
    @DisplayName("Descodifica las contrasenas con caracteres escapados")
    void descodificaContrasena() {
        var parsed = CloudDatabaseUrl.parse("postgres://u:cla%40ve%3Arara@host/base").orElseThrow();

        assertThat(parsed.password()).isEqualTo("cla@ve:rara");
    }

    @Test
    @DisplayName("Una cadena que no es una URL no revienta el arranque")
    void toleraBasura() {
        assertThat(CloudDatabaseUrl.parse("esto no es una url")).isEmpty();
    }
}
