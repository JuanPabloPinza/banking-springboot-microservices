package git.juanpablopinza.clients.infrastructure.adapter.out.messaging;

import git.juanpablopinza.clients.application.port.out.ClienteEventoPublisherPort;
import git.juanpablopinza.clients.domain.event.ClienteEvento;
import git.juanpablopinza.clients.infrastructure.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitClienteEventoPublisher implements ClienteEventoPublisherPort {

	private final RabbitTemplate rabbitTemplate;

	@Override
	public void publicar(ClienteEvento evento) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					enviar(evento);
				}
			});
		} else {
			enviar(evento);
		}
	}

	private void enviar(ClienteEvento evento) {
		String routingKey = "cliente." + evento.tipo().name().toLowerCase(Locale.ROOT);
		ClienteEventoMessage mensaje = new ClienteEventoMessage(evento.eventoId(), evento.tipo().name(),
				evento.clienteId(), evento.nombre(), evento.estado(), evento.ocurridoEn());
		try {
			rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_CLIENTES, routingKey, mensaje);
		} catch (AmqpException e) {
			log.error("No se pudo publicar el evento {} del cliente {}", routingKey, evento.clienteId(), e);
		}
	}
}
