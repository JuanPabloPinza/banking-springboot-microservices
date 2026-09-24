package git.juanpablopinza.accounts.application.service;

import git.juanpablopinza.accounts.application.port.in.ActualizarCuentaCommand;
import git.juanpablopinza.accounts.application.port.in.ActualizarParcialCuentaCommand;
import git.juanpablopinza.accounts.application.port.in.CrearCuentaCommand;
import git.juanpablopinza.accounts.application.port.out.ClienteRefRepositoryPort;
import git.juanpablopinza.accounts.application.port.out.CuentaRepositoryPort;
import git.juanpablopinza.accounts.domain.exception.ClienteInactivoException;
import git.juanpablopinza.accounts.domain.exception.ClienteNoEncontradoException;
import git.juanpablopinza.accounts.domain.exception.CuentaNoEncontradaException;
import git.juanpablopinza.accounts.domain.exception.NumeroCuentaDuplicadoException;
import git.juanpablopinza.accounts.domain.model.ClienteRef;
import git.juanpablopinza.accounts.domain.model.Cuenta;
import git.juanpablopinza.accounts.domain.model.TipoCuenta;
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
class CuentaServiceTest {

	private static final UUID CLIENTE = UUID.randomUUID();
	private static final Instant AHORA = Instant.parse("2026-09-23T15:00:00.750Z");

	@Mock
	private CuentaRepositoryPort cuentaRepository;
	@Mock
	private ClienteRefRepositoryPort clienteRefRepository;

	private CuentaService service;

	@BeforeEach
	void setUp() {
		service = new CuentaService(cuentaRepository, clienteRefRepository, Clock.fixed(AHORA, ZoneOffset.UTC));
	}

	private static CrearCuentaCommand crear(Boolean estado) {
		return new CrearCuentaCommand("478758", TipoCuenta.AHORROS, new BigDecimal("2000"), estado, CLIENTE);
	}

	private void clienteRegistrado(boolean activo) {
		when(clienteRefRepository.buscarConBloqueoCompartido(CLIENTE))
				.thenReturn(Optional.of(new ClienteRef(CLIENTE, "Jose Lema", activo, Instant.now())));
	}

	private void cuentaExistente(Cuenta cuenta) {
		when(cuentaRepository.buscarClienteId("478758")).thenReturn(Optional.of(CLIENTE));
		when(cuentaRepository.buscarPorNumeroParaActualizar("478758")).thenReturn(Optional.of(cuenta));
	}

	private static Cuenta cuenta(boolean estado) {
		BigDecimal saldo = new BigDecimal("2000.00");
		return new Cuenta("478758", TipoCuenta.AHORROS, saldo, saldo, estado, CLIENTE,
				LocalDateTime.of(2026, 9, 1, 9, 0));
	}

	@Test
	@DisplayName("Crear una cuenta para un cliente activo la deja activa, con saldo disponible igual al inicial y fecha de apertura del Clock")
	void crear() {
		clienteRegistrado(true);
		when(cuentaRepository.crear(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

		Cuenta cuenta = service.crear(crear(null));

		assertThat(cuenta.isEstado()).isTrue();
		assertThat(cuenta.getSaldoInicial()).isEqualTo(new BigDecimal("2000.00"));
		assertThat(cuenta.getSaldoDisponible()).isEqualTo(new BigDecimal("2000.00"));
		assertThat(cuenta.getClienteId()).isEqualTo(CLIENTE);
		assertThat(cuenta.getFechaApertura()).isEqualTo(LocalDateTime.of(2026, 9, 23, 15, 0, 0));
	}

	@Test
	@DisplayName("No se crea una cuenta si el cliente no llegó a la réplica local")
	void clienteInexistente() {
		when(clienteRefRepository.buscarConBloqueoCompartido(CLIENTE)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.crear(crear(true))).isInstanceOf(ClienteNoEncontradoException.class);
		verify(cuentaRepository, never()).crear(any());
	}

	@Test
	@DisplayName("No se crea una cuenta para un cliente inactivo")
	void clienteInactivo() {
		clienteRegistrado(false);

		assertThatThrownBy(() -> service.crear(crear(true))).isInstanceOf(ClienteInactivoException.class);
		verify(cuentaRepository, never()).crear(any());
	}

	@Test
	@DisplayName("El número de cuenta no se puede repetir")
	void numeroDuplicado() {
		clienteRegistrado(true);
		when(cuentaRepository.existePorNumero("478758")).thenReturn(true);

		assertThatThrownBy(() -> service.crear(crear(true))).isInstanceOf(NumeroCuentaDuplicadoException.class);
		verify(cuentaRepository, never()).crear(any());
	}

	@Test
	@DisplayName("PATCH solo cambia los campos enviados")
	void actualizarParcial() {
		Cuenta cuenta = cuenta(true);
		clienteRegistrado(true);
		cuentaExistente(cuenta);
		when(cuentaRepository.guardar(cuenta)).thenReturn(cuenta);

		service.actualizarParcial("478758", new ActualizarParcialCuentaCommand(TipoCuenta.CORRIENTE, null));

		assertThat(cuenta.getTipoCuenta()).isEqualTo(TipoCuenta.CORRIENTE);
		assertThat(cuenta.isEstado()).isTrue();
	}

	@Test
	@DisplayName("Al modificar una cuenta se bloquea primero al cliente y después la cuenta, igual que la baja del cliente")
	void ordenDeBloqueo() {
		clienteRegistrado(true);
		cuentaExistente(cuenta(false));

		service.actualizarParcial("478758", new ActualizarParcialCuentaCommand(null, true));

		InOrder orden = inOrder(clienteRefRepository, cuentaRepository);
		orden.verify(clienteRefRepository).buscarConBloqueoCompartido(CLIENTE);
		orden.verify(cuentaRepository).buscarPorNumeroParaActualizar("478758");
	}

	@Test
	@DisplayName("No se puede reactivar con PATCH una cuenta cuyo cliente está inactivo")
	void reactivarConClienteInactivoPorPatch() {
		Cuenta cuenta = cuenta(false);
		clienteRegistrado(false);
		cuentaExistente(cuenta);

		assertThatThrownBy(() -> service.actualizarParcial("478758", new ActualizarParcialCuentaCommand(null, true)))
				.isInstanceOf(ClienteInactivoException.class);
		assertThat(cuenta.isEstado()).isFalse();
		verify(cuentaRepository, never()).guardar(any());
	}

	@Test
	@DisplayName("No se puede reactivar con PUT una cuenta cuyo cliente está inactivo")
	void reactivarConClienteInactivoPorPut() {
		clienteRegistrado(false);
		cuentaExistente(cuenta(false));

		assertThatThrownBy(() -> service.actualizar("478758", new ActualizarCuentaCommand(TipoCuenta.AHORROS, true)))
				.isInstanceOf(ClienteInactivoException.class);
		verify(cuentaRepository, never()).guardar(any());
	}

	@Test
	@DisplayName("Desactivar una cuenta está permitido aunque el cliente esté inactivo")
	void desactivarConClienteInactivo() {
		Cuenta cuenta = cuenta(true);
		clienteRegistrado(false);
		cuentaExistente(cuenta);

		service.actualizarParcial("478758", new ActualizarParcialCuentaCommand(null, false));

		assertThat(cuenta.isEstado()).isFalse();
		verify(cuentaRepository).guardar(cuenta);
	}

	@Test
	@DisplayName("Modificar una cuenta inexistente lanza CuentaNoEncontradaException sin bloquear nada")
	void modificarCuentaInexistente() {
		when(cuentaRepository.buscarClienteId("000000")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.actualizarParcial("000000", new ActualizarParcialCuentaCommand(null, true)))
				.isInstanceOf(CuentaNoEncontradaException.class);
		verify(clienteRefRepository, never()).buscarConBloqueoCompartido(any());
	}

	@Test
	@DisplayName("Eliminar una cuenta la desactiva sin borrarla")
	void eliminar() {
		Cuenta cuenta = cuenta(true);
		when(cuentaRepository.buscarPorNumeroParaActualizar("478758")).thenReturn(Optional.of(cuenta));

		service.eliminar("478758");

		assertThat(cuenta.isEstado()).isFalse();
		verify(cuentaRepository).guardar(cuenta);
	}

	@Test
	@DisplayName("Obtener una cuenta inexistente lanza CuentaNoEncontradaException")
	void cuentaInexistente() {
		when(cuentaRepository.buscarPorNumero("000000")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.obtener("000000")).isInstanceOf(CuentaNoEncontradaException.class);
	}
}
