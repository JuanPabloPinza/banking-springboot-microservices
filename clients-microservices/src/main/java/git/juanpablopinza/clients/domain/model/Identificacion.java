package git.juanpablopinza.clients.domain.model;

import git.juanpablopinza.clients.domain.exception.DatoInvalidoException;


public record Identificacion(String valor) {

	public Identificacion {
		if (!esValida(valor)) {
			throw new DatoInvalidoException("La identificación debe ser una cédula de Ecuador válida");
		}
	}

	public static boolean esValida(String cedula) {
		if (cedula == null || !cedula.matches("\\d{10}")) {
			return false;
		}
		int provincia = Integer.parseInt(cedula.substring(0, 2));
		if ((provincia < 1 || provincia > 24) && provincia != 30) {
			return false;
		}
		if (digito(cedula, 2) >= 6) {
			return false;
		}
		int suma = 0;
		for (int i = 0; i < 9; i++) {
			int producto = digito(cedula, i) * (i % 2 == 0 ? 2 : 1);
			suma += producto > 9 ? producto - 9 : producto;
		}
		int verificador = (10 - suma % 10) % 10;
		return verificador == digito(cedula, 9);
	}

	private static int digito(String cedula, int posicion) {
		return cedula.charAt(posicion) - '0';
	}
}
