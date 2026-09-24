package git.juanpablopinza.accounts.application.port.out;

import git.juanpablopinza.accounts.application.port.Pagina;
import git.juanpablopinza.accounts.domain.model.Cuenta;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CuentaRepositoryPort {

	Cuenta crear(Cuenta cuenta);

	Cuenta guardar(Cuenta cuenta);

	Optional<Cuenta> buscarPorNumero(String numeroCuenta);

	Optional<Cuenta> buscarPorNumeroParaActualizar(String numeroCuenta);

	Optional<UUID> buscarClienteId(String numeroCuenta);

	boolean existePorNumero(String numeroCuenta);

	Pagina<Cuenta> listar(UUID clienteId, int pagina, int tamanio);

	List<Cuenta> listarPorCliente(UUID clienteId);

	int desactivarPorCliente(UUID clienteId);
}
