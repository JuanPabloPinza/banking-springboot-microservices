package git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitud_idempotente")
@Getter
@Setter
public class SolicitudIdempotenteEntity implements Persistable<String> {

	@Id
	@Column(name = "idempotency_key", length = 64)
	private String idempotencyKey;

	@Column(name = "numero_cuenta", nullable = false, length = 20)
	private String numeroCuenta;

	@Column(nullable = false, precision = 15, scale = 2)
	private BigDecimal valor;

	@Column(name = "movimiento_id")
	private Long movimientoId;

	@Column(name = "creado_en", nullable = false)
	private LocalDateTime creadoEn;

	@Override
	public String getId() {
		return idempotencyKey;
	}

	@Override
	public boolean isNew() {
		return true;
	}
}
