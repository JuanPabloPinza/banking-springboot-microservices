package git.juanpablopinza.accounts.application.service;

import git.juanpablopinza.accounts.application.port.in.EstadoCuentaReporte;
import git.juanpablopinza.accounts.application.port.in.EstadoCuentaReporte.CuentaReporte;
import git.juanpablopinza.accounts.application.port.out.ClienteRefRepositoryPort;
import git.juanpablopinza.accounts.application.port.out.CuentaRepositoryPort;
import git.juanpablopinza.accounts.application.port.out.MovimientoRepositoryPort;
import git.juanpablopinza.accounts.domain.exception.ClienteNoEncontradoException;
import git.juanpablopinza.accounts.domain.exception.DatoInvalidoException;
import git.juanpablopinza.accounts.domain.model.ClienteRef;
import git.juanpablopinza.accounts.domain.model.Cuenta;
import git.juanpablopinza.accounts.domain.model.Movimiento;
import git.juanpablopinza.accounts.domain.model.TipoCuenta;
import git.juanpablopinza.accounts.domain.model.TipoMovimiento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

	private static final UUID CLIENTE = UUID.randomUUID();
	private static final LocalDate DESDE = LocalDate.of(2026, 9, 1);
	private static final LocalDate HASTA = LocalDate.of(2026, 9, 30);

	@Mock
	private ClienteRefRepositoryPort clienteRefRepository;
	@Mock
	private CuentaRepositoryPort cuentaRepository;
	@Mock
	private MovimientoRepositoryPort movimientoRepository;

	@InjectMocks
	private ReporteService service;

	private static Cuenta cuenta(String numero, TipoCuenta tipo, String saldoInicial, String saldoDisponible) {
		return cuenta(numero, tipo, saldoInicial, saldoDisponible, LocalDateTime.of(2026, 1, 1, 9, 0));
	}

	private static Cuenta cuenta(String numero, TipoCuenta tipo, String saldoInicial, String saldoDisponible,
			LocalDateTime fechaApertura) {
		return new Cuenta(numero, tipo, new BigDecimal(saldoInicial), new BigDecimal(saldoDisponible), true, CLIENTE,
				fechaApertura);
	}

	private static Movimiento movimiento(String numero, int dia, String valor, String saldo) {
		BigDecimal monto = new BigDecimal(valor);
		return new Movimiento(null, numero, LocalDateTime.of(2026, 9, dia, 10, 0), TipoMovimiento.segun(monto), monto,
				new BigDecimal(saldo));
	}

	@Test
	@DisplayName("Consulta el rango semiabierto [desde, hasta+1) y agrupa los movimientos por cuenta con sus totales")
	void agrupaYTotaliza() {
		when(clienteRefRepository.buscar(CLIENTE))
				.thenReturn(Optional.of(new ClienteRef(CLIENTE, "Marianela Montalvo", true, Instant.now())));
		when(cuentaRepository.listarPorCliente(CLIENTE)).thenReturn(List.of(
				cuenta("225487", TipoCuenta.CORRIENTE, "100.00", "700.00"),
				cuenta("496825", TipoCuenta.AHORROS, "540.00", "0.00"),
				cuenta("999999", TipoCuenta.AHORROS, "50.00", "50.00")));
		when(movimientoRepository.listarPorClienteEntre(CLIENTE, LocalDateTime.of(2026, 9, 1, 0, 0),
				LocalDateTime.of(2026, 10, 1, 0, 0))).thenReturn(List.of(
				movimiento("225487", 5, "650.00", "750.00"),
				movimiento("496825", 6, "-540.00", "0.00"),
				movimiento("225487", 30, "-50.00", "700.00")));

		EstadoCuentaReporte reporte = service.generar(CLIENTE, DESDE, HASTA);

		assertThat(reporte.cliente().nombre()).isEqualTo("Marianela Montalvo");
		assertThat(reporte.cuentas()).extracting(CuentaReporte::numeroCuenta)
				.containsExactly("225487", "496825", "999999");

		CuentaReporte corriente = reporte.cuentas().get(0);
		assertThat(corriente.totalCreditos()).isEqualByComparingTo("650");
		assertThat(corriente.totalDebitos()).isEqualByComparingTo("-50");
		assertThat(corriente.movimientos()).hasSize(2);

		assertThat(reporte.cuentas().get(1).totalDebitos()).isEqualByComparingTo("-540");

		CuentaReporte sinMovimientos = reporte.cuentas().get(2);
		assertThat(sinMovimientos.movimientos()).isEmpty();
		assertThat(sinMovimientos.totalCreditos()).isEqualTo(new BigDecimal("0.00"));
		assertThat(sinMovimientos.totalDebitos()).isEqualTo(new BigDecimal("0.00"));
	}

	@Test
	@DisplayName("Los saldos del período salen del último movimiento anterior y del último dentro del rango, no del saldo actual")
	void saldosDelPeriodo() {
		LocalDate febrero = LocalDate.of(2026, 2, 1);
		LocalDate finFebrero = LocalDate.of(2026, 2, 28);
		when(clienteRefRepository.buscar(CLIENTE))
				.thenReturn(Optional.of(new ClienteRef(CLIENTE, "Marianela Montalvo", true, Instant.now())));
		when(cuentaRepository.listarPorCliente(CLIENTE)).thenReturn(List.of(
				cuenta("225487", TipoCuenta.CORRIENTE, "100.00", "900.00"),
				cuenta("999999", TipoCuenta.AHORROS, "50.00", "80.00")));
		when(movimientoRepository.listarUltimosPorClienteAntesDe(CLIENTE, febrero.atStartOfDay()))
				.thenReturn(List.of(new Movimiento(1L, "225487", LocalDateTime.of(2026, 1, 20, 9, 0),
						TipoMovimiento.DEPOSITO, new BigDecimal("200.00"), new BigDecimal("300.00"))));
		when(movimientoRepository.listarPorClienteEntre(CLIENTE, febrero.atStartOfDay(),
				LocalDate.of(2026, 3, 1).atStartOfDay())).thenReturn(List.of(
				new Movimiento(2L, "225487", LocalDateTime.of(2026, 2, 10, 9, 0), TipoMovimiento.DEPOSITO,
						new BigDecimal("150.00"), new BigDecimal("450.00")),
				new Movimiento(3L, "225487", LocalDateTime.of(2026, 2, 28, 23, 59), TipoMovimiento.RETIRO,
						new BigDecimal("-50.00"), new BigDecimal("400.00"))));

		EstadoCuentaReporte reporte = service.generar(CLIENTE, febrero, finFebrero);

		CuentaReporte corriente = reporte.cuentas().get(0);
		assertThat(corriente.saldoInicioPeriodo()).isEqualByComparingTo("300");
		assertThat(corriente.saldoFinPeriodo()).isEqualByComparingTo("400");
		assertThat(corriente.saldoDisponible()).isEqualByComparingTo("900");
		assertThat(corriente.saldoInicioPeriodo().add(corriente.totalCreditos()).add(corriente.totalDebitos()))
				.isEqualByComparingTo(corriente.saldoFinPeriodo());

		CuentaReporte sinHistorial = reporte.cuentas().get(1);
		assertThat(sinHistorial.saldoInicioPeriodo()).isEqualByComparingTo("50");
		assertThat(sinHistorial.saldoFinPeriodo()).isEqualByComparingTo("50");
	}

	@Test
	@DisplayName("Una cuenta abierta después del rango no aparece; una abierta dentro parte de su saldo de apertura")
	void cuentasSegunFechaDeApertura() {
		when(clienteRefRepository.buscar(CLIENTE))
				.thenReturn(Optional.of(new ClienteRef(CLIENTE, "Marianela Montalvo", true, Instant.now())));
		when(cuentaRepository.listarPorCliente(CLIENTE)).thenReturn(List.of(
				cuenta("225487", TipoCuenta.CORRIENTE, "100.00", "100.00", LocalDateTime.of(2026, 9, 30, 23, 59, 59)),
				cuenta("496825", TipoCuenta.AHORROS, "540.00", "540.00", LocalDateTime.of(2026, 10, 1, 0, 0))));

		EstadoCuentaReporte reporte = service.generar(CLIENTE, DESDE, HASTA);

		assertThat(reporte.cuentas()).extracting(CuentaReporte::numeroCuenta).containsExactly("225487");
		CuentaReporte abierta = reporte.cuentas().get(0);
		assertThat(abierta.fechaApertura()).isEqualTo(LocalDateTime.of(2026, 9, 30, 23, 59, 59));
		assertThat(abierta.saldoInicioPeriodo()).isEqualByComparingTo("100");
		assertThat(abierta.saldoFinPeriodo()).isEqualByComparingTo("100");
	}

	@Test
	@DisplayName("Un rango invertido es una solicitud inválida y no consulta nada")
	void rangoInvertido() {
		assertThatThrownBy(() -> service.generar(CLIENTE, HASTA, DESDE))
				.isInstanceOf(DatoInvalidoException.class)
				.hasMessageContaining("posterior");
		verifyNoInteractions(clienteRefRepository, cuentaRepository, movimientoRepository);
	}

	@Test
	@DisplayName("Admite hasta 366 días (ambos inclusive) y rechaza 367")
	void rangoMaximo() {
		LocalDate inicio = LocalDate.of(2024, 1, 1);
		when(clienteRefRepository.buscar(CLIENTE)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.generar(CLIENTE, inicio, inicio.plusDays(365)))
				.isInstanceOf(ClienteNoEncontradoException.class);
		assertThatThrownBy(() -> service.generar(CLIENTE, inicio, inicio.plusDays(366)))
				.isInstanceOf(DatoInvalidoException.class)
				.hasMessageContaining("366");
	}

	@Test
	@DisplayName("Una fecha vacía en el rango es una solicitud inválida")
	void fechaAusente() {
		assertThatThrownBy(() -> service.generar(CLIENTE, DESDE, null))
				.isInstanceOf(DatoInvalidoException.class);
	}
}
