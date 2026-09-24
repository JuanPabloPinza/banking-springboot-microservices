package git.juanpablopinza.accounts.application.service;

import git.juanpablopinza.accounts.application.port.in.RegistrarMovimientoCommand;
import git.juanpablopinza.accounts.application.port.in.ResultadoMovimiento;
import git.juanpablopinza.accounts.application.port.out.CuentaRepositoryPort;
import git.juanpablopinza.accounts.application.port.out.MovimientoRepositoryPort;
import git.juanpablopinza.accounts.application.port.out.SolicitudIdempotenteRepositoryPort;
import git.juanpablopinza.accounts.domain.exception.CuentaNoEncontradaException;
import git.juanpablopinza.accounts.domain.exception.IdempotencyKeyReutilizadaException;
import git.juanpablopinza.accounts.domain.exception.MovimientoNoEncontradoException;
import git.juanpablopinza.accounts.domain.exception.MovimientoNoModificableException;
import git.juanpablopinza.accounts.domain.exception.OperacionAnuladaException;
import git.juanpablopinza.accounts.domain.model.Cuenta;
import git.juanpablopinza.accounts.domain.model.Movimiento;
import git.juanpablopinza.accounts.domain.model.SolicitudIdempotente;
import git.juanpablopinza.accounts.domain.model.TipoCuenta;
import git.juanpablopinza.accounts.domain.model.TipoMovimiento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovimientoServiceTest {

	private static final Instant AHORA = Instant.parse("2026-09-23T15:00:00Z");
	private static final LocalDateTime FECHA = LocalDateTime.of(2026, 9, 23, 10, 0);

	@Mock
	private CuentaRepositoryPort cuentaRepository;
	@Mock
	private MovimientoRepositoryPort movimientoRepository;
	@Mock
	private SolicitudIdempotenteRepositoryPort solicitudRepository;

	private MovimientoService service;

	@BeforeEach
	void setUp() {
		service = new MovimientoService(cuentaRepository, movimientoRepository, solicitudRepository,
				Clock.fixed(AHORA, ZoneOffset.UTC));
	}

	private Cuenta cuenta(String numero, String saldo) {
		BigDecimal valor = new BigDecimal(saldo);
		return new Cuenta(numero, TipoCuenta.AHORROS, valor, valor, true, UUID.randomUUID(), FECHA);
	}

	private Movimiento movimiento(Long id, String numero, String valor, String saldo) {
		BigDecimal monto = new BigDecimal(valor);
		return new Movimiento(id, numero, FECHA, TipoMovimiento.segun(monto), monto, new BigDecimal(saldo));
	}

	private void cuentaBloqueada(String numero, String saldo) {
		when(cuentaRepository.buscarPorNumeroParaActualizar(numero)).thenReturn(Optional.of(cuenta(numero, saldo)));
	}

	private void solicitudPrevia(String clave, String numero, String valor, Long movimientoId) {
		when(solicitudRepository.buscar(clave)).thenReturn(Optional.of(
				new SolicitudIdempotente(clave, numero, new BigDecimal(valor), movimientoId, FECHA)));
	}

	private ResultadoMovimiento registrar(String numero, String valor, String clave) {
		return service.registrar(new RegistrarMovimientoCommand(numero, new BigDecimal(valor), clave));
	}

	@Test
	@DisplayName("Registrar bloquea la cuenta, actualiza el saldo y guarda el movimiento con la fecha del Clock")
	void registrarRetiro() {
		Cuenta cuenta = cuenta("478758", "2000");
		when(cuentaRepository.buscarPorNumeroParaActualizar("478758")).thenReturn(Optional.of(cuenta));
		when(movimientoRepository.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

		ResultadoMovimiento resultado = registrar("478758", "-575", null);

		assertThat(resultado.repetido()).isFalse();
		assertThat(resultado.movimiento().getSaldo()).isEqualByComparingTo("1425");
		assertThat(resultado.movimiento().getFecha()).isEqualTo(LocalDateTime.of(2026, 9, 23, 15, 0));
		verify(cuentaRepository).guardar(cuenta);
		verify(solicitudRepository, never()).crear(any());
		assertThat(cuenta.getSaldoDisponible()).isEqualByComparingTo("1425");
	}

	@Test
	@DisplayName("Con Idempotency-Key se registra la solicitud original ligada al movimiento creado")
	void registrarGuardaSolicitudIdempotente() {
		cuentaBloqueada("585545", "1000");
		when(solicitudRepository.buscar("clave-1")).thenReturn(Optional.empty());
		when(movimientoRepository.guardar(any())).thenReturn(movimiento(7L, "585545", "100.00", "1100.00"));

		registrar("585545", "100", "clave-1");

		verify(solicitudRepository).crear(new SolicitudIdempotente("clave-1", "585545", new BigDecimal("100.00"), 7L,
				LocalDateTime.of(2026, 9, 23, 15, 0)));
	}

	@Test
	@DisplayName("Un reintento con la misma clave y la misma solicitud devuelve el movimiento sin duplicarlo")
	void idempotenciaDevuelveOriginal() {
		Movimiento original = movimiento(7L, "478758", "-575", "1425");
		cuentaBloqueada("478758", "1425");
		solicitudPrevia("clave-1", "478758", "-575", 7L);
		when(movimientoRepository.buscarPorId(7L)).thenReturn(Optional.of(original));

		ResultadoMovimiento resultado = registrar("478758", "-575.00", "clave-1");

		assertThat(resultado.repetido()).isTrue();
		assertThat(resultado.movimiento()).isSameAs(original);
		verify(movimientoRepository, never()).guardar(any());
		verify(cuentaRepository, never()).guardar(any());
	}

	@Test
	@DisplayName("Si el movimiento se corrigió después, el reintento original se reconoce y devuelve el estado actual")
	void idempotenciaTrasCorreccion() {
		Movimiento corregido = movimiento(7L, "585545", "200", "1200");
		cuentaBloqueada("585545", "1200");
		solicitudPrevia("clave-1", "585545", "100", 7L);
		when(movimientoRepository.buscarPorId(7L)).thenReturn(Optional.of(corregido));

		ResultadoMovimiento resultado = registrar("585545", "100", "clave-1");

		assertThat(resultado.repetido()).isTrue();
		assertThat(resultado.movimiento().getValor()).isEqualByComparingTo("200");
		verify(movimientoRepository, never()).guardar(any());
	}

	@Test
	@DisplayName("Si el movimiento se eliminó, el reintento no se vuelve a aplicar")
	void idempotenciaTrasAnulacion() {
		cuentaBloqueada("585545", "1000");
		solicitudPrevia("clave-1", "585545", "100", null);

		assertThatThrownBy(() -> registrar("585545", "100", "clave-1"))
				.isInstanceOf(OperacionAnuladaException.class);
		verify(movimientoRepository, never()).guardar(any());
		verify(cuentaRepository, never()).guardar(any());
	}

	@Test
	@DisplayName("Reusar una Idempotency-Key con otra solicitud es un conflicto, aunque el movimiento se haya corregido")
	void idempotenciaConOtraSolicitud() {
		cuentaBloqueada("478758", "1425");
		solicitudPrevia("clave-1", "478758", "-575", 7L);

		assertThatThrownBy(() -> registrar("478758", "-100", "clave-1"))
				.isInstanceOf(IdempotencyKeyReutilizadaException.class);
		verify(movimientoRepository, never()).guardar(any());
	}

	@Test
	@DisplayName("Registrar en una cuenta inexistente lanza CuentaNoEncontradaException")
	void cuentaInexistente() {
		when(cuentaRepository.buscarPorNumeroParaActualizar("000000")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> registrar("000000", "10", null)).isInstanceOf(CuentaNoEncontradaException.class);
	}

	@Test
	@DisplayName("Solo se puede corregir el último movimiento de la cuenta")
	void corregirMovimientoQueNoEsElUltimo() {
		Movimiento antiguo = movimiento(1L, "585545", "100", "1100");
		when(movimientoRepository.buscarNumeroCuenta(1L)).thenReturn(Optional.of("585545"));
		when(movimientoRepository.buscarPorId(1L)).thenReturn(Optional.of(antiguo));
		cuentaBloqueada("585545", "1300");
		when(movimientoRepository.buscarUltimo("585545"))
				.thenReturn(Optional.of(movimiento(2L, "585545", "200", "1300")));

		assertThatThrownBy(() -> service.corregir(1L, new BigDecimal("50")))
				.isInstanceOf(MovimientoNoModificableException.class);
		verify(movimientoRepository, never()).guardar(any());
	}

	@Test
	@DisplayName("Corregir bloquea la cuenta antes de leer el movimiento y la cuenta, para no operar con estado obsoleto")
	void corregirBloqueaAntesDeLeer() {
		Movimiento ultimo = movimiento(2L, "585545", "200", "1200");
		when(movimientoRepository.buscarNumeroCuenta(2L)).thenReturn(Optional.of("585545"));
		cuentaBloqueada("585545", "1200");
		when(movimientoRepository.buscarPorId(2L)).thenReturn(Optional.of(ultimo));
		when(movimientoRepository.buscarUltimo("585545")).thenReturn(Optional.of(ultimo));

		service.corregir(2L, new BigDecimal("50"));

		InOrder orden = inOrder(movimientoRepository, cuentaRepository);
		orden.verify(movimientoRepository).buscarNumeroCuenta(2L);
		orden.verify(cuentaRepository).buscarPorNumeroParaActualizar("585545");
		orden.verify(movimientoRepository).buscarPorId(2L);
	}

	@Test
	@DisplayName("Corregir un movimiento inexistente lanza MovimientoNoEncontradoException sin bloquear cuentas")
	void corregirMovimientoInexistente() {
		when(movimientoRepository.buscarNumeroCuenta(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.corregir(99L, BigDecimal.TEN))
				.isInstanceOf(MovimientoNoEncontradoException.class);
		verify(cuentaRepository, never()).buscarPorNumeroParaActualizar(any());
	}

	@Test
	@DisplayName("Eliminar el último movimiento revierte su efecto en el saldo")
	void eliminarUltimoMovimiento() {
		Movimiento ultimo = movimiento(2L, "585545", "200", "1200");
		Cuenta cuenta = cuenta("585545", "1200");
		when(movimientoRepository.buscarNumeroCuenta(2L)).thenReturn(Optional.of("585545"));
		when(movimientoRepository.buscarPorId(2L)).thenReturn(Optional.of(ultimo));
		when(cuentaRepository.buscarPorNumeroParaActualizar("585545")).thenReturn(Optional.of(cuenta));
		when(movimientoRepository.buscarUltimo("585545")).thenReturn(Optional.of(ultimo));

		service.eliminar(2L);

		assertThat(cuenta.getSaldoDisponible()).isEqualByComparingTo("1000");
		verify(movimientoRepository).eliminar(2L);
	}
}
