package git.juanpablopinza.accounts.domain.exception;

public class IdempotencyKeyReutilizadaException extends ConflictoException {

	public IdempotencyKeyReutilizadaException(String idempotencyKey) {
		super("IDEMPOTENCY_KEY_REUTILIZADA",
				"La Idempotency-Key " + idempotencyKey + " ya se usó con una solicitud distinta");
	}
}
