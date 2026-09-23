package git.juanpablopinza.accounts.domain.exception;

public class NumeroCuentaDuplicadoException extends ConflictoException {

	public NumeroCuentaDuplicadoException(String numeroCuenta) {
		super("NUMERO_CUENTA_DUPLICADO", "Ya existe la cuenta " + numeroCuenta);
	}
}
