package git.juanpablopinza.clients.domain.model;

import git.juanpablopinza.clients.domain.exception.DatoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClienteTest {

	private static final Identificacion CEDULA = new Identificacion("1710034065");

	private Cliente nuevoCliente() {
		return Cliente.crear("Jose Lema", Genero.MASCULINO, 35, CEDULA, "Otavalo sn y principal", "098254785",
				"hash-1234", true);
	}

	@Test
	@DisplayName("Un cliente nuevo es una Persona, nace activo y con clienteId generado")
	void crearClienteNuevo() {
		Cliente cliente = nuevoCliente();

		assertThat(cliente).isInstanceOf(Persona.class);
		assertThat(cliente.getClienteId()).isNotNull();
		assertThat(cliente.isEstado()).isTrue();
		assertThat(cliente.getNombre()).isEqualTo("Jose Lema");
		assertThat(cliente.getIdentificacion().valor()).isEqualTo("1710034065");
		assertThat(cliente.getContrasena()).isEqualTo("hash-1234");
	}

	@Test
	@DisplayName("Dos clientes nuevos nunca comparten clienteId")
	void clienteIdUnico() {
		assertThat(nuevoCliente().getClienteId()).isNotEqualTo(nuevoCliente().getClienteId());
	}

	@Test
	@DisplayName("Actualizar cambia los datos personales pero conserva clienteId e identificación")
	void actualizarConservaIdentidad() {
		Cliente cliente = nuevoCliente();
		var clienteId = cliente.getClienteId();

		cliente.actualizar("José Lema Andrade", Genero.MASCULINO, 36, "Quito", "0991234567", false);

		assertThat(cliente.getNombre()).isEqualTo("José Lema Andrade");
		assertThat(cliente.getEdad()).isEqualTo(36);
		assertThat(cliente.isEstado()).isFalse();
		assertThat(cliente.getClienteId()).isEqualTo(clienteId);
		assertThat(cliente.getIdentificacion()).isEqualTo(CEDULA);
	}

	@Test
	@DisplayName("Desactivar es un borrado lógico: el cliente sigue existiendo con estado false")
	void desactivar() {
		Cliente cliente = nuevoCliente();

		cliente.desactivar();

		assertThat(cliente.isEstado()).isFalse();
	}

	@Test
	@DisplayName("Un cliente menor de edad no puede existir")
	void rechazaMenorDeEdad() {
		assertThatThrownBy(() -> Cliente.crear("Ana", Genero.FEMENINO, 17, CEDULA, "Quito", "098254785", "hash", true))
				.isInstanceOf(DatoInvalidoException.class)
				.hasMessageContaining("entre 18 y 120");
	}
}
