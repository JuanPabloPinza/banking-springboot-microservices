package git.juanpablopinza.accounts.domain.exception;

public class CuentaInactivaException extends ReglaNegocioException {

	public CuentaInactivaException(String numeroCuenta) {
		super("CUENTA_INACTIVA", "La cuenta " + numeroCuenta + " está inactiva");
	}
}
