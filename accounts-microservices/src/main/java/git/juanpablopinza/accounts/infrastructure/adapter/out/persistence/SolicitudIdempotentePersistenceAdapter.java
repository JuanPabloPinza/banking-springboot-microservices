package git.juanpablopinza.accounts.infrastructure.adapter.out.persistence;

import git.juanpablopinza.accounts.application.port.out.SolicitudIdempotenteRepositoryPort;
import git.juanpablopinza.accounts.domain.exception.IdempotencyKeyReutilizadaException;
import git.juanpablopinza.accounts.domain.model.SolicitudIdempotente;
import git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.mapper.CuentaPersistenceMapper;
import git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.repository.SolicitudIdempotenteJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SolicitudIdempotentePersistenceAdapter implements SolicitudIdempotenteRepositoryPort {

	private final SolicitudIdempotenteJpaRepository repository;
	private final CuentaPersistenceMapper mapper;

	@Override
	public Optional<SolicitudIdempotente> buscar(String idempotencyKey) {
		return repository.findById(idempotencyKey).map(mapper::toDomain);
	}

	@Override
	public void crear(SolicitudIdempotente solicitud) {
		try {
			repository.saveAndFlush(mapper.toEntity(solicitud));
		} catch (DataIntegrityViolationException e) {
			if (Restricciones.esViolacionDe(e, "pk_solicitud_idempotente")) {
				throw new IdempotencyKeyReutilizadaException(solicitud.idempotencyKey());
			}
			throw e;
		}
	}
}
