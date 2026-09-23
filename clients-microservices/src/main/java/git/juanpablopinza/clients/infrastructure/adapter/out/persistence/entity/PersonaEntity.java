package git.juanpablopinza.clients.infrastructure.adapter.out.persistence.entity;

import git.juanpablopinza.clients.domain.model.Genero;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** JOINED: persona y cliente en tablas separadas, unidas por la misma PK. */
@Entity
@Table(name = "persona")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
public abstract class PersonaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String nombre;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Genero genero;

	@Column(nullable = false)
	private Integer edad;

	@Column(nullable = false, length = 10, unique = true, updatable = false)
	private String identificacion;

	@Column(nullable = false, length = 200)
	private String direccion;

	@Column(nullable = false, length = 10)
	private String telefono;
}
