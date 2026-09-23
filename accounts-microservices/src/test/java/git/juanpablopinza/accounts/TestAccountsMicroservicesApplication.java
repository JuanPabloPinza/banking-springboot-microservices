package git.juanpablopinza.accounts;

import org.springframework.boot.SpringApplication;

public class TestAccountsMicroservicesApplication {

	public static void main(String[] args) {
		SpringApplication.from(AccountsMicroservicesApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
