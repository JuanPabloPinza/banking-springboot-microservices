package git.juanpablopinza.accounts.infrastructure.adapter.in.web.dto;

import git.juanpablopinza.accounts.domain.model.TipoCuenta;

public record CuentaPatchRequest(TipoCuenta tipoCuenta, Boolean estado) {
}
