package git.juanpablopinza.clients.infrastructure.adapter.out.security;

import git.juanpablopinza.clients.application.port.out.PasswordHasherPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordHasher implements PasswordHasherPort {

	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

	@Override
	public String hash(String contrasena) {
		return encoder.encode(contrasena);
	}
}
