package git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cliente_ref")
@Getter
@Setter
public class ClienteRefEntity {

	@Id
	@Column(name = "cliente_id")
	private UUID clienteId;

	@Column(nullable = false, length = 100)
	private String nombre;

	@Column(nullable = false)
	private boolean estado;

	@Column(name = "actualizado_en", nullable = false)
	private Instant actualizadoEn;
}
