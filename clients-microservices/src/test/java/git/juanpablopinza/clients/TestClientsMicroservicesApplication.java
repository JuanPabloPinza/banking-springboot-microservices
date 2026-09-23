package git.juanpablopinza.clients;

import org.springframework.boot.SpringApplication;

public class TestClientsMicroservicesApplication {

	public static void main(String[] args) {
		SpringApplication.from(ClientsMicroservicesApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
