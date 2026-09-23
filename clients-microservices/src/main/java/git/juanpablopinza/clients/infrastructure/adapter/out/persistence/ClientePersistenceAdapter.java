package git.juanpablopinza.clients.infrastructure.adapter.out.persistence;

import git.juanpablopinza.clients.application.port.Pagina;
import git.juanpablopinza.clients.application.port.out.ClienteRepositoryPort;
import git.juanpablopinza.clients.domain.model.Cliente;
import git.juanpablopinza.clients.infrastructure.adapter.out.persistence.entity.ClienteEntity;
import git.juanpablopinza.clients.infrastructure.adapter.out.persistence.mapper.ClientePersistenceMapper;
import git.juanpablopinza.clients.infrastructure.adapter.out.persistence.repository.ClienteJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ClientePersistenceAdapter implements ClienteRepositoryPort {

	private final ClienteJpaRepository repository;
	private final ClientePersistenceMapper mapper;

	@Override
	public Cliente guardar(Cliente cliente) {
		ClienteEntity entity = repository.findByClienteId(cliente.getClienteId())
				.map(existente -> {
					mapper.actualizar(cliente, existente);
					return existente;
				})
				.orElseGet(() -> mapper.toEntity(cliente));
		return mapper.toDomain(repository.save(entity));
	}

	@Override
	public Optional<Cliente> buscarPorClienteId(UUID clienteId) {
		return repository.findByClienteId(clienteId).map(mapper::toDomain);
	}

	@Override
	public boolean existePorIdentificacion(String identificacion) {
		return repository.existsByIdentificacion(identificacion);
	}

	@Override
	public Pagina<Cliente> listar(int pagina, int tamanio) {
		Page<Cliente> page = repository.findAll(PageRequest.of(pagina, tamanio, Sort.by("id")))
				.map(mapper::toDomain);
		return new Pagina<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
				page.getTotalPages());
	}
}
