package git.juanpablopinza.accounts.infrastructure.adapter.in.messaging;

import git.juanpablopinza.accounts.application.port.in.SincronizarClienteCommand;
import git.juanpablopinza.accounts.application.port.in.SincronizarClienteUseCase;
import git.juanpablopinza.accounts.infrastructure.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClienteEventoListener {

	private final SincronizarClienteUseCase sincronizarClienteUseCase;

	@RabbitListener(queues = RabbitConfig.COLA_CLIENTES)
	public void recibir(ClienteEventoMessage mensaje) {
		log.info("Evento {} recibido para el cliente {}", mensaje.tipo(), mensaje.clienteId());
		sincronizarClienteUseCase.sincronizar(new SincronizarClienteCommand(mensaje.clienteId(), mensaje.nombre(),
				mensaje.estado(), mensaje.ocurridoEn()));
	}
}
