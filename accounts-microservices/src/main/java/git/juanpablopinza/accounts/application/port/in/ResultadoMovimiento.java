package git.juanpablopinza.accounts.application.port.in;

import git.juanpablopinza.accounts.domain.model.Movimiento;

public record ResultadoMovimiento(Movimiento movimiento, boolean repetido) {
}
