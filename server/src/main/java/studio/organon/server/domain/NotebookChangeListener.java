package studio.organon.server.domain;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Anuncia {@link NotebookChanged} cuando cambia cualquier entidad, pero solo si
 * la transaccion se confirma: reaccionar a un cambio que luego se deshace
 * dejaria, por ejemplo, vectores de busqueda de algo que no existe.
 *
 * <p>Esta registrado en {@link BaseEntity}, asi que cubre cualquier escritura
 * —formularios, semilla, restauracion de copias— sin que cada servicio tenga
 * que acordarse de avisar. Una transaccion que toca muchas entidades anuncia una
 * sola vez. Hibernate lo obtiene del contenedor de Spring, que es quien le
 * inyecta el publicador.
 */
@Component
public class NotebookChangeListener {

    /** Marca, ligada a la transaccion en curso, de que el aviso ya esta programado. */
    private static final Object AVISO_PROGRAMADO = new Object();

    private final ApplicationEventPublisher eventos;

    public NotebookChangeListener(ApplicationEventPublisher eventos) {
        this.eventos = eventos;
    }

    @PostPersist
    @PostUpdate
    @PostRemove
    void alCambiar(Object entidad) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            eventos.publishEvent(new NotebookChanged());
            return;
        }
        if (TransactionSynchronizationManager.hasResource(AVISO_PROGRAMADO)) {
            return;
        }
        TransactionSynchronizationManager.bindResource(AVISO_PROGRAMADO, Boolean.TRUE);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventos.publishEvent(new NotebookChanged());
            }

            @Override
            public void afterCompletion(int estado) {
                TransactionSynchronizationManager.unbindResourceIfPossible(AVISO_PROGRAMADO);
            }
        });
    }
}
