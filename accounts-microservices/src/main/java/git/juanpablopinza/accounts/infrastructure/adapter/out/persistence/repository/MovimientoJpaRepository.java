package git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.repository;

import git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.entity.MovimientoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MovimientoJpaRepository extends JpaRepository<MovimientoEntity, Long> {

	@EntityGraph(attributePaths = "cuenta")
	Optional<MovimientoEntity> findWithCuentaById(Long id);

	@Query("select m.cuenta.numeroCuenta from MovimientoEntity m where m.id = :id")
	Optional<String> findNumeroCuentaById(@Param("id") Long id);

	@EntityGraph(attributePaths = "cuenta")
	Optional<MovimientoEntity> findFirstByCuentaNumeroCuentaOrderByIdDesc(String numeroCuenta);

	@EntityGraph(attributePaths = "cuenta")
	Page<MovimientoEntity> findByCuentaNumeroCuenta(String numeroCuenta, Pageable pageable);

	@Override
	@EntityGraph(attributePaths = "cuenta")
	Page<MovimientoEntity> findAll(Pageable pageable);

	@Query("""
			select m from MovimientoEntity m join fetch m.cuenta c
			where c.clienteId = :clienteId and m.fecha >= :desde and m.fecha < :hasta
			order by m.id""")
	List<MovimientoEntity> findPorClienteEntre(@Param("clienteId") UUID clienteId,
			@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hastaExclusivo);

	@Query("""
			select m from MovimientoEntity m join fetch m.cuenta c
			where m.id in (
				select max(m2.id) from MovimientoEntity m2
				where m2.cuenta.clienteId = :clienteId and m2.fecha < :fecha
				group by m2.cuenta.id)""")
	List<MovimientoEntity> findUltimosPorClienteAntesDe(@Param("clienteId") UUID clienteId,
			@Param("fecha") LocalDateTime fecha);
}
