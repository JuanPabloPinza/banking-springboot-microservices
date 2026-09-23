package git.juanpablopinza.clients.application.port.out;

import git.juanpablopinza.clients.application.port.Pagina;
import git.juanpablopinza.clients.domain.model.Cliente;

import java.util.Optional;
import java.util.UUID;

public interface ClienteRepositoryPort {

	Cliente guardar(Cliente cliente);

	Optional<Cliente> buscarPorClienteId(UUID clienteId);

	boolean existePorIdentificacion(String identificacion);

	Pagina<Cliente> listar(int pagina, int tamanio);
}
