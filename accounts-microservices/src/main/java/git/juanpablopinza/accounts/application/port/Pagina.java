package git.juanpablopinza.accounts.application.port;

import java.util.List;
import java.util.function.Function;

public record Pagina<T>(List<T> contenido, int pagina, int tamanio, long totalElementos, int totalPaginas) {

	public <R> Pagina<R> map(Function<T, R> mapper) {
		return new Pagina<>(contenido.stream().map(mapper).toList(), pagina, tamanio, totalElementos, totalPaginas);
	}
}
