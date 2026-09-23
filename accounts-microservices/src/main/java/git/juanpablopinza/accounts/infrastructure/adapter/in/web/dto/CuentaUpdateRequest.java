package git.juanpablopinza.accounts.infrastructure.adapter.in.web.dto;

import git.juanpablopinza.accounts.domain.model.TipoCuenta;
import jakarta.validation.constraints.NotNull;

public record CuentaUpdateRequest(
		@NotNull(message = "El tipo de cuenta es obligatorio (AHORROS o CORRIENTE)")
		TipoCuenta tipoCuenta,

		@NotNull(message = "El estado es obligatorio")
		Boolean estado) {
}
