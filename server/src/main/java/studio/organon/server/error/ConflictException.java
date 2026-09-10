package studio.organon.server.error;

/** Peticion coherente en si misma pero incompatible con el estado del corpus. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
