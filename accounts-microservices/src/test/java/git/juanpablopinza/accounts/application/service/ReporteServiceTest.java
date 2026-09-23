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
		return new Cuenta(numero, tipo, new BigDecimal(saldoInicial), new BigDecimal(saldoDisponible), true, CLIENTE);
	}

	private static Movimiento movimiento(String numero, int dia, String valor, String saldo) {
		BigDecimal monto = new BigDecimal(valor);
		return new Movimiento(null, numero, LocalDateTime.of(2026, 9, dia, 10, 0), TipoMovimiento.segun(monto), monto,
				new BigDecimal(saldo), null);
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
