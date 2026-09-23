package git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.entity;

import git.juanpablopinza.accounts.domain.model.TipoCuenta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "cuenta")
@Getter
@Setter
public class CuentaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "numero_cuenta", nullable = false, unique = true, length = 20, updatable = false)
	private String numeroCuenta;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_cuenta", nullable = false, length = 20)
	private TipoCuenta tipoCuenta;

	@Column(name = "saldo_inicial", nullable = false, precision = 15, scale = 2, updatable = false)
	private BigDecimal saldoInicial;

	@Column(name = "saldo_disponible", nullable = false, precision = 15, scale = 2)
	private BigDecimal saldoDisponible;

	@Column(nullable = false)
	private boolean estado;

	@Column(name = "cliente_id", nullable = false, updatable = false)
	private UUID clienteId;
}
