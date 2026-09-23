package git.juanpablopinza.accounts.domain.exception;

import java.util.UUID;

public class ClienteNoEncontradoException extends NoEncontradoException {

	public ClienteNoEncontradoException(UUID clienteId) {
		super("CLIENTE_NO_ENCONTRADO", "No existe un cliente con clienteId " + clienteId);
	}
}
