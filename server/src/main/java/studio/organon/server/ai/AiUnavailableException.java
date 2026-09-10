package studio.organon.server.ai;

/**
 * El asistente no esta disponible. Nunca es un error del lector, asi que el
 * mensaje va escrito para mostrarse tal cual en la interfaz.
 */
public class AiUnavailableException extends RuntimeException {

    public AiUnavailableException(String message) {
        super(message);
    }

    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
