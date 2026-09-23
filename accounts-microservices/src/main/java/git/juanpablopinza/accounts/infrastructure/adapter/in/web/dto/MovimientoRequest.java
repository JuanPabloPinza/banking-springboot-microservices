package git.juanpablopinza.accounts.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MovimientoRequest(
		@NotBlank(message = "El número de cuenta es obligatorio")
		String numeroCuenta,

		@NotNull(message = "El valor es obligatorio")
		@Digits(integer = 13, fraction = 2, message = "El valor admite máximo 2 decimales")
		BigDecimal valor) {
}
