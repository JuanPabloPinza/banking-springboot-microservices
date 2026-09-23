package git.juanpablopinza.clients.application.service;

import git.juanpablopinza.clients.application.port.Pagina;
import git.juanpablopinza.clients.application.port.in.ActualizarClienteCommand;
import git.juanpablopinza.clients.application.port.in.ActualizarParcialClienteCommand;
import git.juanpablopinza.clients.application.port.in.ClienteUseCase;
import git.juanpablopinza.clients.application.port.in.CrearClienteCommand;
import git.juanpablopinza.clients.application.port.out.ClienteEventoPublisherPort;
import git.juanpablopinza.clients.application.port.out.ClienteRepositoryPort;
import git.juanpablopinza.clients.application.port.out.PasswordHasherPort;
import git.juanpablopinza.clients.domain.event.ClienteEvento;
import git.juanpablopinza.clients.domain.event.TipoEventoCliente;
import git.juanpablopinza.clients.domain.exception.ClienteNoEncontradoException;
import git.juanpablopinza.clients.domain.exception.IdentificacionDuplicadaException;
import git.juanpablopinza.clients.domain.model.Cliente;
import git.juanpablopinza.clients.domain.model.Identificacion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNullElse;

@Service
@Transactional
@RequiredArgsConstructor
public class ClienteService implements ClienteUseCase {

	private final ClienteRepositoryPort clienteRepository;
	private final ClienteEventoPublisherPort eventoPublisher;
	private final PasswordHasherPort passwordHasher;
	private final Clock clock;

	@Override
	public Cliente crear(CrearClienteCommand command) {
		if (clienteRepository.existePorIdentificacion(command.identificacion())) {
			throw new IdentificacionDuplicadaException(command.identificacion());
		}
		Cliente cliente = Cliente.crear(command.nombre(), command.genero(), command.edad(),
				new Identificacion(command.identificacion()), command.direccion(), command.telefono(),
				passwordHasher.hash(command.contrasena()), requireNonNullElse(command.estado(), true));
		return guardarYPublicar(cliente, TipoEventoCliente.CREADO);
	}

	@Override
	@Transactional(readOnly = true)
	public Cliente obtener(UUID clienteId) {
		return buscar(clienteId);
	}

	@Override
	@Transactional(readOnly = true)
	public Pagina<Cliente> listar(int pagina, int tamanio) {
		return clienteRepository.listar(pagina, tamanio);
	}

	@Override
	public Cliente actualizar(UUID clienteId, ActualizarClienteCommand command) {
		Cliente cliente = buscar(clienteId);
		cliente.actualizar(command.nombre(), command.genero(), command.edad(), command.direccion(),
				command.telefono(), command.estado());
		return guardarYPublicar(cliente, TipoEventoCliente.ACTUALIZADO);
	}

	@Override
	public Cliente actualizarParcial(UUID clienteId, ActualizarParcialClienteCommand command) {
		Cliente cliente = buscar(clienteId);
		cliente.actualizar(
				requireNonNullElse(command.nombre(), cliente.getNombre()),
				requireNonNullElse(command.genero(), cliente.getGenero()),
				requireNonNullElse(command.edad(), cliente.getEdad()),
				requireNonNullElse(command.direccion(), cliente.getDireccion()),
				requireNonNullElse(command.telefono(), cliente.getTelefono()),
				requireNonNullElse(command.estado(), cliente.isEstado()));
		if (command.contrasena() != null) {
			cliente.cambiarContrasena(passwordHasher.hash(command.contrasena()));
		}
		return guardarYPublicar(cliente, TipoEventoCliente.ACTUALIZADO);
	}

	@Override
	public void eliminar(UUID clienteId) {
		Cliente cliente = buscar(clienteId);
		cliente.desactivar();
		guardarYPublicar(cliente, TipoEventoCliente.ELIMINADO);
	}

	private Cliente buscar(UUID clienteId) {
		return clienteRepository.buscarPorClienteId(clienteId)
				.orElseThrow(() -> new ClienteNoEncontradoException(clienteId));
	}

	private Cliente guardarYPublicar(Cliente cliente, TipoEventoCliente tipo) {
		Cliente guardado = clienteRepository.guardar(cliente);
		eventoPublisher.publicar(ClienteEvento.de(tipo, guardado, Instant.now(clock)));
		return guardado;
	}
}
