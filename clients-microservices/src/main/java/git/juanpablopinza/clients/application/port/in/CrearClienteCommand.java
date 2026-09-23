package git.juanpablopinza.clients.application.port.in;

import git.juanpablopinza.clients.domain.model.Genero;

public record CrearClienteCommand(String nombre, Genero genero, int edad, String identificacion,
		String direccion, String telefono, String contrasena, Boolean estado) {
}
