package git.juanpablopinza.clients.domain.exception;

public abstract non-sealed class SolicitudInvalidaException extends DominioException {

	protected SolicitudInvalidaException(String codigo, String mensaje) {
		super(codigo, mensaje);
	}
}
