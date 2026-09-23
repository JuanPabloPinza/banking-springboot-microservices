package git.juanpablopinza.clients;

import git.juanpablopinza.clients.infrastructure.adapter.out.persistence.entity.ClienteEntity;
import git.juanpablopinza.clients.infrastructure.adapter.out.persistence.repository.ClienteJpaRepository;
import git.juanpablopinza.clients.infrastructure.config.RabbitConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ClienteIT {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ClienteJpaRepository clienteJpaRepository;
	@Autowired
	private AmqpAdmin amqpAdmin;
	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Test
	@DisplayName("Crear un cliente lo persiste (herencia JOINED, contraseña con BCrypt) y publica cliente.creado")
	void crearClientePersisteYPublicaEvento() throws Exception {
		Queue cola = new AnonymousQueue();
		amqpAdmin.declareQueue(cola);
		amqpAdmin.declareBinding(BindingBuilder.bind(cola)
				.to(new TopicExchange(RabbitConfig.EXCHANGE_CLIENTES)).with("cliente.#"));

		String location = mockMvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content("""
						{
						  "nombre": "Marianela Montalvo",
						  "genero": "FEMENINO",
						  "edad": 30,
						  "identificacion": "1712345675",
						  "direccion": "Amazonas y NNUU",
						  "telefono": "097548965",
						  "contrasena": "5678"
						}
						"""))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getHeader("Location");
		UUID clienteId = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));

		ClienteEntity guardado = clienteJpaRepository.findByClienteId(clienteId).orElseThrow();
		assertThat(guardado.getNombre()).isEqualTo("Marianela Montalvo");
		assertThat(guardado.isEstado()).isTrue();
		assertThat(guardado.getContrasena()).isNotEqualTo("5678").startsWith("$2");

		Message mensaje = rabbitTemplate.receive(cola.getName(), 5_000);
		assertThat(mensaje).isNotNull();
		assertThat(mensaje.getMessageProperties().getReceivedRoutingKey()).isEqualTo("cliente.creado");
		assertThat(new String(mensaje.getBody(), StandardCharsets.UTF_8))
				.contains(clienteId.toString())
				.contains("Marianela Montalvo");
	}
}
