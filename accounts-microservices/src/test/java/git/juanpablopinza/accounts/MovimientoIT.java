package git.juanpablopinza.accounts;

import git.juanpablopinza.accounts.infrastructure.adapter.in.messaging.ClienteEventoMessage;
import git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.entity.CuentaEntity;
import git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.repository.ClienteRefJpaRepository;
import git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.repository.CuentaJpaRepository;
import git.juanpablopinza.accounts.infrastructure.config.RabbitConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MovimientoIT {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private RabbitTemplate rabbitTemplate;
	@Autowired
	private ClienteRefJpaRepository clienteRefRepository;
	@Autowired
	private CuentaJpaRepository cuentaRepository;
	@Autowired
	private Clock clock;

	private UUID publicarClienteCreado(String nombre) {
		UUID clienteId = UUID.randomUUID();
		rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_CLIENTES, "cliente.creado",
				new ClienteEventoMessage(UUID.randomUUID(), "CREADO", clienteId, nombre, true, Instant.now()));
		await().atMost(Duration.ofSeconds(10)).until(() -> clienteRefRepository.existsById(clienteId));
		return clienteId;
	}

	private void crearCuenta(String numeroCuenta, String saldoInicial, UUID clienteId) throws Exception {
		mockMvc.perform(post("/api/cuentas").contentType(MediaType.APPLICATION_JSON).content("""
						{ "numeroCuenta": "%s", "tipoCuenta": "AHORROS", "saldoInicial": %s, "clienteId": "%s" }
						""".formatted(numeroCuenta, saldoInicial, clienteId)))
				.andExpect(status().isCreated());
	}

	private ResultActions registrarMovimiento(String numeroCuenta, String valor) throws Exception {
		return mockMvc.perform(post("/api/movimientos").contentType(MediaType.APPLICATION_JSON).content("""
				{ "numeroCuenta": "%s", "valor": %s }
				""".formatted(numeroCuenta, valor)));
	}

	private CuentaEntity cuenta(String numeroCuenta) {
		return cuentaRepository.findByNumeroCuenta(numeroCuenta).orElseThrow();
	}

	@Test
	@DisplayName("Caso de uso del enunciado: el evento crea la réplica, los movimientos mueven el saldo, sin saldo responde 422 y el reporte lo refleja")
	void flujoDeMovimientos() throws Exception {
		UUID marianela = publicarClienteCreado("Marianela Montalvo");
		crearCuenta("225487", "100", marianela);
		crearCuenta("496825", "540", marianela);

		registrarMovimiento("225487", "600")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tipoMovimiento").value("DEPOSITO"))
				.andExpect(jsonPath("$.saldo").value(700.0));
		registrarMovimiento("496825", "-540")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tipoMovimiento").value("RETIRO"))
				.andExpect(jsonPath("$.saldo").value(0.0));
		registrarMovimiento("496825", "-1")
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value("Saldo no disponible"));

		assertThat(cuenta("225487").getSaldoInicial()).isEqualByComparingTo("100");
		assertThat(cuenta("225487").getSaldoDisponible()).isEqualByComparingTo("700");
		assertThat(cuenta("496825").getSaldoDisponible()).isEqualByComparingTo("0");

		LocalDate hoy = LocalDate.now(clock);
		mockMvc.perform(get("/api/reportes").param("cliente", marianela.toString())
						.param("fecha", hoy.minusDays(1) + "," + hoy))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cliente.nombre").value("Marianela Montalvo"))
				.andExpect(jsonPath("$.cuentas.length()").value(2))
				.andExpect(jsonPath("$.cuentas[0].numeroCuenta").value("225487"))
				.andExpect(jsonPath("$.cuentas[0].totalCreditos").value(600.0))
				.andExpect(jsonPath("$.cuentas[0].movimientos.length()").value(1))
				.andExpect(jsonPath("$.cuentas[1].numeroCuenta").value("496825"))
				.andExpect(jsonPath("$.cuentas[1].totalDebitos").value(-540.0))
				.andExpect(jsonPath("$.cuentas[1].saldoDisponible").value(0.0));
	}

	@Test
	@DisplayName("Dos retiros simultáneos de 60 sobre saldo 100: el bloqueo pesimista deja pasar solo uno")
	void retirosConcurrentes() throws Exception {
		UUID jose = publicarClienteCreado("Jose Lema");
		crearCuenta("478758", "100", jose);

		ExecutorService hilos = Executors.newFixedThreadPool(2);
		CountDownLatch largada = new CountDownLatch(1);
		Callable<Integer> retiro = () -> {
			largada.await();
			return registrarMovimiento("478758", "-60").andReturn().getResponse().getStatus();
		};
		try {
			Future<Integer> primero = hilos.submit(retiro);
			Future<Integer> segundo = hilos.submit(retiro);
			largada.countDown();

			List<Integer> estados = List.of(primero.get(15, TimeUnit.SECONDS), segundo.get(15, TimeUnit.SECONDS));

			assertThat(estados).containsExactlyInAnyOrder(201, 422);
			assertThat(cuenta("478758").getSaldoDisponible()).isEqualByComparingTo("40");
		} finally {
			hilos.shutdownNow();
		}
	}

	@Test
	@DisplayName("Cuando el cliente se elimina (evento con estado false), sus cuentas se desactivan")
	void clienteEliminadoDesactivaCuentas() throws Exception {
		UUID juan = publicarClienteCreado("Juan Osorio");
		crearCuenta("495878", "0", juan);

		rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_CLIENTES, "cliente.eliminado",
				new ClienteEventoMessage(UUID.randomUUID(), "ELIMINADO", juan, "Juan Osorio", false, Instant.now()));

		await().atMost(Duration.ofSeconds(10)).until(() -> !cuenta("495878").isEstado());
	}
}
