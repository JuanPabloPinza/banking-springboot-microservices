package git.juanpablopinza.clients.application.port.out;

public interface PasswordHasherPort {

	String hash(String contrasena);
}
