package git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.repository;

import git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.entity.ClienteRefEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ClienteRefJpaRepository extends JpaRepository<ClienteRefEntity, UUID> {

	@Lock(LockModeType.PESSIMISTIC_READ)
	@Query("select c from ClienteRefEntity c where c.clienteId = :clienteId")
	Optional<ClienteRefEntity> findConBloqueoCompartido(@Param("clienteId") UUID clienteId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from ClienteRefEntity c where c.clienteId = :clienteId")
	Optional<ClienteRefEntity> findParaActualizar(@Param("clienteId") UUID clienteId);
}
