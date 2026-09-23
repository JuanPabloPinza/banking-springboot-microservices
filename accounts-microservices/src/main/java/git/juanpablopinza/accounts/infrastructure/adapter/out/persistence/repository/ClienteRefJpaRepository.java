package git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.repository;

import git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.entity.ClienteRefEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClienteRefJpaRepository extends JpaRepository<ClienteRefEntity, UUID> {
}
