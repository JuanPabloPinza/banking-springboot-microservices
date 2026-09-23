package git.juanpablopinza.accounts.domain.exception;

public abstract non-sealed class ConflictoException extends DominioException {

	protected ConflictoException(String codigo, String mensaje) {
		super(codigo, mensaje);
	}
}
