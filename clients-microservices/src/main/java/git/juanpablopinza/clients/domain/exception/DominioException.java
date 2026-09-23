package git.juanpablopinza.clients.domain.exception;


public abstract sealed class DominioException extends RuntimeException
		permits NoEncontradoException, ConflictoException, SolicitudInvalidaException {

	private final String codigo;

	protected DominioException(String codigo, String mensaje) {
		super(mensaje);
		this.codigo = codigo;
	}

	public String getCodigo() {
		return codigo;
	}
}
