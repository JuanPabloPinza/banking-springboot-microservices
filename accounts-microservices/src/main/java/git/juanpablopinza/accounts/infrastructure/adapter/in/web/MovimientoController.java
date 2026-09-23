package git.juanpablopinza.accounts.infrastructure.adapter.in.web;

import git.juanpablopinza.accounts.application.port.Pagina;
import git.juanpablopinza.accounts.application.port.in.MovimientoUseCase;
import git.juanpablopinza.accounts.application.port.in.RegistrarMovimientoCommand;
import git.juanpablopinza.accounts.application.port.in.ResultadoMovimiento;
import git.juanpablopinza.accounts.infrastructure.adapter.in.web.dto.MovimientoRequest;
import git.juanpablopinza.accounts.infrastructure.adapter.in.web.dto.MovimientoResponse;
import git.juanpablopinza.accounts.infrastructure.adapter.in.web.dto.MovimientoUpdateRequest;
import git.juanpablopinza.accounts.infrastructure.adapter.in.web.mapper.CuentaWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/movimientos")
@RequiredArgsConstructor
@Tag(name = "Movimientos", description = "Registro de movimientos (F1, F2, F3)")
public class MovimientoController {

	static final String IDEMPOTENCY_KEY = "Idempotency-Key";
	static final String IDEMPOTENT_REPLAYED = "Idempotent-Replayed";

	private final MovimientoUseCase movimientoUseCase;
	private final CuentaWebMapper mapper;

	@PostMapping
	@Operation(summary = "Registrar un depósito (valor positivo) o un retiro (valor negativo)",
			description = "Sin saldo suficiente responde 422 'Saldo no disponible'. Con Idempotency-Key, "
					+ "un reintento devuelve el movimiento original (200) en vez de duplicarlo.")
	public ResponseEntity<MovimientoResponse> registrar(
			@Parameter(description = "Clave única por operación para reintentos seguros")
			@RequestHeader(name = IDEMPOTENCY_KEY, required = false)
			@Size(max = 64, message = "Idempotency-Key admite máximo 64 caracteres") String idempotencyKey,
			@Valid @RequestBody MovimientoRequest request) {
		ResultadoMovimiento resultado = movimientoUseCase.registrar(
				new RegistrarMovimientoCommand(request.numeroCuenta(), request.valor(), idempotencyKey));
		MovimientoResponse body = mapper.toResponse(resultado.movimiento());
		if (resultado.repetido()) {
			return ResponseEntity.ok().header(IDEMPOTENT_REPLAYED, "true").body(body);
		}
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(body.id())
				.toUri();
		return ResponseEntity.created(location).body(body);
	}

	@GetMapping
	@Operation(summary = "Listar movimientos paginados (más recientes primero), opcionalmente de una cuenta")
	public Pagina<MovimientoResponse> listar(
			@RequestParam(required = false) String numeroCuenta,
			@RequestParam(defaultValue = "0") @Min(value = 0, message = "page no puede ser negativo") int page,
			@RequestParam(defaultValue = "20") @Min(value = 1, message = "size mínimo es 1")
			@Max(value = 100, message = "size máximo es 100") int size) {
		return movimientoUseCase.listar(numeroCuenta, page, size).map(mapper::toResponse);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Obtener un movimiento")
	public MovimientoResponse obtener(@PathVariable Long id) {
		return mapper.toResponse(movimientoUseCase.obtener(id));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Corregir el valor del último movimiento de la cuenta")
	public MovimientoResponse corregir(@PathVariable Long id, @Valid @RequestBody MovimientoUpdateRequest request) {
		return mapper.toResponse(movimientoUseCase.corregir(id, request.valor()));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Eliminar el último movimiento de la cuenta y revertir su efecto en el saldo")
	public void eliminar(@PathVariable Long id) {
		movimientoUseCase.eliminar(id);
	}
}
