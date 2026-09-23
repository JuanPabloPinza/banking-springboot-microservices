package git.juanpablopinza.accounts.application.port.in;

import java.time.Instant;
import java.util.UUID;

public record SincronizarClienteCommand(UUID clienteId, String nombre, boolean estado, Instant ocurridoEn) {
}
