package git.juanpablopinza.clients.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "cliente")
@Getter
@Setter
public class ClienteEntity extends PersonaEntity {

	@Column(name = "cliente_id", nullable = false, unique = true, updatable = false)
	private UUID clienteId;

	@Column(nullable = false, length = 100)
	private String contrasena;

	@Column(nullable = false)
	private boolean estado;
}
