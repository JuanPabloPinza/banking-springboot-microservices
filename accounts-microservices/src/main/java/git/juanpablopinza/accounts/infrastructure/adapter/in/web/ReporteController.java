package git.juanpablopinza.accounts.infrastructure.adapter.in.web;

import git.juanpablopinza.accounts.application.port.in.EstadoCuentaReporte;
import git.juanpablopinza.accounts.application.port.in.ReporteUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "Estado de cuenta (F4)")
public class ReporteController {

	private final ReporteUseCase reporteUseCase;

	@GetMapping
	@Operation(summary = "Estado de cuenta de un cliente en un rango de fechas",
			description = "Incluye todas las cuentas del cliente con sus saldos, totales y movimientos del rango. "
					+ "Ambas fechas son inclusivas y el rango admite máximo 366 días.")
	public EstadoCuentaReporte generar(
			@Parameter(description = "clienteId del cliente") @RequestParam UUID cliente,
			@Parameter(description = "Rango desde,hasta en formato yyyy-MM-dd", example = "2026-09-01,2026-09-30")
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
			@Size(min = 2, max = 2, message = "fecha debe tener el formato desde,hasta") List<LocalDate> fecha) {
		return reporteUseCase.generar(cliente, fecha.get(0), fecha.get(1));
	}
}
