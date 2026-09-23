package git.juanpablopinza.clients.infrastructure.adapter.in.web;

import git.juanpablopinza.clients.application.port.Pagina;
import git.juanpablopinza.clients.application.port.in.ClienteUseCase;
import git.juanpablopinza.clients.domain.model.Cliente;
import git.juanpablopinza.clients.infrastructure.adapter.in.web.dto.ClientePatchRequest;
import git.juanpablopinza.clients.infrastructure.adapter.in.web.dto.ClienteRequest;
import git.juanpablopinza.clients.infrastructure.adapter.in.web.dto.ClienteResponse;
import git.juanpablopinza.clients.infrastructure.adapter.in.web.dto.ClienteUpdateRequest;
import git.juanpablopinza.clients.infrastructure.adapter.in.web.mapper.ClienteWebMapper;
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
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "CRUD de clientes (F1)")
public class ClienteController {

	private final ClienteUseCase clienteUseCase;
	private final ClienteWebMapper mapper;

	@PostMapping
	@Operation(summary = "Crear un cliente")
	public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody ClienteRequest request) {
		Cliente cliente = clienteUseCase.crear(mapper.toCommand(request));
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{clienteId}")
				.buildAndExpand(cliente.getClienteId())
				.toUri();
		return ResponseEntity.created(location).body(mapper.toResponse(cliente));
	}

	@GetMapping
	@Operation(summary = "Listar clientes paginados")
	public Pagina<ClienteResponse> listar(
			@RequestParam(defaultValue = "0") @Min(value = 0, message = "page no puede ser negativo") int page,
			@RequestParam(defaultValue = "20") @Min(value = 1, message = "size mínimo es 1")
			@Max(value = 100, message = "size máximo es 100") int size) {
		return clienteUseCase.listar(page, size).map(mapper::toResponse);
	}

	@GetMapping("/{clienteId}")
	@Operation(summary = "Obtener un cliente por su clienteId")
	public ClienteResponse obtener(@PathVariable UUID clienteId) {
		return mapper.toResponse(clienteUseCase.obtener(clienteId));
	}

	@PutMapping("/{clienteId}")
	@Operation(summary = "Reemplazar los datos editables de un cliente")
	public ClienteResponse actualizar(@PathVariable UUID clienteId, @Valid @RequestBody ClienteUpdateRequest request) {
		return mapper.toResponse(clienteUseCase.actualizar(clienteId, mapper.toCommand(request)));
	}

	@PatchMapping("/{clienteId}")
	@Operation(summary = "Actualizar parcialmente un cliente (incluye cambio de contraseña)")
	public ClienteResponse actualizarParcial(@PathVariable UUID clienteId,
			@Valid @RequestBody ClientePatchRequest request) {
		return mapper.toResponse(clienteUseCase.actualizarParcial(clienteId, mapper.toCommand(request)));
	}

	@DeleteMapping("/{clienteId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Eliminar (desactivar) un cliente")
	public void eliminar(@PathVariable UUID clienteId) {
		clienteUseCase.eliminar(clienteId);
	}
}
