package git.juanpablopinza.accounts.domain.model;

import java.math.BigDecimal;

public enum TipoMovimiento {
	DEPOSITO,
	RETIRO;

	public static TipoMovimiento segun(BigDecimal valor) {
		return valor.signum() > 0 ? DEPOSITO : RETIRO;
	}
}
