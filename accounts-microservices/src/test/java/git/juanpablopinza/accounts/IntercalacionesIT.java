package git.juanpablopinza.accounts;

import com.jayway.jsonpath.JsonPath;
import git.juanpablopinza.accounts.application.port.out.ClienteRefRepositoryPort;
import git.juanpablopinza.accounts.application.port.out.MovimientoRepositoryPort;
import git.juanpablopinza.accounts.application.port.out.SolicitudIdempotenteRepositoryPort;
import git.juanpablopinza.accounts.infrastructure.adapter.in.messaging.ClienteEventoMessage;
import git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.entity.CuentaEntity;
import git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.repository.ClienteRefJpaRepository;
import git.juanpablopinza.accounts.infrastructure.adapter.out.persistence.repository.CuentaJpaRepository;
import git.juanpablopinza.accounts.infrastructure.config.RabbitConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class IntercalacionesIT {

	private static final Duration ESPERA = Duration.ofSeconds(15);

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private RabbitTemplate rabbitTemplate;
	@Autowired
	private ClienteRefJpaRepository clienteRefRepository;
	@Autowired
	private CuentaJpaRepository cuentaRepository;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private Clock clock;

	@MockitoSpyBean
	private SolicitudIdempotenteRepositoryPort solicitudPort;
	@MockitoSpyBean
	private ClienteRefRepositoryPort clienteRefPort;
	@MockitoSpyBean
	private MovimientoRepositoryPort movimientoPort;

	private final ExecutorService segundoPlano = Executors.newFixedThreadPool(2);

	@AfterEach
	void detenerSegundoPlano() {
		segundoPlano.shutdownNow();
	}

	private final class Pausa implements Answer<Object> {

		private final CountDownLatch alcanzada = new CountDownLatch(1);
		private final CountDownLatch liberada = new CountDownLatch(1);
		private final AtomicBoolean pendiente = new AtomicBoolean(true);
		private volatile int sesion;

		@Override
		public Object answer(InvocationOnMock invocacion) throws Throwable {
			Object resultado = invocacion.callRealMethod();
			if (pendiente.compareAndSet(true, false)) {
				sesion = jdbcTemplate.queryForObject("select pg_backend_pid()", Integer.class);
				alcanzada.countDown();
				if (!liberada.await(30, TimeUnit.SECONDS)) {
					throw new IllegalStateException("La transacción pausada no se liberó a tiempo");
				}
			}
			return resultado;
		}

		void esperarQueSeAlcance() throws InterruptedException {
			assertThat(alcanzada.await(30, TimeUnit.SECONDS)).as("la transacción llegó a la pausa").isTrue();
		}

		void esperarSesionesBloqueadas(int cantidad) {
			await().atMost(ESPERA).until(() -> jdbcTemplate.queryForObject(
					"select count(*) from pg_stat_activity where ? = any(pg_blocking_pids(pid))",
					Integer.class, sesion) >= cantidad);
		}

		void liberar() {
			liberada.countDown();
		}
	}

	private UUID publicarCliente(String nombre) {
		UUID clienteId = UUID.randomUUID();
		rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_CLIENTES, "cliente.creado",
				new ClienteEventoMessage(UUID.randomUUID(), "CREADO", clienteId, nombre, true, Instant.now()));
		await().atMost(ESPERA).until(() -> clienteRefRepository.existsById(clienteId));
		return clienteId;
	}

	private void publicarBaja(UUID clienteId) {
		rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_CLIENTES, "cliente.eliminado",
				new ClienteEventoMessage(UUID.randomUUID(), "ELIMINADO", clienteId, "Cliente", false, Instant.now()));
	}

	private ResultActions crearCuenta(String numeroCuenta, String saldoInicial, UUID clienteId) throws Exception {
		return mockMvc.perform(post("/api/cuentas").with(jwt()).contentType(MediaType.APPLICATION_JSON).content("""
				{ "numeroCuenta": "%s", "tipoCuenta": "AHORROS", "saldoInicial": %s, "clienteId": "%s" }
				""".formatted(numeroCuenta, saldoInicial, clienteId)));
	}

	private ResultActions cambiarEstado(String numeroCuenta, boolean estado) throws Exception {
		return mockMvc.perform(patch("/api/cuentas/" + numeroCuenta).with(jwt())
				.contentType(MediaType.APPLICATION_JSON).content("{ \"estado\": %s }".formatted(estado)));
	}

	private ResultActions registrar(String numeroCuenta, String valor, String clave) throws Exception {
		MockHttpServletRequestBuilder peticion = post("/api/movimientos").with(jwt())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"numeroCuenta\": \"%s\", \"valor\": %s }".formatted(numeroCuenta, valor));
		return mockMvc.perform(clave == null ? peticion : peticion.header("Idempotency-Key", clave));
	}

	private ResultActions corregir(Long id, String valor) throws Exception {
		return mockMvc.perform(put("/api/movimientos/" + id).with(jwt())
				.contentType(MediaType.APPLICATION_JSON).content("{ \"valor\": %s }".formatted(valor)));
	}

	private ResultActions reporte(UUID clienteId, LocalDate desde, LocalDate hasta) throws Exception {
		return mockMvc.perform(get("/api/reportes").with(jwt())
				.param("cliente", clienteId.toString()).param("fecha", desde + "," + hasta));
	}

	private static Long id(ResultActions resultado) throws Exception {
		return Long.valueOf(JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id").toString());
	}

	private static BigDecimal decimal(String json, String ruta) {
		return new BigDecimal(JsonPath.read(json, ruta).toString());
	}

	private CuentaEntity cuenta(String numeroCuenta) {
		return cuentaRepository.findByNumeroCuenta(numeroCuenta).orElseThrow();
	}

	private int movimientosDe(String numeroCuenta) {
		return jdbcTemplate.queryForObject("""
				select count(*) from cuentas.movimiento m join cuentas.cuenta c on c.id = m.cuenta_id
				where c.numero_cuenta = ?""", Integer.class, numeroCuenta);
	}

	@Test
	@DisplayName("Misma Idempotency-Key en dos cuentas: B la consulta antes del commit de A y la guarda después; B se revierte entera")
	void claveIdempotenteEnCarrera() throws Exception {
		UUID cliente = publicarCliente("Cliente Idempotencia");
		crearCuenta("600001", "1000", cliente).andExpect(status().isCreated());
		crearCuenta("600002", "1000", cliente).andExpect(status().isCreated());
		Pausa pausa = new Pausa();
		doAnswer(pausa).when(solicitudPort).buscar("clave-en-carrera");

		Future<ResultActions> b = segundoPlano.submit(() -> registrar("600001", "100", "clave-en-carrera"));
		pausa.esperarQueSeAlcance();
		registrar("600002", "50", "clave-en-carrera").andExpect(status().isCreated());
		pausa.liberar();

		b.get(30, TimeUnit.SECONDS)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("IDEMPOTENCY_KEY_REUTILIZADA"));
		assertThat(cuenta("600001").getSaldoDisponible()).isEqualByComparingTo("1000");
		assertThat(movimientosDe("600001")).isZero();
		assertThat(cuenta("600002").getSaldoDisponible()).isEqualByComparingTo("1050");
		registrar("600002", "50", "clave-en-carrera")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.numeroCuenta").value("600002"));
	}

	@Test
	@DisplayName("Reintento con la misma Idempotency-Key mientras la original sigue en curso: espera y devuelve el mismo movimiento")
	void reintentoMientrasLaOriginalSigueEnCurso() throws Exception {
		UUID cliente = publicarCliente("Cliente Reintento");
		crearCuenta("600007", "1000", cliente).andExpect(status().isCreated());
		Pausa pausa = new Pausa();
		doAnswer(pausa).when(solicitudPort).buscar("clave-reintento");

		Future<ResultActions> original = segundoPlano.submit(() -> registrar("600007", "100", "clave-reintento"));
		pausa.esperarQueSeAlcance();
		Future<ResultActions> reintento = segundoPlano.submit(() -> registrar("600007", "100", "clave-reintento"));
		pausa.esperarSesionesBloqueadas(1);
		pausa.liberar();

		Long movimiento = id(original.get(30, TimeUnit.SECONDS).andExpect(status().isCreated()));
		reintento.get(30, TimeUnit.SECONDS)
				.andExpect(status().isOk())
				.andExpect(header().string("Idempotent-Replayed", "true"))
				.andExpect(jsonPath("$.id").value(movimiento));
		assertThat(cuenta("600007").getSaldoDisponible()).isEqualByComparingTo("1100");
		assertThat(movimientosDe("600007")).isEqualTo(1);
	}

	@Test
	@DisplayName("Reactivar una cuenta mientras llega la baja del cliente: la baja espera al bloqueo del cliente y la desactiva")
	void reactivacionDuranteLaBaja() throws Exception {
		UUID cliente = publicarCliente("Cliente Reactivación");
		crearCuenta("600003", "100", cliente).andExpect(status().isCreated());
		cambiarEstado("600003", false).andExpect(status().isOk());
		Pausa pausa = new Pausa();
		doAnswer(pausa).when(clienteRefPort).buscarConBloqueoCompartido(cliente);

		Future<ResultActions> reactivacion = segundoPlano.submit(() -> cambiarEstado("600003", true));
		pausa.esperarQueSeAlcance();
		publicarBaja(cliente);
		pausa.esperarSesionesBloqueadas(1);
		pausa.liberar();

		reactivacion.get(30, TimeUnit.SECONDS).andExpect(status().isOk());
		await().atMost(ESPERA).until(() -> !cuenta("600003").isEstado());
		assertThat(clienteRefRepository.findById(cliente).orElseThrow().isEstado()).isFalse();
	}

	@Test
	@DisplayName("Crear una cuenta mientras llega la baja del cliente: la baja espera y también desactiva la cuenta nueva")
	void creacionDuranteLaBaja() throws Exception {
		UUID cliente = publicarCliente("Cliente Creación");
		Pausa pausa = new Pausa();
		doAnswer(pausa).when(clienteRefPort).buscarConBloqueoCompartido(cliente);

		Future<ResultActions> creacion = segundoPlano.submit(() -> crearCuenta("600004", "100", cliente));
		pausa.esperarQueSeAlcance();
		publicarBaja(cliente);
		pausa.esperarSesionesBloqueadas(1);
		pausa.liberar();

		creacion.get(30, TimeUnit.SECONDS).andExpect(status().isCreated());
		await().atMost(ESPERA).until(() -> !cuenta("600004").isEstado());
	}

	@Test
	@DisplayName("Si la baja bloquea primero al cliente, la creación y la reactivación esperan y después la rechazan con 422")
	void bajaAntesDeCrearYReactivar() throws Exception {
		UUID cliente = publicarCliente("Cliente Baja Primero");
		crearCuenta("600008", "100", cliente).andExpect(status().isCreated());
		cambiarEstado("600008", false).andExpect(status().isOk());
		Pausa pausa = new Pausa();
		doAnswer(pausa).when(clienteRefPort).buscarParaActualizar(cliente);

		publicarBaja(cliente);
		pausa.esperarQueSeAlcance();
		Future<ResultActions> creacion = segundoPlano.submit(() -> crearCuenta("600009", "100", cliente));
		Future<ResultActions> reactivacion = segundoPlano.submit(() -> cambiarEstado("600008", true));
		pausa.esperarSesionesBloqueadas(2);
		pausa.liberar();

		creacion.get(30, TimeUnit.SECONDS)
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.codigo").value("CLIENTE_INACTIVO"));
		reactivacion.get(30, TimeUnit.SECONDS)
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.codigo").value("CLIENTE_INACTIVO"));
		assertThat(cuentaRepository.findByNumeroCuenta("600009")).isEmpty();
		assertThat(cuenta("600008").isEstado()).isFalse();
	}

	@Test
	@DisplayName("Si la baja se confirma entre ubicar el movimiento y bloquear la cuenta, la corrección ve la cuenta inactiva")
	void correccionTrasBajaConcurrente() throws Exception {
		UUID cliente = publicarCliente("Cliente Corrección");
		crearCuenta("600005", "100", cliente).andExpect(status().isCreated());
		Long movimiento = id(registrar("600005", "50", null).andExpect(status().isCreated()));
		Pausa pausa = new Pausa();
		doAnswer(pausa).when(movimientoPort).buscarNumeroCuenta(movimiento);

		Future<ResultActions> correccion = segundoPlano.submit(() -> corregir(movimiento, "80"));
		pausa.esperarQueSeAlcance();
		publicarBaja(cliente);
		await().atMost(ESPERA).until(() -> !cuenta("600005").isEstado());
		pausa.liberar();

		correccion.get(30, TimeUnit.SECONDS)
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.codigo").value("CUENTA_INACTIVA"));
		assertThat(cuenta("600005").isEstado()).isFalse();
		assertThat(cuenta("600005").getSaldoDisponible()).isEqualByComparingTo("150");
	}

	@Test
	@DisplayName("Si el movimiento se elimina entre ubicarlo y bloquear la cuenta, la corrección responde 404 sin tocar el saldo")
	void movimientoEliminadoDuranteLaCorreccion() throws Exception {
		UUID cliente = publicarCliente("Cliente Eliminación");
		crearCuenta("600010", "100", cliente).andExpect(status().isCreated());
		Long movimiento = id(registrar("600010", "50", null).andExpect(status().isCreated()));
		Pausa pausa = new Pausa();
		doAnswer(pausa).when(movimientoPort).buscarNumeroCuenta(movimiento);

		Future<ResultActions> correccion = segundoPlano.submit(() -> corregir(movimiento, "80"));
		pausa.esperarQueSeAlcance();
		mockMvc.perform(delete("/api/movimientos/" + movimiento).with(jwt())).andExpect(status().isNoContent());
		pausa.liberar();

		correccion.get(30, TimeUnit.SECONDS)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("MOVIMIENTO_NO_ENCONTRADO"));
		assertThat(cuenta("600010").getSaldoDisponible()).isEqualByComparingTo("100");
		assertThat(movimientosDe("600010")).isZero();
	}

	@Test
	@DisplayName("Si se registra otro movimiento entre ubicarlo y bloquear la cuenta, la corrección se rechaza porque ya no es el último")
	void nuevoMovimientoDuranteLaCorreccion() throws Exception {
		UUID cliente = publicarCliente("Cliente Nuevo Movimiento");
		crearCuenta("600011", "100", cliente).andExpect(status().isCreated());
		Long movimiento = id(registrar("600011", "50", null).andExpect(status().isCreated()));
		Pausa pausa = new Pausa();
		doAnswer(pausa).when(movimientoPort).buscarNumeroCuenta(movimiento);

		Future<ResultActions> correccion = segundoPlano.submit(() -> corregir(movimiento, "80"));
		pausa.esperarQueSeAlcance();
		registrar("600011", "30", null).andExpect(status().isCreated());
		pausa.liberar();

		correccion.get(30, TimeUnit.SECONDS)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("MOVIMIENTO_NO_MODIFICABLE"));
		assertThat(cuenta("600011").getSaldoDisponible()).isEqualByComparingTo("180");
		mockMvc.perform(get("/api/movimientos/" + movimiento).with(jwt()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valor").value(50.0));
	}

	@Test
	@DisplayName("El reporte lee una sola instantánea aunque se eliminen y corrijan movimientos mientras se genera")
	void reporteConModificacionesConcurrentes() throws Exception {
		UUID cliente = publicarCliente("Cliente Reporte");
		crearCuenta("600006", "100", cliente).andExpect(status().isCreated());
		jdbcTemplate.update("update cuentas.cuenta set fecha_apertura = fecha_apertura - interval '60 days' "
				+ "where numero_cuenta = '600006'");
		Long anterior = id(registrar("600006", "10", null).andExpect(status().isCreated()));
		jdbcTemplate.update("update cuentas.movimiento set fecha = fecha - interval '40 days' where id = ?", anterior);
		Long enPeriodo = id(registrar("600006", "20", null).andExpect(status().isCreated()));
		LocalDate hoy = LocalDate.now(clock);
		Pausa pausa = new Pausa();
		doAnswer(pausa).when(movimientoPort).listarPorClienteEntre(eq(cliente), any(), any());

		Future<ResultActions> reporte = segundoPlano.submit(() -> reporte(cliente, hoy.minusDays(10), hoy));
		pausa.esperarQueSeAlcance();
		mockMvc.perform(delete("/api/movimientos/" + enPeriodo).with(jwt())).andExpect(status().isNoContent());
		corregir(anterior, "30").andExpect(status().isOk());
		pausa.liberar();

		String cuerpo = reporte.get(30, TimeUnit.SECONDS).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		BigDecimal inicio = decimal(cuerpo, "$.cuentas[0].saldoInicioPeriodo");
		BigDecimal creditos = decimal(cuerpo, "$.cuentas[0].totalCreditos");
		BigDecimal debitos = decimal(cuerpo, "$.cuentas[0].totalDebitos");
		BigDecimal fin = decimal(cuerpo, "$.cuentas[0].saldoFinPeriodo");
		assertThat(inicio).isEqualByComparingTo("110");
		assertThat(creditos).isEqualByComparingTo("20");
		assertThat(fin).isEqualByComparingTo("130");
		assertThat(inicio.add(creditos).add(debitos)).isEqualByComparingTo(fin);
	}

	@Test
	@DisplayName("Un reloj que retrocede no altera el reporte: los movimientos siguen el orden en que se registraron")
	void relojQueRetrocede() throws Exception {
		UUID cliente = publicarCliente("Cliente Reloj");
		crearCuenta("600012", "100", cliente).andExpect(status().isCreated());
		registrar("600012", "10", null).andExpect(status().isCreated());
		Long segundo = id(registrar("600012", "20", null).andExpect(status().isCreated()));
		jdbcTemplate.update("update cuentas.movimiento set fecha = fecha - interval '1 minute' where id = ?", segundo);
		LocalDate hoy = LocalDate.now(clock);

		reporte(cliente, hoy.minusDays(1), hoy)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cuentas[0].movimientos[0].valor").value(10.0))
				.andExpect(jsonPath("$.cuentas[0].movimientos[1].valor").value(20.0))
				.andExpect(jsonPath("$.cuentas[0].saldoFinPeriodo").value(130.0));
	}
}
