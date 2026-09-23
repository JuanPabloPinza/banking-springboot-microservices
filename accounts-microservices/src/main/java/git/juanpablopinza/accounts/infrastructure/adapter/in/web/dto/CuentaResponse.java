package git.juanpablopinza.accounts.infrastructure.adapter.in.web.dto;

import git.juanpablopinza.accounts.domain.model.TipoCuenta;

import java.math.BigDecimal;
import java.util.UUID;

public record CuentaResponse(String numeroCuenta, TipoCuenta tipoCuenta, BigDecimal saldoInicial,
		BigDecimal saldoDisponible, boolean estado, UUID clienteId) {
}
