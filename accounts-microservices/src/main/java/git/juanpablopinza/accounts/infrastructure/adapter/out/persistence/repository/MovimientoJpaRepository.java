package git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.repository;

import git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.entity.MovimientoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MovimientoJpaRepository extends JpaRepository<MovimientoEntity, Long> {

	@EntityGraph(attributePaths = "cuenta")
	Optional<MovimientoEntity> findWithCuentaById(Long id);

	@EntityGraph(attributePaths = "cuenta")
	Optional<MovimientoEntity> findFirstByCuentaNumeroCuentaOrderByIdDesc(String numeroCuenta);

	@EntityGraph(attributePaths = "cuenta")
	Optional<MovimientoEntity> findByIdempotencyKey(String idempotencyKey);

	@EntityGraph(attributePaths = "cuenta")
	Page<MovimientoEntity> findByCuentaNumeroCuenta(String numeroCuenta, Pageable pageable);

	@Override
	@EntityGraph(attributePaths = "cuenta")
	Page<MovimientoEntity> findAll(Pageable pageable);
}
