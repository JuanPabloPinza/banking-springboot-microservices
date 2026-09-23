package git.juanpablopinza.clients.infrastructure.adapter.in.web.mapper;

import git.juanpablopinza.clients.application.port.in.ActualizarClienteCommand;
import git.juanpablopinza.clients.application.port.in.ActualizarParcialClienteCommand;
import git.juanpablopinza.clients.application.port.in.CrearClienteCommand;
import git.juanpablopinza.clients.domain.model.Cliente;
import git.juanpablopinza.clients.domain.model.Identificacion;
import git.juanpablopinza.clients.infrastructure.adapter.in.web.dto.ClientePatchRequest;
import git.juanpablopinza.clients.infrastructure.adapter.in.web.dto.ClienteRequest;
import git.juanpablopinza.clients.infrastructure.adapter.in.web.dto.ClienteResponse;
import git.juanpablopinza.clients.infrastructure.adapter.in.web.dto.ClienteUpdateRequest;
import org.mapstruct.Mapper;

@Mapper
public interface ClienteWebMapper {

	CrearClienteCommand toCommand(ClienteRequest request);

	ActualizarClienteCommand toCommand(ClienteUpdateRequest request);

	ActualizarParcialClienteCommand toCommand(ClientePatchRequest request);

	ClienteResponse toResponse(Cliente cliente);

	default String toValor(Identificacion identificacion) {
		return identificacion.valor();
	}
}
