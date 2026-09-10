package studio.organon.server.error;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String entity, Object id) {
        super("No existe %s con id %s".formatted(entity, id));
    }

    public NotFoundException(String message) {
        super(message);
    }
}
