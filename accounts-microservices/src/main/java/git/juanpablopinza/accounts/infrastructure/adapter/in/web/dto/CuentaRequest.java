package git.juanpablopinza.accounts.infrastructure.adapter.in.web.dto;

import git.juanpablopinza.accounts.domain.model.TipoCuenta;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.UUID;

public record CuentaRequest(
		@NotBlank(message = "El número de cuenta es obligatorio")
		@Pattern(regexp = "^\\d{6,20}$", message = "El número de cuenta debe tener entre 6 y 20 dígitos")
		String numeroCuenta,

		@NotNull(message = "El tipo de cuenta es obligatorio (AHORROS o CORRIENTE)")
		TipoCuenta tipoCuenta,

		@NotNull(message = "El saldo inicial es obligatorio")
		@DecimalMin(value = "0.00", message = "El saldo inicial no puede ser negativo")
		@Digits(integer = 13, fraction = 2, message = "El saldo inicial admite máximo 2 decimales")
		BigDecimal saldoInicial,

		Boolean estado,

		@NotNull(message = "El clienteId es obligatorio")
		UUID clienteId) {
}
