package git.juanpablopinza.clients.infrastructure.adapter.in.web.validation;

import git.juanpablopinza.clients.domain.model.Identificacion;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CedulaEcuatorianaValidator implements ConstraintValidator<CedulaEcuatoriana, String> {

	@Override
	public boolean isValid(String valor, ConstraintValidatorContext context) {
		return valor == null || Identificacion.esValida(valor);
	}
}
