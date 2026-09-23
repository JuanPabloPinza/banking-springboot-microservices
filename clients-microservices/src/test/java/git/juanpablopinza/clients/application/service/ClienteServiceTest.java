package git.juanpablopinza.clients.application.service;

import git.juanpablopinza.clients.application.port.in.ActualizarParcialClienteCommand;
import git.juanpablopinza.clients.application.port.in.CrearClienteCommand;
import git.juanpablopinza.clients.application.port.out.ClienteEventoPublisherPort;
import git.juanpablopinza.clients.application.port.out.ClienteRepositoryPort;
import git.juanpablopinza.clients.application.port.out.PasswordHasherPort;
import git.juanpablopinza.clients.domain.event.ClienteEvento;
import git.juanpablopinza.clients.domain.event.TipoEventoCliente;
import git.juanpablopinza.clients.domain.exception.ClienteNoEncontradoException;
import git.juanpablopinza.clients.domain.exception.IdentificacionDuplicadaException;
import git.juanpablopinza.clients.domain.model.Cliente;
import git.juanpablopinza.clients.domain.model.Genero;
import git.juanpablopinza.clients.domain.model.Identificacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

	private static final Instant AHORA = Instant.parse("2026-09-23T15:00:00Z");

	@Mock
	private ClienteRepositoryPort clienteRepository;
	@Mock
	private ClienteEventoPublisherPort eventoPublisher;
	@Mock
	private PasswordHasherPort passwordHasher;

	private ClienteService service;

	@BeforeEach
	void setUp() {
		service = new ClienteService(clienteRepository, eventoPublisher, passwordHasher,
				Clock.fixed(AHORA, ZoneOffset.UTC));
	}

	private CrearClienteCommand comandoJoseLema() {
		return new CrearClienteCommand("Jose Lema", Genero.MASCULINO, 35, "1710034065", "Otavalo sn y principal",
				"098254785", "1234", null);
	}

	private Cliente clienteExistente() {
		return Cliente.crear("Marianela Montalvo", Genero.FEMENINO, 30, new Identificacion("1712345675"),
				"Amazonas y NNUU", "097548965", "hash-5678", true);
	}

	@Test
	@DisplayName("Crear guarda la contraseña hasheada, nace activo y publica el evento CREADO")
	void crearCliente() {
		when(clienteRepository.existePorIdentificacion("1710034065")).thenReturn(false);
		when(passwordHasher.hash("1234")).thenReturn("hash-1234");
		when(clienteRepository.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

		Cliente creado = service.crear(comandoJoseLema());

		assertThat(creado.getContrasena()).isEqualTo("hash-1234");
		assertThat(creado.isEstado()).isTrue();
		ArgumentCaptor<ClienteEvento> evento = ArgumentCaptor.forClass(ClienteEvento.class);
		verify(eventoPublisher).publicar(evento.capture());
		assertThat(evento.getValue().tipo()).isEqualTo(TipoEventoCliente.CREADO);
		assertThat(evento.getValue().clienteId()).isEqualTo(creado.getClienteId());
		assertThat(evento.getValue().ocurridoEn()).isEqualTo(AHORA);
	}

	@Test
	@DisplayName("Crear con identificación duplicada lanza conflicto y no guarda ni publica")
	void crearConIdentificacionDuplicada() {
		when(clienteRepository.existePorIdentificacion("1710034065")).thenReturn(true);

		assertThatThrownBy(() -> service.crear(comandoJoseLema()))
				.isInstanceOf(IdentificacionDuplicadaException.class);
		verify(clienteRepository, never()).guardar(any());
		verifyNoInteractions(eventoPublisher);
	}

	@Test
	@DisplayName("PATCH solo cambia los campos enviados y hashea la nueva contraseña")
	void actualizarParcial() {
		Cliente existente = clienteExistente();
		when(clienteRepository.buscarPorClienteId(existente.getClienteId())).thenReturn(Optional.of(existente));
		when(passwordHasher.hash("9999")).thenReturn("hash-9999");
		when(clienteRepository.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

		Cliente actualizado = service.actualizarParcial(existente.getClienteId(),
				new ActualizarParcialClienteCommand(null, null, null, "Av. Shyris", null, "9999", null));

		assertThat(actualizado.getDireccion()).isEqualTo("Av. Shyris");
		assertThat(actualizado.getNombre()).isEqualTo("Marianela Montalvo");
		assertThat(actualizado.getContrasena()).isEqualTo("hash-9999");
		assertThat(actualizado.isEstado()).isTrue();
	}

	@Test
	@DisplayName("Eliminar desactiva al cliente y publica el evento ELIMINADO")
	void eliminar() {
		Cliente existente = clienteExistente();
		when(clienteRepository.buscarPorClienteId(existente.getClienteId())).thenReturn(Optional.of(existente));
		when(clienteRepository.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

		service.eliminar(existente.getClienteId());

		assertThat(existente.isEstado()).isFalse();
		ArgumentCaptor<ClienteEvento> evento = ArgumentCaptor.forClass(ClienteEvento.class);
		verify(eventoPublisher).publicar(evento.capture());
		assertThat(evento.getValue().tipo()).isEqualTo(TipoEventoCliente.ELIMINADO);
		assertThat(evento.getValue().estado()).isFalse();
	}

	@Test
	@DisplayName("Obtener un cliente inexistente lanza ClienteNoEncontradoException")
	void obtenerInexistente() {
		UUID clienteId = UUID.randomUUID();
		when(clienteRepository.buscarPorClienteId(clienteId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.obtener(clienteId)).isInstanceOf(ClienteNoEncontradoException.class);
	}
}
