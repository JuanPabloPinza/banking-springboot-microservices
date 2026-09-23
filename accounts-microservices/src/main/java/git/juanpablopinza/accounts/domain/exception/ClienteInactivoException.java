package git.juanpablopinza.accounts.domain.exception;

import java.util.UUID;

public class ClienteInactivoException extends ReglaNegocioException {

	public ClienteInactivoException(UUID clienteId) {
		super("CLIENTE_INACTIVO", "El cliente " + clienteId + " está inactivo");
	}
}
