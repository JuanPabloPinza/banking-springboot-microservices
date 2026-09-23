package git.juanpablopinza.accounts.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

	public static final String EXCHANGE_CLIENTES = "clientes.eventos";
	public static final String COLA_CLIENTES = "cuentas.clientes-eventos";
	public static final String EXCHANGE_DLX = "clientes.eventos.dlx";
	public static final String COLA_DLQ = "cuentas.clientes-eventos.dlq";

	@Bean
	TopicExchange clientesExchange() {
		return new TopicExchange(EXCHANGE_CLIENTES, true, false);
	}

	@Bean
	DirectExchange clientesDeadLetterExchange() {
		return new DirectExchange(EXCHANGE_DLX, true, false);
	}

	@Bean
	Queue clientesEventosQueue() {
		return QueueBuilder.durable(COLA_CLIENTES)
				.deadLetterExchange(EXCHANGE_DLX)
				.deadLetterRoutingKey(COLA_DLQ)
				.build();
	}

	@Bean
	Queue clientesEventosDlq() {
		return QueueBuilder.durable(COLA_DLQ).build();
	}

	@Bean
	Binding clientesEventosBinding() {
		return BindingBuilder.bind(clientesEventosQueue()).to(clientesExchange()).with("cliente.*");
	}

	@Bean
	Binding clientesEventosDlqBinding() {
		return BindingBuilder.bind(clientesEventosDlq()).to(clientesDeadLetterExchange()).with(COLA_DLQ);
	}

	@Bean
	MessageConverter messageConverter() {
		JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
		converter.setAlwaysConvertToInferredType(true);
		return converter;
	}
}
