package git.juanpablopinza.accounts.infrastructure.adapter.out.persistence;

import git.juanpablopinza.accounts.application.port.out.ClienteRefRepositoryPort;
import git.juanpablopinza.accounts.domain.model.ClienteRef;
import git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.mapper.CuentaPersistenceMapper;
import git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.repository.ClienteRefJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ClienteRefPersistenceAdapter implements ClienteRefRepositoryPort {

	private final ClienteRefJpaRepository repository;
	private final CuentaPersistenceMapper mapper;

	@Override
	public Optional<ClienteRef> buscar(UUID clienteId) {
		return repository.findById(clienteId).map(mapper::toDomain);
	}

	@Override
	public Optional<ClienteRef> buscarConBloqueoCompartido(UUID clienteId) {
		return repository.findConBloqueoCompartido(clienteId).map(mapper::toDomain);
	}

	@Override
	public Optional<ClienteRef> buscarParaActualizar(UUID clienteId) {
		return repository.findParaActualizar(clienteId).map(mapper::toDomain);
	}

	@Override
	public void guardar(ClienteRef clienteRef) {
		repository.save(mapper.toEntity(clienteRef));
	}
}
