package git.juanpablopinza.clients.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** Un Clock inyectable hace que las fechas sean deterministas en las pruebas. */
@Configuration
public class ClockConfig {

	@Bean
	Clock clock() {
		return Clock.system(ZoneId.of("America/Guayaquil"));
	}
}
