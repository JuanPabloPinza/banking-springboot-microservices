package git.juanpablopinza.accounts.application.port.in;

import git.juanpablopinza.accounts.application.port.Pagina;
import git.juanpablopinza.accounts.domain.model.Cuenta;

import java.util.UUID;

public interface CuentaUseCase {

	Cuenta crear(CrearCuentaCommand command);

	Cuenta obtener(String numeroCuenta);

	Pagina<Cuenta> listar(UUID clienteId, int pagina, int tamanio);

	Cuenta actualizar(String numeroCuenta, ActualizarCuentaCommand command);

	Cuenta actualizarParcial(String numeroCuenta, ActualizarParcialCuentaCommand command);

	void eliminar(String numeroCuenta);
}
