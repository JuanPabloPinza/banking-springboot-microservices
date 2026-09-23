package git.juanpablopinza.accounts.application.port.in;

import git.juanpablopinza.accounts.domain.model.TipoCuenta;
import git.juanpablopinza.accounts.domain.model.TipoMovimiento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record EstadoCuentaReporte(ClienteReporte cliente, LocalDate fechaInicio, LocalDate fechaFin,
		List<CuentaReporte> cuentas) {

	public record ClienteReporte(UUID clienteId, String nombre) {
	}

	public record CuentaReporte(String numeroCuenta, TipoCuenta tipoCuenta, boolean estado, BigDecimal saldoInicial,
			BigDecimal saldoDisponible, BigDecimal totalCreditos, BigDecimal totalDebitos,
			List<MovimientoReporte> movimientos) {
	}

	public record MovimientoReporte(LocalDateTime fecha, TipoMovimiento tipoMovimiento, BigDecimal valor,
			BigDecimal saldo) {
	}
}
