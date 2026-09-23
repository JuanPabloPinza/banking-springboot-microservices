package git.juanpablopinza.accounts.infrastructure.adapter.out.persistence;

import git.juanpablopinza.accounts.application.port.Pagina;
import git.juanpablopinza.accounts.application.port.out.CuentaRepositoryPort;
import git.juanpablopinza.accounts.domain.model.Cuenta;
import git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.entity.CuentaEntity;
import git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.mapper.CuentaPersistenceMapper;
import git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.repository.CuentaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CuentaPersistenceAdapter implements CuentaRepositoryPort {

	private final CuentaJpaRepository repository;
	private final CuentaPersistenceMapper mapper;

	@Override
	public Cuenta guardar(Cuenta cuenta) {
		CuentaEntity entity = repository.findByNumeroCuenta(cuenta.getNumeroCuenta())
				.map(existente -> {
					mapper.actualizar(cuenta, existente);
					return existente;
				})
				.orElseGet(() -> mapper.toEntity(cuenta));
		return mapper.toDomain(repository.save(entity));
	}

	@Override
	public Optional<Cuenta> buscarPorNumero(String numeroCuenta) {
		return repository.findByNumeroCuenta(numeroCuenta).map(mapper::toDomain);
	}

	@Override
	public Optional<Cuenta> buscarPorNumeroParaActualizar(String numeroCuenta) {
		return repository.findByNumeroCuentaParaActualizar(numeroCuenta).map(mapper::toDomain);
	}

	@Override
	public boolean existePorNumero(String numeroCuenta) {
		return repository.existsByNumeroCuenta(numeroCuenta);
	}

	@Override
	public Pagina<Cuenta> listar(UUID clienteId, int pagina, int tamanio) {
		Pageable pageable = PageRequest.of(pagina, tamanio, Sort.by("numeroCuenta"));
		Page<CuentaEntity> page = clienteId == null
				? repository.findAll(pageable)
				: repository.findByClienteId(clienteId, pageable);
		return new Pagina<>(page.map(mapper::toDomain).getContent(), page.getNumber(), page.getSize(),
				page.getTotalElements(), page.getTotalPages());
	}

	@Override
	public List<Cuenta> listarPorCliente(UUID clienteId) {
		return repository.findByClienteIdOrderByNumeroCuenta(clienteId).stream().map(mapper::toDomain).toList();
	}

	@Override
	public int desactivarPorCliente(UUID clienteId) {
		return repository.desactivarPorCliente(clienteId);
	}
}
