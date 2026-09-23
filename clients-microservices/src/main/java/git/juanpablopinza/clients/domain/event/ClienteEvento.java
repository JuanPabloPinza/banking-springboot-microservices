package git.juanpablopinza.clients.domain.event;

import git.juanpablopinza.clients.domain.model.Cliente;

import java.time.Instant;
import java.util.UUID;

public record ClienteEvento(UUID eventoId, TipoEventoCliente tipo, UUID clienteId, String nombre,
		boolean estado, Instant ocurridoEn) {

	public static ClienteEvento de(TipoEventoCliente tipo, Cliente cliente, Instant ocurridoEn) {
		return new ClienteEvento(UUID.randomUUID(), tipo, cliente.getClienteId(), cliente.getNombre(),
				cliente.isEstado(), ocurridoEn);
	}
}
