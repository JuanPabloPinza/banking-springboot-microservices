package git.juanpablopinza.accounts.infrastructure.adapter.in.web;

import git.juanpablopinza.accounts.application.port.in.EstadoCuentaReporte;
import git.juanpablopinza.accounts.application.port.in.EstadoCuentaReporte.ClienteReporte;
import git.juanpablopinza.accounts.application.port.in.EstadoCuentaReporte.CuentaReporte;
import git.juanpablopinza.accounts.application.port.in.EstadoCuentaReporte.MovimientoReporte;
import git.juanpablopinza.accounts.application.port.in.ReporteUseCase;
import git.juanpablopinza.accounts.domain.exception.DatoInvalidoException;
import git.juanpablopinza.accounts.domain.model.TipoCuenta;
import git.juanpablopinza.accounts.domain.model.TipoMovimiento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReporteController.class)
class ReporteControllerTest {

	private static final UUID CLIENTE = UUID.fromString("7f0f7a3e-4b6e-4c55-9d8a-2a1f3c0b9e11");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ReporteUseCase reporteUseCase;

	@Test
	@DisplayName("GET /api/reportes convierte fecha=desde,hasta y devuelve el estado de cuenta")
	void estadoDeCuenta() throws Exception {
		LocalDate desde = LocalDate.of(2026, 9, 1);
		LocalDate hasta = LocalDate.of(2026, 9, 30);
		when(reporteUseCase.generar(CLIENTE, desde, hasta)).thenReturn(new EstadoCuentaReporte(
				new ClienteReporte(CLIENTE, "Marianela Montalvo"), desde, hasta,
				List.of(new CuentaReporte("225487", TipoCuenta.CORRIENTE, true, new BigDecimal("100.00"),
						new BigDecimal("700.00"), new BigDecimal("600.00"), new BigDecimal("0.00"),
						List.of(new MovimientoReporte(LocalDateTime.of(2026, 9, 23, 10, 15, 30),
								TipoMovimiento.DEPOSITO, new BigDecimal("600.00"), new BigDecimal("700.00")))))));

		mockMvc.perform(get("/api/reportes").param("cliente", CLIENTE.toString()).param("fecha", "2026-09-01,2026-09-30"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cliente.nombre").value("Marianela Montalvo"))
				.andExpect(jsonPath("$.fechaInicio").value("2026-09-01"))
				.andExpect(jsonPath("$.cuentas[0].saldoDisponible").value(700.0))
				.andExpect(jsonPath("$.cuentas[0].movimientos[0].fecha").value("2026-09-23T10:15:30"))
				.andExpect(jsonPath("$.cuentas[0].movimientos[0].tipoMovimiento").value("DEPOSITO"));
	}

	@Test
	@DisplayName("Con una sola fecha responde 400 y no llega al caso de uso")
	void unaSolaFecha() throws Exception {
		mockMvc.perform(get("/api/reportes").param("cliente", CLIENTE.toString()).param("fecha", "2026-09-01"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"))
				.andExpect(jsonPath("$.errores[0].campo").value("fecha"));
		verifyNoInteractions(reporteUseCase);
	}

	@Test
	@DisplayName("Un rango inválido del caso de uso se traduce en 400 con código DATO_INVALIDO")
	void rangoInvalido() throws Exception {
		when(reporteUseCase.generar(any(), any(), any()))
				.thenThrow(new DatoInvalidoException("La fecha inicial no puede ser posterior a la fecha final"));

		mockMvc.perform(get("/api/reportes").param("cliente", CLIENTE.toString()).param("fecha", "2026-09-30,2026-09-01"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("DATO_INVALIDO"));
	}

	@Test
	@DisplayName("Una fecha con formato inválido responde 400")
	void fechaMalFormada() throws Exception {
		mockMvc.perform(get("/api/reportes").param("cliente", CLIENTE.toString()).param("fecha", "01/09/2026,2026-09-30"))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(reporteUseCase);
	}
}
