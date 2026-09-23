package git.juanpablopinza.clients.domain.exception;

public class IdentificacionDuplicadaException extends ConflictoException {

	public IdentificacionDuplicadaException(String identificacion) {
		super("IDENTIFICACION_DUPLICADA", "Ya existe un cliente con la identificación " + identificacion);
	}
}
