package git.juanpablopinza.accounts.domain.model;

import java.time.Instant;
import java.util.UUID;

public record ClienteRef(UUID clienteId, String nombre, boolean estado, Instant actualizadoEn) {

	public boolean esMasRecienteQue(Instant ocurridoEn) {
		return actualizadoEn.isAfter(ocurridoEn);
	}
}
