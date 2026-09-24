package git.juanpablopinza.accounts.infrastructure.adapter.out.persistence;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

final class Restricciones {

	private Restricciones() {
	}

	static boolean esViolacionDe(DataIntegrityViolationException error, String restriccion) {
		for (Throwable causa = error; causa != null; causa = causa.getCause()) {
			if (causa instanceof ConstraintViolationException violacion) {
				return restriccion.equalsIgnoreCase(violacion.getConstraintName());
			}
		}
		return false;
	}
}
