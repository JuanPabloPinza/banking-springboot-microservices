package git.juanpablopinza.accounts.application.service;

import git.juanpablopinza.accounts.application.port.in.SincronizarClienteCommand;
import git.juanpablopinza.accounts.application.port.in.SincronizarClienteUseCase;
import git.juanpablopinza.accounts.application.port.out.ClienteRefRepositoryPort;
import git.juanpablopinza.accounts.application.port.out.CuentaRepositoryPort;
import git.juanpablopinza.accounts.domain.model.ClienteRef;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SincronizacionClienteService implements SincronizarClienteUseCase {

	private final ClienteRefRepositoryPort clienteRefRepository;
	private final CuentaRepositoryPort cuentaRepository;

	@Override
	public void sincronizar(SincronizarClienteCommand command) {
		boolean atrasado = clienteRefRepository.buscarParaActualizar(command.clienteId())
				.map(actual -> actual.esMasRecienteQue(command.ocurridoEn()))
				.orElse(false);
		if (atrasado) {
			log.info("Evento atrasado del cliente {} ignorado", command.clienteId());
			return;
		}
		clienteRefRepository.guardar(new ClienteRef(command.clienteId(), command.nombre(), command.estado(),
				command.ocurridoEn()));
		if (!command.estado()) {
			int desactivadas = cuentaRepository.desactivarPorCliente(command.clienteId());
			log.info("Cliente {} inactivo: {} cuenta(s) desactivada(s)", command.clienteId(), desactivadas);
		}
	}
}
