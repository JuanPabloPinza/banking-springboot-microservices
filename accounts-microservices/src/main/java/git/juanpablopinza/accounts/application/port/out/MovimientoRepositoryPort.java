package git.juanpablopinza.accounts.application.port.out;

import git.juanpablopinza.accounts.application.port.Pagina;
import git.juanpablopinza.accounts.domain.model.Movimiento;

import java.util.Optional;

public interface MovimientoRepositoryPort {

	Movimiento guardar(Movimiento movimiento);

	Optional<Movimiento> buscarPorId(Long id);

	Optional<Movimiento> buscarUltimo(String numeroCuenta);

	Optional<Movimiento> buscarPorIdempotencyKey(String idempotencyKey);

	Pagina<Movimiento> listar(String numeroCuenta, int pagina, int tamanio);

	void eliminar(Long id);
}
