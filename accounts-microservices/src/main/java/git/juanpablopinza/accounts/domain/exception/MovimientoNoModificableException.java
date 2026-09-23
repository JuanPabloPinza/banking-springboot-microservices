package git.juanpablopinza.accounts.domain.exception;

public class MovimientoNoModificableException extends ConflictoException {

	public MovimientoNoModificableException(Long id) {
		super("MOVIMIENTO_NO_MODIFICABLE",
				"Solo se puede modificar o eliminar el último movimiento de la cuenta (movimiento " + id + ")");
	}
}
