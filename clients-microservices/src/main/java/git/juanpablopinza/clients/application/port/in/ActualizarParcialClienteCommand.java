package git.juanpablopinza.clients.application.port.in;

import git.juanpablopinza.clients.domain.model.Genero;

public record ActualizarParcialClienteCommand(String nombre, Genero genero, Integer edad, String direccion,
		String telefono, String contrasena, Boolean estado) {
}
