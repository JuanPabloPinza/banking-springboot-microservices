package git.juanpablopinza.accounts.application.port.in;

import git.juanpablopinza.accounts.domain.model.TipoCuenta;

public record ActualizarCuentaCommand(TipoCuenta tipoCuenta, boolean estado) {
}
