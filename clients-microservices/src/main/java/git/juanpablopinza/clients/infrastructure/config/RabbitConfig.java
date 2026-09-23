package git.juanpablopinza.clients.infrastructure.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

	public static final String EXCHANGE_CLIENTES = "clientes.eventos";

	@Bean
	TopicExchange clientesExchange() {
		return new TopicExchange(EXCHANGE_CLIENTES, true, false);
	}

	@Bean
	MessageConverter messageConverter() {
		return new JacksonJsonMessageConverter();
	}
}
