package git.juanpablopinza.accounts.infrastructure.adapter.in.web;

import git.juanpablopinza.accounts.domain.exception.ConflictoException;
import git.juanpablopinza.accounts.domain.exception.DominioException;
import git.juanpablopinza.accounts.domain.exception.NoEncontradoException;
import git.juanpablopinza.accounts.domain.exception.ReglaNegocioException;
import git.juanpablopinza.accounts.domain.exception.SolicitudInvalidaException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	record ErrorCampo(String campo, String mensaje) {
	}

	@ExceptionHandler(DominioException.class)
	ProblemDetail manejarDominio(DominioException ex) {
		return switch (ex) {
			case NoEncontradoException e -> problema(HttpStatus.NOT_FOUND, "Recurso no encontrado", e);
			case ConflictoException e -> problema(HttpStatus.CONFLICT, "Conflicto", e);
			case ReglaNegocioException e -> problema(HttpStatus.UNPROCESSABLE_CONTENT, "Regla de negocio incumplida", e);
			case SolicitudInvalidaException e -> problema(HttpStatus.BAD_REQUEST, "Solicitud inválida", e);
		};
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail manejarIntegridad(DataIntegrityViolationException ex) {
		log.warn("Violación de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
		return problema(HttpStatus.CONFLICT, "Conflicto",
				"La operación viola una restricción de datos (registro duplicado o inválido)", "CONFLICTO_DATOS");
	}

	@ExceptionHandler(PessimisticLockingFailureException.class)
	ProblemDetail manejarBloqueo(PessimisticLockingFailureException ex) {
		log.warn("Conflicto de bloqueo: {}", ex.getMessage());
		return problema(HttpStatus.CONFLICT, "Conflicto",
				"La cuenta está siendo modificada por otra operación. Intente nuevamente.", "OPERACION_CONCURRENTE");
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail manejarInesperado(Exception ex) {
		log.error("Error no controlado", ex);
		return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
				"Ocurrió un error inesperado. Intente nuevamente.", "ERROR_INTERNO");
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<ErrorCampo> errores = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> new ErrorCampo(error.getField(), mensaje(error.getDefaultMessage())))
				.toList();
		return ResponseEntity.badRequest().body(errorValidacion(errores));
	}

	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<ErrorCampo> errores = ex.getParameterValidationResults().stream()
				.flatMap(resultado -> resultado.getResolvableErrors().stream()
						.map(error -> new ErrorCampo(
								error instanceof FieldError campo ? campo.getField()
										: resultado.getMethodParameter().getParameterName(),
								mensaje(error.getDefaultMessage()))))
				.toList();
		return ResponseEntity.badRequest().body(errorValidacion(errores));
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return ResponseEntity.badRequest().body(problema(HttpStatus.BAD_REQUEST, "Solicitud inválida",
				"El cuerpo no es un JSON válido o contiene valores no permitidos", "JSON_INVALIDO"));
	}

	@Override
	protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		return ResponseEntity.badRequest().body(problema(HttpStatus.BAD_REQUEST, "Solicitud inválida",
				"El valor '%s' no es válido para '%s'".formatted(ex.getValue(), ex.getPropertyName()),
				"PARAMETRO_INVALIDO"));
	}

	private static ProblemDetail errorValidacion(List<ErrorCampo> errores) {
		ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Solicitud inválida",
				"La solicitud tiene campos inválidos", "VALIDACION");
		problema.setProperty("errores", errores);
		return problema;
	}

	private static String mensaje(String mensaje) {
		return Objects.requireNonNullElse(mensaje, "Valor inválido");
	}

	private static ProblemDetail problema(HttpStatus status, String titulo, DominioException ex) {
		return problema(status, titulo, ex.getMessage(), ex.getCodigo());
	}

	private static ProblemDetail problema(HttpStatus status, String titulo, String detalle, String codigo) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalle);
		problema.setTitle(titulo);
		problema.setProperty("codigo", codigo);
		problema.setProperty("timestamp", Instant.now());
		return problema;
	}
}
