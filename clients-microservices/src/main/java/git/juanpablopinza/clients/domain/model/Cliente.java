package git.juanpablopinza.clients.domain.model;

import lombok.Getter;

import java.util.UUID;

@Getter
public class Cliente extends Persona {

	private final UUID clienteId;
	private String contrasena;
	private boolean estado;

	public Cliente(UUID clienteId, String nombre, Genero genero, int edad, Identificacion identificacion,
			String direccion, String telefono, String contrasena, boolean estado) {
		super(nombre, genero, edad, identificacion, direccion, telefono);
		this.clienteId = clienteId;
		this.contrasena = contrasena;
		this.estado = estado;
	}

	public static Cliente crear(String nombre, Genero genero, int edad, Identificacion identificacion,
			String direccion, String telefono, String contrasenaHash, boolean estado) {
		return new Cliente(UUID.randomUUID(), nombre, genero, edad, identificacion, direccion, telefono,
				contrasenaHash, estado);
	}

	public void actualizar(String nombre, Genero genero, int edad, String direccion, String telefono,
			boolean estado) {
		actualizarDatosPersonales(nombre, genero, edad, direccion, telefono);
		this.estado = estado;
	}

	public void cambiarContrasena(String contrasenaHash) {
		this.contrasena = contrasenaHash;
	}

	public void desactivar() {
		this.estado = false;
	}
}
