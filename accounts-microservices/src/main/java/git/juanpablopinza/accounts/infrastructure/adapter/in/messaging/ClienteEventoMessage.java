package git.juanpablopinza.accounts.infrastructure.adapter.in.messaging;

import java.time.Instant;
import java.util.UUID;

public record ClienteEventoMessage(UUID eventoId, String tipo, UUID clienteId, String nombre, boolean estado,
		Instant ocurridoEn) {
}
