package git.juanpablopinza.clients.application.port.in;

import git.juanpablopinza.clients.domain.model.Genero;

public record ActualizarClienteCommand(String nombre, Genero genero, int edad, String direccion,
		String telefono, boolean estado) {
}
