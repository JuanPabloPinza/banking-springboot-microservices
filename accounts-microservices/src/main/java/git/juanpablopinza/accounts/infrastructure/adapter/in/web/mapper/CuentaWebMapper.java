package git.juanpablopinza.accounts.infrastructure.adapter.in.web.mapper;

import git.juanpablopinza.accounts.application.port.in.ActualizarCuentaCommand;
import git.juanpablopinza.accounts.application.port.in.ActualizarParcialCuentaCommand;
import git.juanpablopinza.accounts.application.port.in.CrearCuentaCommand;
import git.juanpablopinza.accounts.domain.model.Cuenta;
import git.juanpablopinza.accounts.domain.model.Movimiento;
import git.juanpablopinza.accounts.infrastructure.adapter.in.web.dto.CuentaPatchRequest;
import git.juanpablopinza.accounts.infrastructure.adapter.in.web.dto.CuentaRequest;
import git.juanpablopinza.accounts.infrastructure.adapter.in.web.dto.CuentaResponse;
import git.juanpablopinza.accounts.infrastructure.adapter.in.web.dto.CuentaUpdateRequest;
import git.juanpablopinza.accounts.infrastructure.adapter.in.web.dto.MovimientoResponse;
import org.mapstruct.Mapper;

@Mapper
public interface CuentaWebMapper {

	CrearCuentaCommand toCommand(CuentaRequest request);

	ActualizarCuentaCommand toCommand(CuentaUpdateRequest request);

	ActualizarParcialCuentaCommand toCommand(CuentaPatchRequest request);

	CuentaResponse toResponse(Cuenta cuenta);

	MovimientoResponse toResponse(Movimiento movimiento);
}
