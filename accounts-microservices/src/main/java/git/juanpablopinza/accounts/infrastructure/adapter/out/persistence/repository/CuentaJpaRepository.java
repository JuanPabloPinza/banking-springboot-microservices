package git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.repository;

import git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.entity.CuentaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CuentaJpaRepository extends JpaRepository<CuentaEntity, Long> {

	Optional<CuentaEntity> findByNumeroCuenta(String numeroCuenta);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from CuentaEntity c where c.numeroCuenta = :numeroCuenta")
	Optional<CuentaEntity> findByNumeroCuentaParaActualizar(@Param("numeroCuenta") String numeroCuenta);

	boolean existsByNumeroCuenta(String numeroCuenta);

	Page<CuentaEntity> findByClienteId(UUID clienteId, Pageable pageable);

	List<CuentaEntity> findByClienteIdOrderByNumeroCuenta(UUID clienteId);

	@Modifying
	@Query("update CuentaEntity c set c.estado = false where c.clienteId = :clienteId and c.estado = true")
	int desactivarPorCliente(@Param("clienteId") UUID clienteId);
}
