package git.juanpablopinza.accounts.application.port.in;

import git.juanpablopinza.accounts.domain.model.TipoCuenta;

import java.math.BigDecimal;
import java.util.UUID;

public record CrearCuentaCommand(String numeroCuenta, TipoCuenta tipoCuenta, BigDecimal saldoInicial, Boolean estado,
		UUID clienteId) {
}
