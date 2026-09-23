package git.juanpablopinza.clients.application.port.in;

import git.juanpablopinza.clients.application.port.Pagina;
import git.juanpablopinza.clients.domain.model.Cliente;

import java.util.UUID;

public interface ClienteUseCase {

	Cliente crear(CrearClienteCommand command);

	Cliente obtener(UUID clienteId);

	Pagina<Cliente> listar(int pagina, int tamanio);

	Cliente actualizar(UUID clienteId, ActualizarClienteCommand command);

	Cliente actualizarParcial(UUID clienteId, ActualizarParcialClienteCommand command);

	void eliminar(UUID clienteId);
}
