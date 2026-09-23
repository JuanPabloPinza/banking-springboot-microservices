package git.juanpablopinza.accounts.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI openApi() {
		return new OpenAPI().info(new Info()
				.title("accounts-microservices")
				.version("v1")
				.description("Cuentas, movimientos y reportes. Consume eventos de clientes desde RabbitMQ."));
	}
}
