package git.juanpablopinza.clients.infrastructure.adapter.in.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = CedulaEcuatorianaValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface CedulaEcuatoriana {

	String message() default "La identificación debe ser una cédula ecuatoriana válida";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
