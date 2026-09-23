package git.juanpablopinza.clients.domain.model;

import git.juanpablopinza.clients.domain.exception.DatoInvalidoException;
import lombok.Getter;

import java.util.Objects;

@Getter
public abstract class Persona {

	private static final int EDAD_MINIMA = 18;
	private static final int EDAD_MAXIMA = 120;

	private String nombre;
	private Genero genero;
	private int edad;
	private final Identificacion identificacion;
	private String direccion;
	private String telefono;

	protected Persona(String nombre, Genero genero, int edad, Identificacion identificacion,
			String direccion, String telefono) {
		this.identificacion = Objects.requireNonNull(identificacion, "La identificación es obligatoria");
		actualizarDatosPersonales(nombre, genero, edad, direccion, telefono);
	}

	protected final void actualizarDatosPersonales(String nombre, Genero genero, int edad,
			String direccion, String telefono) {
		if (edad < EDAD_MINIMA || edad > EDAD_MAXIMA) {
			throw new DatoInvalidoException("La edad debe estar entre %d y %d años".formatted(EDAD_MINIMA, EDAD_MAXIMA));
		}
		this.nombre = nombre;
		this.genero = genero;
		this.edad = edad;
		this.direccion = direccion;
		this.telefono = telefono;
	}
}
