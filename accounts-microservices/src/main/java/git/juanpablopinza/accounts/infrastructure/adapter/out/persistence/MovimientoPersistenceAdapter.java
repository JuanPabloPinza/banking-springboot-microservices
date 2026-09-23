package git.juanpablopinza.accounts.infrastructure.adapter.out.persistence;

import git.juanpablopinza.accounts.application.port.Pagina;
import git.juanpablopinza.accounts.application.port.out.MovimientoRepositoryPort;
import git.juanpablopinza.accounts.domain.model.Movimiento;
import git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.entity.MovimientoEntity;
import git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.mapper.CuentaPersistenceMapper;
import git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.repository.CuentaJpaRepository;
import git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.repository.MovimientoJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MovimientoPersistenceAdapter implements MovimientoRepositoryPort {

	private final MovimientoJpaRepository repository;
	private final CuentaJpaRepository cuentaRepository;
	private final CuentaPersistenceMapper mapper;

	@Override
	public Movimiento guardar(Movimiento movimiento) {
		MovimientoEntity entity = mapper.toEntity(movimiento);
		entity.setCuenta(cuentaRepository.findByNumeroCuenta(movimiento.getNumeroCuenta()).orElseThrow());
		return mapper.toDomain(repository.save(entity));
	}

	@Override
	public Optional<Movimiento> buscarPorId(Long id) {
		return repository.findWithCuentaById(id).map(mapper::toDomain);
	}

	@Override
	public Optional<Movimiento> buscarUltimo(String numeroCuenta) {
		return repository.findFirstByCuentaNumeroCuentaOrderByIdDesc(numeroCuenta).map(mapper::toDomain);
	}

	@Override
	public Optional<Movimiento> buscarPorIdempotencyKey(String idempotencyKey) {
		return repository.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
	}

	@Override
	public Pagina<Movimiento> listar(String numeroCuenta, int pagina, int tamanio) {
		Pageable pageable = PageRequest.of(pagina, tamanio, Sort.by(Sort.Direction.DESC, "id"));
		Page<MovimientoEntity> page = numeroCuenta == null
				? repository.findAll(pageable)
				: repository.findByCuentaNumeroCuenta(numeroCuenta, pageable);
		return new Pagina<>(page.map(mapper::toDomain).getContent(), page.getNumber(), page.getSize(),
				page.getTotalElements(), page.getTotalPages());
	}

	@Override
	public void eliminar(Long id) {
		repository.deleteById(id);
	}
}
