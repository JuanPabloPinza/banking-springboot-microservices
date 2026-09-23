package git.juanpablopinza.clients.infrastructure.adapter.out.persistence.repository;

import git.juanpablopinza.clients.infrastructure.adapter.out.persistence.entity.ClienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClienteJpaRepository extends JpaRepository<ClienteEntity, Long> {

	Optional<ClienteEntity> findByClienteId(UUID clienteId);

	boolean existsByIdentificacion(String identificacion);
}
