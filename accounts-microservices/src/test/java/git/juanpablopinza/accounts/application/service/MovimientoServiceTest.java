package git.juanpablopinza.accounts.application.service;

import git.juanpablopinza.accounts.application.port.in.RegistrarMovimientoCommand;
import git.juanpablopinza.accounts.application.port.in.ResultadoMovimiento;
import git.juanpablopinza.accounts.application.port.out.CuentaRepositoryPort;
import git.juanpablopinza.accounts.application.port.out.MovimientoRepositoryPort;
import git.juanpablopinza.accounts.domain.exception.CuentaNoEncontradaException;
import git.juanpablopinza.accounts.domain.exception.IdempotencyKeyReutilizadaException;
import git.juanpablopinza.accounts.domain.exception.MovimientoNoModificableException;
import git.juanpablopinza.accounts.domain.model.Cuenta;
import git.juanpablopinza.accounts.domain.model.Movimiento;
import git.juanpablopinza.accounts.domain.model.TipoCuenta;
import git.juanpablopinza.accounts.domain.model.TipoMovimiento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovimientoServiceTest {

	private static final Instant AHORA = Instant.parse("2026-09-23T15:00:00Z");

	@Mock
	private CuentaRepositoryPort cuentaRepository;
	@Mock
	private MovimientoRepositoryPort movimientoRepository;

	private MovimientoService service;

	@BeforeEach
	void setUp() {
		service = new MovimientoService(cuentaRepository, movimientoRepository, Clock.fixed(AHORA, ZoneOffset.UTC));
	}

	private Cuenta cuenta(String numero, String saldo) {
		BigDecimal valor = new BigDecimal(saldo);
		return new Cuenta(numero, TipoCuenta.AHORROS, valor, valor, true, UUID.randomUUID());
	}

	private Movimiento movimiento(Long id, String numero, String valor, String saldo, String clave) {
		BigDecimal monto = new BigDecimal(valor);
		return new Movimiento(id, numero, LocalDateTime.of(2026, 9, 23, 10, 0), TipoMovimiento.segun(monto), monto,
				new BigDecimal(saldo), clave);
	}

	@Test
	@DisplayName("Registrar bloquea la cuenta, actualiza el saldo y guarda el movimiento con la fecha del Clock")
	void registrarRetiro() {
		Cuenta cuenta = cuenta("478758", "2000");
		when(cuentaRepository.buscarPorNumeroParaActualizar("478758")).thenReturn(Optional.of(cuenta));
		when(movimientoRepository.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

		ResultadoMovimiento resultado = service.registrar(
				new RegistrarMovimientoCommand("478758", new BigDecimal("-575"), null));

		assertThat(resultado.repetido()).isFalse();
		assertThat(resultado.movimiento().getSaldo()).isEqualByComparingTo("1425");
		assertThat(resultado.movimiento().getFecha()).isEqualTo(LocalDateTime.of(2026, 9, 23, 15, 0));
		verify(cuentaRepository).guardar(cuenta);
		assertThat(cuenta.getSaldoDisponible()).isEqualByComparingTo("1425");
	}

	@Test
	@DisplayName("Con la misma Idempotency-Key y la misma solicitud se devuelve el movimiento original sin duplicarlo")
	void idempotenciaDevuelveOriginal() {
		Movimiento original = movimiento(7L, "478758", "-575", "1425", "clave-1");
		when(cuentaRepository.buscarPorNumeroParaActualizar("478758")).thenReturn(Optional.of(cuenta("478758", "1425")));
		when(movimientoRepository.buscarPorIdempotencyKey("clave-1")).thenReturn(Optional.of(original));

		ResultadoMovimiento resultado = service.registrar(
				new RegistrarMovimientoCommand("478758", new BigDecimal("-575.00"), "clave-1"));

		assertThat(resultado.repetido()).isTrue();
		assertThat(resultado.movimiento()).isSameAs(original);
		verify(movimientoRepository, never()).guardar(any());
		verify(cuentaRepository, never()).guardar(any());
	}

	@Test
	@DisplayName("Reusar una Idempotency-Key con otra solicitud es un conflicto")
	void idempotenciaConOtraSolicitud() {
		when(cuentaRepository.buscarPorNumeroParaActualizar("478758")).thenReturn(Optional.of(cuenta("478758", "1425")));
		when(movimientoRepository.buscarPorIdempotencyKey("clave-1"))
				.thenReturn(Optional.of(movimiento(7L, "478758", "-575", "1425", "clave-1")));

		assertThatThrownBy(() -> service.registrar(
				new RegistrarMovimientoCommand("478758", new BigDecimal("-100"), "clave-1")))
				.isInstanceOf(IdempotencyKeyReutilizadaException.class);
	}

	@Test
	@DisplayName("Registrar en una cuenta inexistente lanza CuentaNoEncontradaException")
	void cuentaInexistente() {
		when(cuentaRepository.buscarPorNumeroParaActualizar("000000")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.registrar(new RegistrarMovimientoCommand("000000", BigDecimal.TEN, null)))
				.isInstanceOf(CuentaNoEncontradaException.class);
	}

	@Test
	@DisplayName("Solo se puede corregir el último movimiento de la cuenta")
	void corregirMovimientoQueNoEsElUltimo() {
		Movimiento antiguo = movimiento(1L, "585545", "100", "1100", null);
		when(movimientoRepository.buscarPorId(1L)).thenReturn(Optional.of(antiguo));
		when(cuentaRepository.buscarPorNumeroParaActualizar("585545")).thenReturn(Optional.of(cuenta("585545", "1300")));
		when(movimientoRepository.buscarUltimo("585545"))
				.thenReturn(Optional.of(movimiento(2L, "585545", "200", "1300", null)));

		assertThatThrownBy(() -> service.corregir(1L, new BigDecimal("50")))
				.isInstanceOf(MovimientoNoModificableException.class);
		verify(movimientoRepository, never()).guardar(any());
	}

	@Test
	@DisplayName("Eliminar el último movimiento revierte su efecto en el saldo")
	void eliminarUltimoMovimiento() {
		Movimiento ultimo = movimiento(2L, "585545", "200", "1200", null);
		Cuenta cuenta = cuenta("585545", "1200");
		when(movimientoRepository.buscarPorId(2L)).thenReturn(Optional.of(ultimo));
		when(cuentaRepository.buscarPorNumeroParaActualizar("585545")).thenReturn(Optional.of(cuenta));
		when(movimientoRepository.buscarUltimo("585545")).thenReturn(Optional.of(ultimo));

		service.eliminar(2L);

		assertThat(cuenta.getSaldoDisponible()).isEqualByComparingTo("1000");
		verify(movimientoRepository).eliminar(2L);
	}
}
