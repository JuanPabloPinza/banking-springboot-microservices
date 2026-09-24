package git.juanpablopinza.accounts.application.port.out;

import git.juanpablopinza.accounts.domain.model.ClienteRef;

import java.util.Optional;
import java.util.UUID;

public interface ClienteRefRepositoryPort {

	Optional<ClienteRef> buscar(UUID clienteId);

	Optional<ClienteRef> buscarConBloqueoCompartido(UUID clienteId);

	Optional<ClienteRef> buscarParaActualizar(UUID clienteId);

	void guardar(ClienteRef clienteRef);
}
