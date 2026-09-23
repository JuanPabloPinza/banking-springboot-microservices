package git.juanpablopinza.clients.infrastructure.adapter.in.web.dto;

import git.juanpablopinza.clients.domain.model.Genero;

import java.util.UUID;

/** Nunca expone la contraseña ni el id interno de la base de datos. */
public record ClienteResponse(UUID clienteId, String nombre, Genero genero, int edad, String identificacion,
		String direccion, String telefono, boolean estado) {
}
