package git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.entity;

import git.juanpablopinza.accounts.domain.model.TipoMovimiento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimiento")
@Getter
@Setter
public class MovimientoEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, updatable = false)
	private LocalDateTime fecha;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_movimiento", nullable = false, length = 20)
	private TipoMovimiento tipoMovimiento;

	@Column(nullable = false, precision = 15, scale = 2)
	private BigDecimal valor;

	@Column(nullable = false, precision = 15, scale = 2)
	private BigDecimal saldo;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cuenta_id", nullable = false, updatable = false)
	private CuentaEntity cuenta;
}
