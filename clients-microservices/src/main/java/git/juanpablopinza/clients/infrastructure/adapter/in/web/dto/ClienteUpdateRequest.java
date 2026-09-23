package git.juanpablopinza.clients.infrastructure.adapter.in.web.dto;

import git.juanpablopinza.clients.domain.model.Genero;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** PUT: reemplazo completo de los datos editables del cliente. */
public record ClienteUpdateRequest(
		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 100, message = "El nombre admite máximo 100 caracteres")
		String nombre,

		@NotNull(message = "El género es obligatorio")
		Genero genero,

		@NotNull(message = "La edad es obligatoria")
		@Min(value = 18, message = "La edad mínima es 18 años")
		@Max(value = 120, message = "La edad máxima es 120 años")
		Integer edad,

		@NotBlank(message = "La dirección es obligatoria")
		@Size(max = 200, message = "La dirección admite máximo 200 caracteres")
		String direccion,

		@NotBlank(message = "El teléfono es obligatorio")
		@Pattern(regexp = "^0\\d{8,9}$", message = "El teléfono debe empezar con 0 y tener 9 o 10 dígitos")
		String telefono,

		@NotNull(message = "El estado es obligatorio")
		Boolean estado) {
}
