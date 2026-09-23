package git.juanpablopinza.accounts.domain.exception;

public class SaldoNoDisponibleException extends ReglaNegocioException {

	public SaldoNoDisponibleException() {
		super("SALDO_NO_DISPONIBLE", "Saldo no disponible");
	}
}
