package git.juanpablopinza.clients.infrastructure.adapter.in.web.validation;

import git.juanpablopinza.clients.domain.model.Identificacion;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CedulaEcuatorianaValidator implements ConstraintValidator<CedulaEcuatoriana, String> {

	/** null lo resuelve @NotBlank; aquí solo se valida el formato. */
	@Override
	public boolean isValid(String valor, ConstraintValidatorContext context) {
		return valor == null || Identificacion.esValida(valor);
	}
}
