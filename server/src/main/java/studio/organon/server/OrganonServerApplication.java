package studio.organon.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import studio.organon.server.config.CloudDatabaseUrl;

@SpringBootApplication
public class OrganonServerApplication {

	public static void main(String[] args) {
		// Antes de arrancar: los proveedores cloud inyectan DATABASE_URL en el
		// formato de libpq, que el driver JDBC no entiende. En local no hace
		// nada, porque esa variable no existe.
		CloudDatabaseUrl.applyIfPresent();
		SpringApplication.run(OrganonServerApplication.class, args);
	}

}
