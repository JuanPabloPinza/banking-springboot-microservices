package git.juanpablopinza.accounts.domain.exception;

public class OperacionAnuladaException extends ConflictoException {

	public OperacionAnuladaException(String idempotencyKey) {
		super("OPERACION_ANULADA",
				"La operación de la Idempotency-Key " + idempotencyKey + " fue anulada y no se vuelve a aplicar");
	}
}
