package git.juanpablopinza.accounts.application.service;

import git.juanpablopinza.accounts.application.port.in.SincronizarClienteCommand;
import git.juanpablopinza.accounts.application.port.out.ClienteRefRepositoryPort;
import git.juanpablopinza.accounts.application.port.out.CuentaRepositoryPort;
import git.juanpablopinza.accounts.domain.model.ClienteRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SincronizacionClienteServiceTest {

	private static final UUID CLIENTE = UUID.randomUUID();
	private static final Instant T1 = Instant.parse("2026-09-23T15:00:00Z");
	private static final Instant T2 = T1.plusSeconds(5);

	@Mock
	private ClienteRefRepositoryPort clienteRefRepository;
	@Mock
	private CuentaRepositoryPort cuentaRepository;

	@InjectMocks
	private SincronizacionClienteService service;

	@Test
	@DisplayName("Un cliente nuevo se guarda en la réplica sin tocar cuentas")
	void clienteNuevo() {
		when(clienteRefRepository.buscarParaActualizar(CLIENTE)).thenReturn(Optional.empty());

		service.sincronizar(new SincronizarClienteCommand(CLIENTE, "Jose Lema", true, T1));

		verify(clienteRefRepository).guardar(new ClienteRef(CLIENTE, "Jose Lema", true, T1));
		verify(cuentaRepository, never()).desactivarPorCliente(any());
	}

	@Test
	@DisplayName("Un evento con estado false actualiza la réplica y desactiva las cuentas del cliente")
	void clienteEliminado() {
		when(clienteRefRepository.buscarParaActualizar(CLIENTE))
				.thenReturn(Optional.of(new ClienteRef(CLIENTE, "Juan Osorio", true, T1)));

		service.sincronizar(new SincronizarClienteCommand(CLIENTE, "Juan Osorio", false, T2));

		verify(clienteRefRepository).guardar(new ClienteRef(CLIENTE, "Juan Osorio", false, T2));
		verify(cuentaRepository).desactivarPorCliente(CLIENTE);
	}

	@Test
	@DisplayName("Un evento anterior al último aplicado se ignora")
	void eventoAtrasado() {
		when(clienteRefRepository.buscarParaActualizar(CLIENTE))
				.thenReturn(Optional.of(new ClienteRef(CLIENTE, "Nombre nuevo", true, T2)));

		service.sincronizar(new SincronizarClienteCommand(CLIENTE, "Nombre viejo", false, T1));

		verify(clienteRefRepository, never()).guardar(any());
		verify(cuentaRepository, never()).desactivarPorCliente(any());
	}
}
