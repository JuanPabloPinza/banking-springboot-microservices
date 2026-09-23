package git.juanpablopinza.accounts.domain.exception;

public abstract non-sealed class NoEncontradoException extends DominioException {

	protected NoEncontradoException(String codigo, String mensaje) {
		super(codigo, mensaje);
	}
}
