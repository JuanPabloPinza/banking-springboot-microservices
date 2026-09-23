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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static java.util.Objects.requireNonNullElse;

@Service
@Transactional
@RequiredArgsConstructor
public class CuentaService implements CuentaUseCase {

	private final CuentaRepositoryPort cuentaRepository;
	private final ClienteRefRepositoryPort clienteRefRepository;

	@Override
	public Cuenta crear(CrearCuentaCommand command) {
		ClienteRef cliente = clienteRefRepository.buscar(command.clienteId())
				.orElseThrow(() -> new ClienteNoEncontradoException(command.clienteId()));
		if (!cliente.estado()) {
			throw new ClienteInactivoException(cliente.clienteId());
		}
		if (cuentaRepository.existePorNumero(command.numeroCuenta())) {
			throw new NumeroCuentaDuplicadoException(command.numeroCuenta());
		}
		Cuenta cuenta = Cuenta.abrir(command.numeroCuenta(), command.tipoCuenta(), command.saldoInicial(),
				requireNonNullElse(command.estado(), true), command.clienteId());
		return cuentaRepository.guardar(cuenta);
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
		Cuenta cuenta = bloquear(numeroCuenta);
		cuenta.actualizar(command.tipoCuenta(), command.estado());
		return cuentaRepository.guardar(cuenta);
	}

	@Override
	public Cuenta actualizarParcial(String numeroCuenta, ActualizarParcialCuentaCommand command) {
		Cuenta cuenta = bloquear(numeroCuenta);
		cuenta.actualizar(requireNonNullElse(command.tipoCuenta(), cuenta.getTipoCuenta()),
				requireNonNullElse(command.estado(), cuenta.isEstado()));
		return cuentaRepository.guardar(cuenta);
	}

	@Override
	public void eliminar(String numeroCuenta) {
		Cuenta cuenta = bloquear(numeroCuenta);
		cuenta.desactivar();
		cuentaRepository.guardar(cuenta);
	}

	private Cuenta bloquear(String numeroCuenta) {
		return cuentaRepository.buscarPorNumeroParaActualizar(numeroCuenta)
				.orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta));
	}
}
