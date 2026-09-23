package git.juanpablopinza.clients.infrastructure.adapter.out.persistence.mapper;

import git.juanpablopinza.clients.domain.model.Cliente;
import git.juanpablopinza.clients.domain.model.Identificacion;
import git.juanpablopinza.clients.infrastructure.adapter.out.persistence.entity.ClienteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface ClientePersistenceMapper {

	Cliente toDomain(ClienteEntity entity);

	@Mapping(target = "id", ignore = true)
	ClienteEntity toEntity(Cliente cliente);

	/** Actualiza la entidad administrada; clienteId e identificación nunca cambian. */
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "clienteId", ignore = true)
	@Mapping(target = "identificacion", ignore = true)
	void actualizar(Cliente cliente, @MappingTarget ClienteEntity entity);

	default Identificacion toIdentificacion(String valor) {
		return new Identificacion(valor);
	}

	default String toValor(Identificacion identificacion) {
		return identificacion.valor();
	}
}
