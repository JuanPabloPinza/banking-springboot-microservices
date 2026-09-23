package git.juanpablopinza.clients.infrastructure.adapter.out.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Contrato JSON del evento publicado en RabbitMQ. Separado del evento de dominio para que el dominio
 * pueda cambiar sin romper a los consumidores.
 */
public record ClienteEventoMessage(UUID eventoId, String tipo, UUID clienteId, String nombre, boolean estado,
		Instant ocurridoEn) {
}
