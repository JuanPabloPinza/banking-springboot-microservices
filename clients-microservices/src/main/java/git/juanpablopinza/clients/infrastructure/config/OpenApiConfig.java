package git.juanpablopinza.clients.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI openApi() {
		return new OpenAPI().info(new Info()
				.title("clients-microservices")
				.version("v1")
				.description("Gestión de clientes (Persona / Cliente). Publica eventos de cliente en RabbitMQ."));
	}
}
