package git.juanpablopinza.accounts.infrastructure.adapter.in.web;

import git.juanpablopinza.accounts.application.port.Pagina;
import git.juanpablopinza.accounts.application.port.in.CuentaUseCase;
import git.juanpablopinza.accounts.domain.model.Cuenta;
import git.juanpablopinza.accounts.infrastructure.adapter.in.web.dto.CuentaPatchRequest;
import git.juanpablopinza.accounts.infrastructure.adapter.in.web.dto.CuentaRequest;
import git.juanpablopinza.accounts.infrastructure.adapter.in.web.dto.CuentaResponse;
import git.juanpablopinza.accounts.infrastructure.adapter.in.web.dto.CuentaUpdateRequest;
import git.juanpablopinza.accounts.infrastructure.adapter.in.web.mapper.CuentaWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/cuentas")
@RequiredArgsConstructor
@Tag(name = "Cuentas", description = "CRUD de cuentas (F1)")
public class CuentaController {

	private final CuentaUseCase cuentaUseCase;
	private final CuentaWebMapper mapper;

	@PostMapping
	@Operation(summary = "Crear una cuenta para un cliente existente y activo")
	public ResponseEntity<CuentaResponse> crear(@Valid @RequestBody CuentaRequest request) {
		Cuenta cuenta = cuentaUseCase.crear(mapper.toCommand(request));
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{numeroCuenta}")
				.buildAndExpand(cuenta.getNumeroCuenta())
				.toUri();
		return ResponseEntity.created(location).body(mapper.toResponse(cuenta));
	}

	@GetMapping
	@Operation(summary = "Listar cuentas paginadas, opcionalmente de un cliente")
	public Pagina<CuentaResponse> listar(
			@RequestParam(required = false) UUID clienteId,
			@RequestParam(defaultValue = "0") @Min(value = 0, message = "page no puede ser negativo") int page,
			@RequestParam(defaultValue = "20") @Min(value = 1, message = "size mínimo es 1")
			@Max(value = 100, message = "size máximo es 100") int size) {
		return cuentaUseCase.listar(clienteId, page, size).map(mapper::toResponse);
	}

	@GetMapping("/{numeroCuenta}")
	@Operation(summary = "Obtener una cuenta por su número")
	public CuentaResponse obtener(@PathVariable String numeroCuenta) {
		return mapper.toResponse(cuentaUseCase.obtener(numeroCuenta));
	}

	@PutMapping("/{numeroCuenta}")
	@Operation(summary = "Reemplazar tipo y estado de una cuenta")
	public CuentaResponse actualizar(@PathVariable String numeroCuenta,
			@Valid @RequestBody CuentaUpdateRequest request) {
		return mapper.toResponse(cuentaUseCase.actualizar(numeroCuenta, mapper.toCommand(request)));
	}

	@PatchMapping("/{numeroCuenta}")
	@Operation(summary = "Actualizar parcialmente una cuenta")
	public CuentaResponse actualizarParcial(@PathVariable String numeroCuenta,
			@Valid @RequestBody CuentaPatchRequest request) {
		return mapper.toResponse(cuentaUseCase.actualizarParcial(numeroCuenta, mapper.toCommand(request)));
	}

	@DeleteMapping("/{numeroCuenta}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Eliminar (desactivar) una cuenta; su historial se conserva")
	public void eliminar(@PathVariable String numeroCuenta) {
		cuentaUseCase.eliminar(numeroCuenta);
	}
}
