package git.juanpablopinza.accounts.application.service;

import git.juanpablopinza.accounts.application.port.Pagina;
import git.juanpablopinza.accounts.application.port.in.MovimientoUseCase;
import git.juanpablopinza.accounts.application.port.in.RegistrarMovimientoCommand;
import git.juanpablopinza.accounts.application.port.in.ResultadoMovimiento;
import git.juanpablopinza.accounts.application.port.out.CuentaRepositoryPort;
import git.juanpablopinza.accounts.application.port.out.MovimientoRepositoryPort;
import git.juanpablopinza.accounts.domain.exception.CuentaNoEncontradaException;
import git.juanpablopinza.accounts.domain.exception.IdempotencyKeyReutilizadaException;
import git.juanpablopinza.accounts.domain.exception.MovimientoNoEncontradoException;
import git.juanpablopinza.accounts.domain.exception.MovimientoNoModificableException;
import git.juanpablopinza.accounts.domain.model.Cuenta;
import git.juanpablopinza.accounts.domain.model.Movimiento;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class MovimientoService implements MovimientoUseCase {

	private final CuentaRepositoryPort cuentaRepository;
	private final MovimientoRepositoryPort movimientoRepository;
	private final Clock clock;


	@Override
	public ResultadoMovimiento registrar(RegistrarMovimientoCommand command) {
		Cuenta cuenta = bloquear(command.numeroCuenta());

		Optional<Movimiento> previo = Optional.ofNullable(command.idempotencyKey())
				.flatMap(movimientoRepository::buscarPorIdempotencyKey);
		if (previo.isPresent()) {
			if (!previo.get().esMismaSolicitud(command.numeroCuenta(), command.valor())) {
				throw new IdempotencyKeyReutilizadaException(command.idempotencyKey());
			}
			return new ResultadoMovimiento(previo.get(), true);
		}

		Movimiento movimiento = cuenta.registrarMovimiento(command.valor(),
				LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS), command.idempotencyKey());
		cuentaRepository.guardar(cuenta);
		return new ResultadoMovimiento(movimientoRepository.guardar(movimiento), false);
	}

	@Override
	@Transactional(readOnly = true)
	public Movimiento obtener(Long id) {
		return buscar(id);
	}

	@Override
	@Transactional(readOnly = true)
	public Pagina<Movimiento> listar(String numeroCuenta, int pagina, int tamanio) {
		return movimientoRepository.listar(numeroCuenta, pagina, tamanio);
	}

	@Override
	public Movimiento corregir(Long id, BigDecimal nuevoValor) {
		Movimiento movimiento = buscar(id);
		Cuenta cuenta = bloquear(movimiento.getNumeroCuenta());
		validarEsUltimo(movimiento);
		cuenta.corregirUltimoMovimiento(movimiento, nuevoValor);
		cuentaRepository.guardar(cuenta);
		return movimientoRepository.guardar(movimiento);
	}

	@Override
	public void eliminar(Long id) {
		Movimiento movimiento = buscar(id);
		Cuenta cuenta = bloquear(movimiento.getNumeroCuenta());
		validarEsUltimo(movimiento);
		cuenta.revertirUltimoMovimiento(movimiento);
		cuentaRepository.guardar(cuenta);
		movimientoRepository.eliminar(id);
	}

	private Cuenta bloquear(String numeroCuenta) {
		return cuentaRepository.buscarPorNumeroParaActualizar(numeroCuenta)
				.orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta));
	}

	private Movimiento buscar(Long id) {
		return movimientoRepository.buscarPorId(id).orElseThrow(() -> new MovimientoNoEncontradoException(id));
	}

	private void validarEsUltimo(Movimiento movimiento) {
		boolean esUltimo = movimientoRepository.buscarUltimo(movimiento.getNumeroCuenta())
				.map(ultimo -> ultimo.getId().equals(movimiento.getId()))
				.orElse(false);
		if (!esUltimo) {
			throw new MovimientoNoModificableException(movimiento.getId());
		}
	}
}
