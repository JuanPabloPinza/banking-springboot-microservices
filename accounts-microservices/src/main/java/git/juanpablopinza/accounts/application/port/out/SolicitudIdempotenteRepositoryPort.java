package git.juanpablopinza.accounts.application.port.out;

import git.juanpablopinza.accounts.domain.model.SolicitudIdempotente;

import java.util.Optional;

public interface SolicitudIdempotenteRepositoryPort {

	Optional<SolicitudIdempotente> buscar(String idempotencyKey);

	void crear(SolicitudIdempotente solicitud);
}
