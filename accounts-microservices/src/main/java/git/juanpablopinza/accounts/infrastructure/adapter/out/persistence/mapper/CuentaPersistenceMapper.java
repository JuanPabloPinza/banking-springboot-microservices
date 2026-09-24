package git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.mapper;

import git.juanpablopinza.accounts.domain.model.ClienteRef;
import git.juanpablopinza.accounts.domain.model.Cuenta;
import git.juanpablopinza.accounts.domain.model.Movimiento;
import git.juanpablopinza.accounts.domain.model.SolicitudIdempotente;
import git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.entity.ClienteRefEntity;
import git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.entity.CuentaEntity;
import git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.entity.MovimientoEntity;
import git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.entity.SolicitudIdempotenteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface CuentaPersistenceMapper {

	Cuenta toDomain(CuentaEntity entity);

	@Mapping(target = "id", ignore = true)
	CuentaEntity toEntity(Cuenta cuenta);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "numeroCuenta", ignore = true)
	@Mapping(target = "saldoInicial", ignore = true)
	@Mapping(target = "clienteId", ignore = true)
	@Mapping(target = "fechaApertura", ignore = true)
	void actualizar(Cuenta cuenta, @MappingTarget CuentaEntity entity);

	@Mapping(target = "numeroCuenta", source = "cuenta.numeroCuenta")
	Movimiento toDomain(MovimientoEntity entity);

	@Mapping(target = "cuenta", ignore = true)
	MovimientoEntity toEntity(Movimiento movimiento);

	ClienteRef toDomain(ClienteRefEntity entity);

	ClienteRefEntity toEntity(ClienteRef clienteRef);

	SolicitudIdempotente toDomain(SolicitudIdempotenteEntity entity);

	SolicitudIdempotenteEntity toEntity(SolicitudIdempotente solicitud);
}
