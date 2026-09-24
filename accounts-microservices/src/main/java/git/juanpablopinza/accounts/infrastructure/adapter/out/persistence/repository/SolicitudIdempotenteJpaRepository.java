package git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.repository;

import git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.entity.SolicitudIdempotenteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudIdempotenteJpaRepository extends JpaRepository<SolicitudIdempotenteEntity, String> {
}
