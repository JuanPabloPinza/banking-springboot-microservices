package git.juanpablopinza.accounts.domain.exception;

public abstract non-sealed class ReglaNegocioException extends DominioException {

	protected ReglaNegocioException(String codigo, String mensaje) {
		super(codigo, mensaje);
	}
}
