package git.juanpablopinza.accounts.infrastructure.adapter.in.web.dto;

import git.juanpablopinza.accounts.domain.model.TipoMovimiento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoResponse(Long id, LocalDateTime fecha, TipoMovimiento tipoMovimiento, BigDecimal valor,
		BigDecimal saldo, String numeroCuenta) {
}
