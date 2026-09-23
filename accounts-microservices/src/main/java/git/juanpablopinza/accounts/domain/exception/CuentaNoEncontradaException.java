package git.juanpablopinza.accounts.domain.exception;

public class CuentaNoEncontradaException extends NoEncontradoException {

	public CuentaNoEncontradaException(String numeroCuenta) {
		super("CUENTA_NO_ENCONTRADA", "No existe la cuenta " + numeroCuenta);
	}
}
