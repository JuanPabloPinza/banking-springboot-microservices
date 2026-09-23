package git.juanpablopinza.accounts.application.service;

import git.juanpablopinza.accounts.application.port.in.EstadoCuentaReporte;
import git.juanpablopinza.accounts.application.port.in.EstadoCuentaReporte.ClienteReporte;
import git.juanpablopinza.accounts.application.port.in.EstadoCuentaReporte.CuentaReporte;
import git.juanpablopinza.accounts.application.port.in.EstadoCuentaReporte.MovimientoReporte;
import git.juanpablopinza.accounts.application.port.in.ReporteUseCase;
import git.juanpablopinza.accounts.application.port.out.ClienteRefRepositoryPort;
import git.juanpablopinza.accounts.application.port.out.CuentaRepositoryPort;
import git.juanpablopinza.accounts.application.port.out.MovimientoRepositoryPort;
import git.juanpablopinza.accounts.domain.exception.ClienteNoEncontradoException;
import git.juanpablopinza.accounts.domain.exception.DatoInvalidoException;
import git.juanpablopinza.accounts.domain.model.ClienteRef;
import git.juanpablopinza.accounts.domain.model.Cuenta;
import git.juanpablopinza.accounts.domain.model.Movimiento;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import static java.util.stream.Collectors.groupingBy;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReporteService implements ReporteUseCase {

	static final int MAXIMO_DIAS = 366;
	private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2);

	private final ClienteRefRepositoryPort clienteRefRepository;
	private final CuentaRepositoryPort cuentaRepository;
	private final MovimientoRepositoryPort movimientoRepository;

	@Override
	public EstadoCuentaReporte generar(UUID clienteId, LocalDate desde, LocalDate hasta) {
		validarRango(desde, hasta);
		ClienteRef cliente = clienteRefRepository.buscar(clienteId)
				.orElseThrow(() -> new ClienteNoEncontradoException(clienteId));

		Map<String, List<Movimiento>> movimientosPorCuenta = movimientoRepository
				.listarPorClienteEntre(clienteId, desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay())
				.stream()
				.collect(groupingBy(Movimiento::getNumeroCuenta));

		List<CuentaReporte> cuentas = cuentaRepository.listarPorCliente(clienteId).stream()
				.map(cuenta -> cuentaReporte(cuenta,
						movimientosPorCuenta.getOrDefault(cuenta.getNumeroCuenta(), List.of())))
				.toList();

		return new EstadoCuentaReporte(new ClienteReporte(cliente.clienteId(), cliente.nombre()), desde, hasta,
				cuentas);
	}

	private static void validarRango(LocalDate desde, LocalDate hasta) {
		if (desde == null || hasta == null) {
			throw new DatoInvalidoException("El rango de fechas debe tener el formato desde,hasta (yyyy-MM-dd)");
		}
		if (desde.isAfter(hasta)) {
			throw new DatoInvalidoException("La fecha inicial no puede ser posterior a la fecha final");
		}
		if (ChronoUnit.DAYS.between(desde, hasta) >= MAXIMO_DIAS) {
			throw new DatoInvalidoException("El rango de fechas no puede superar " + MAXIMO_DIAS + " días");
		}
	}

	private static CuentaReporte cuentaReporte(Cuenta cuenta, List<Movimiento> movimientos) {
		return new CuentaReporte(cuenta.getNumeroCuenta(), cuenta.getTipoCuenta(), cuenta.isEstado(),
				cuenta.getSaldoInicial(), cuenta.getSaldoDisponible(),
				total(movimientos, valor -> valor.signum() > 0),
				total(movimientos, valor -> valor.signum() < 0),
				movimientos.stream()
						.map(m -> new MovimientoReporte(m.getFecha(), m.getTipoMovimiento(), m.getValor(), m.getSaldo()))
						.toList());
	}

	private static BigDecimal total(List<Movimiento> movimientos, Predicate<BigDecimal> criterio) {
		return movimientos.stream()
				.map(Movimiento::getValor)
				.filter(criterio)
				.reduce(CERO, BigDecimal::add);
	}
}
