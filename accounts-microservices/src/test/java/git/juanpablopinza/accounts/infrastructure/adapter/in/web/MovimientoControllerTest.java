package git.juanpablopinza.accounts.infrastructure.adapter.in.web;

import git.juanpablopinza.accounts.application.port.in.MovimientoUseCase;
import git.juanpablopinza.accounts.application.port.in.ResultadoMovimiento;
import git.juanpablopinza.accounts.domain.exception.SaldoNoDisponibleException;
import git.juanpablopinza.accounts.domain.model.Movimiento;
import git.juanpablopinza.accounts.domain.model.TipoMovimiento;
import git.juanpablopinza.accounts.infrastructure.adapter.in.web.mapper.CuentaWebMapperImpl;
import git.juanpablopinza.accounts.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MovimientoController.class)
@Import({CuentaWebMapperImpl.class, SecurityConfig.class})
class MovimientoControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MovimientoUseCase movimientoUseCase;

	private Movimiento deposito() {
		return new Movimiento(10L, "225487", LocalDateTime.of(2026, 9, 23, 10, 0), TipoMovimiento.DEPOSITO,
				new BigDecimal("600.00"), new BigDecimal("700.00"));
	}

	private static String cuerpo(String numeroCuenta, String valor) {
		return """
				{ "numeroCuenta": "%s", "valor": %s }
				""".formatted(numeroCuenta, valor);
	}

	@Test
	@DisplayName("POST /api/movimientos registra un depósito: 201, Location y tipo derivado del signo")
	void registrarDeposito() throws Exception {
		when(movimientoUseCase.registrar(any())).thenReturn(new ResultadoMovimiento(deposito(), false));

		mockMvc.perform(post("/api/movimientos").with(jwt()).contentType(MediaType.APPLICATION_JSON).content(cuerpo("225487", "600")))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", endsWith("/api/movimientos/10")))
				.andExpect(jsonPath("$.tipoMovimiento").value("DEPOSITO"))
				.andExpect(jsonPath("$.saldo").value(700.0));
	}

	@Test
	@DisplayName("F3: un retiro sin saldo responde 422 con el mensaje 'Saldo no disponible'")
	void retiroSinSaldo() throws Exception {
		when(movimientoUseCase.registrar(any())).thenThrow(new SaldoNoDisponibleException());

		mockMvc.perform(post("/api/movimientos").with(jwt()).contentType(MediaType.APPLICATION_JSON).content(cuerpo("496825", "-1")))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value("Saldo no disponible"))
				.andExpect(jsonPath("$.codigo").value("SALDO_NO_DISPONIBLE"));
	}

	@Test
	@DisplayName("Un reintento con la misma Idempotency-Key responde 200 y marca la respuesta como repetida")
	void reintentoIdempotente() throws Exception {
		when(movimientoUseCase.registrar(any())).thenReturn(new ResultadoMovimiento(deposito(), true));

		mockMvc.perform(post("/api/movimientos").with(jwt()).header("Idempotency-Key", "clave-1")
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo("225487", "600")))
				.andExpect(status().isOk())
				.andExpect(header().string("Idempotent-Replayed", "true"))
				.andExpect(jsonPath("$.id").value(10));
	}

	@Test
	@DisplayName("Sin valor responde 400 con el detalle del campo y no llega al caso de uso")
	void valorAusente() throws Exception {
		mockMvc.perform(post("/api/movimientos").with(jwt()).contentType(MediaType.APPLICATION_JSON)
						.content("{ \"numeroCuenta\": \"225487\" }"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACION"))
				.andExpect(jsonPath("$.errores[?(@.campo == 'valor')]").exists());
		verifyNoInteractions(movimientoUseCase);
	}

	@Test
	@DisplayName("Sin token JWT responde 401 y no llega al caso de uso")
	void sinToken() throws Exception {
		mockMvc.perform(post("/api/movimientos").contentType(MediaType.APPLICATION_JSON).content(cuerpo("225487", "600")))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(movimientoUseCase);
	}
}
