package studio.organon.server.backup;

import java.util.List;

/**
 * La copia no se puede restaurar. Se lanza antes de escribir nada, asi que el
 * cuaderno queda intacto. Mensaje y problemas van redactados para mostrarse tal
 * cual en la interfaz.
 */
public class InvalidBackupException extends RuntimeException {

    private final List<String> problems;

    public InvalidBackupException(String message) {
        this(message, List.of());
    }

    public InvalidBackupException(String message, List<String> problems) {
        super(message);
        this.problems = List.copyOf(problems);
    }

    public List<String> getProblems() {
        return problems;
    }
}
