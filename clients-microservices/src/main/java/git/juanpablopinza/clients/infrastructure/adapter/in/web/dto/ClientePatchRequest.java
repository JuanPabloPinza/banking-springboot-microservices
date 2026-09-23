package git.juanpablopinza.clients.infrastructure.adapter.in.web.dto;

import git.juanpablopinza.clients.domain.model.Genero;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClientePatchRequest(
		@Size(max = 100, message = "El nombre admite máximo 100 caracteres")
		@Pattern(regexp = ".*\\S.*", message = "El nombre no puede estar vacío")
		String nombre,

		Genero genero,

		@Min(value = 18, message = "La edad mínima es 18 años")
		@Max(value = 120, message = "La edad máxima es 120 años")
		Integer edad,

		@Size(max = 200, message = "La dirección admite máximo 200 caracteres")
		@Pattern(regexp = ".*\\S.*", message = "La dirección no puede estar vacía")
		String direccion,

		@Pattern(regexp = "^0\\d{8,9}$", message = "El teléfono debe empezar con 0 y tener 9 o 10 dígitos")
		String telefono,

		@Size(min = 4, max = 72, message = "La contraseña debe tener entre 4 y 72 caracteres")
		String contrasena,

		Boolean estado) {
}
