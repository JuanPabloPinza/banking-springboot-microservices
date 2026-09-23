package git.juanpablopinza.accounts.domain.exception;

public class MovimientoNoEncontradoException extends NoEncontradoException {

	public MovimientoNoEncontradoException(Long id) {
		super("MOVIMIENTO_NO_ENCONTRADO", "No existe el movimiento " + id);
	}
}
