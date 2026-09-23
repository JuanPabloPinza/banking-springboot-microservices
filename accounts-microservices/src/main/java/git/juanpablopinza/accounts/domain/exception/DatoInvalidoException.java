package git.juanpablopinza.accounts.domain.exception;

public class DatoInvalidoException extends SolicitudInvalidaException {

	public DatoInvalidoException(String mensaje) {
		super("DATO_INVALIDO", mensaje);
	}
}
