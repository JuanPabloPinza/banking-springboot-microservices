package git.juanpablopinza.accounts.application.service;

import git.juanpablopinza.accounts.application.port.Pagina;
import git.juanpablopinza.accounts.application.port.in.ActualizarCuentaCommand;
import git.juanpablopinza.accounts.application.port.in.ActualizarParcialCuentaCommand;
import git.juanpablopinza.accounts.application.port.in.CrearCuentaCommand;
import git.juanpablopinza.accounts.application.port.in.CuentaUseCase;
import git.juanpablopinza.accounts.application.port.out.ClienteRefRepositoryPort;
import git.juanpablopinza.accounts.application.port.out.CuentaRepositoryPort;
import git.juanpablopinza.accounts.domain.exception.ClienteInactivoException;
import git.juanpablopinza.accounts.domain.exception.ClienteNoEncontradoException;
import git.juanpablopinza.accounts.domain.exception.CuentaNoEncontradaException;
import git.juanpablopinza.accounts.domain.exception.NumeroCuentaDuplicadoException;
import git.juanpablopinza.accounts.domain.model.ClienteRef;
import git.juanpablopinza.accounts.domain.model.Cuenta;
import git.juanpablopinza.accounts.domain.model.TipoCuenta;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static java.util.Objects.requireNonNullElse;

@Service
@Transactional
@RequiredArgsConstructor
public class CuentaService implements CuentaUseCase {

	private final CuentaRepositoryPort cuentaRepository;
	private final ClienteRefRepositoryPort clienteRefRepository;
	private final Clock clock;

	@Override
	public Cuenta crear(CrearCuentaCommand command) {
		validarActivo(bloquearCliente(command.clienteId()));
		if (cuentaRepository.existePorNumero(command.numeroCuenta())) {
			throw new NumeroCuentaDuplicadoException(command.numeroCuenta());
		}
		Cuenta cuenta = Cuenta.abrir(command.numeroCuenta(), command.tipoCuenta(), command.saldoInicial(),
				requireNonNullElse(command.estado(), true), command.clienteId(),
				LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS));
		return cuentaRepository.crear(cuenta);
	}

	@Override
	@Transactional(readOnly = true)
	public Cuenta obtener(String numeroCuenta) {
		return cuentaRepository.buscarPorNumero(numeroCuenta)
				.orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta));
	}

	@Override
	@Transactional(readOnly = true)
	public Pagina<Cuenta> listar(UUID clienteId, int pagina, int tamanio) {
		return cuentaRepository.listar(clienteId, pagina, tamanio);
	}

	@Override
	public Cuenta actualizar(String numeroCuenta, ActualizarCuentaCommand command) {
		return aplicarCambios(numeroCuenta, command.tipoCuenta(), command.estado());
	}

	@Override
	public Cuenta actualizarParcial(String numeroCuenta, ActualizarParcialCuentaCommand command) {
		return aplicarCambios(numeroCuenta, command.tipoCuenta(), command.estado());
	}

	@Override
	public void eliminar(String numeroCuenta) {
		Cuenta cuenta = bloquear(numeroCuenta);
		cuenta.desactivar();
		cuentaRepository.guardar(cuenta);
	}

	private Cuenta aplicarCambios(String numeroCuenta, TipoCuenta tipoCuenta, Boolean estado) {
		UUID clienteId = cuentaRepository.buscarClienteId(numeroCuenta)
				.orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta));
		ClienteRef cliente = bloquearCliente(clienteId);
		Cuenta cuenta = bloquear(numeroCuenta);
		boolean nuevoEstado = requireNonNullElse(estado, cuenta.isEstado());
		if (nuevoEstado) {
			validarActivo(cliente);
		}
		cuenta.actualizar(requireNonNullElse(tipoCuenta, cuenta.getTipoCuenta()), nuevoEstado);
		return cuentaRepository.guardar(cuenta);
	}

	private ClienteRef bloquearCliente(UUID clienteId) {
		return clienteRefRepository.buscarConBloqueoCompartido(clienteId)
				.orElseThrow(() -> new ClienteNoEncontradoException(clienteId));
	}

	private static void validarActivo(ClienteRef cliente) {
		if (!cliente.estado()) {
			throw new ClienteInactivoException(cliente.clienteId());
		}
	}

	private Cuenta bloquear(String numeroCuenta) {
		return cuentaRepository.buscarPorNumeroParaActualizar(numeroCuenta)
				.orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta));
	}
}
