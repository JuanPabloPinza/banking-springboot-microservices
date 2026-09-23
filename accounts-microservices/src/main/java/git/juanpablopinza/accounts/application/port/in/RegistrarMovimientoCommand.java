package git.juanpablopinza.accounts.application.port.in;

import java.math.BigDecimal;

public record RegistrarMovimientoCommand(String numeroCuenta, BigDecimal valor, String idempotencyKey) {
}
