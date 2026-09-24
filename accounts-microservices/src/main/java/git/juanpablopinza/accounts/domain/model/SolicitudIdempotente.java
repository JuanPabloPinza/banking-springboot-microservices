package git.juanpablopinza.accounts.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SolicitudIdempotente(String idempotencyKey, String numeroCuenta, BigDecimal valor, Long movimientoId,
		LocalDateTime creadoEn) {

	public boolean esMismaSolicitud(String numeroCuenta, BigDecimal valor) {
		return this.numeroCuenta.equals(numeroCuenta) && this.valor.compareTo(valor) == 0;
	}

	public boolean fueAnulada() {
		return movimientoId == null;
	}
}
