package git.juanpablopinza.accounts.application.port.in;

import java.time.LocalDate;
import java.util.UUID;

public interface ReporteUseCase {

	EstadoCuentaReporte generar(UUID clienteId, LocalDate desde, LocalDate hasta);
}
