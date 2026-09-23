package git.juanpablopinza.accounts.application.port.out;

import git.juanpablopinza.accounts.application.port.Pagina;
import git.juanpablopinza.accounts.domain.model.Movimiento;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MovimientoRepositoryPort {

	Movimiento guardar(Movimiento movimiento);

	Optional<Movimiento> buscarPorId(Long id);

	Optional<Movimiento> buscarUltimo(String numeroCuenta);

	Optional<Movimiento> buscarPorIdempotencyKey(String idempotencyKey);

	Pagina<Movimiento> listar(String numeroCuenta, int pagina, int tamanio);

	List<Movimiento> listarPorClienteEntre(UUID clienteId, LocalDateTime desde, LocalDateTime hastaExclusivo);

	void eliminar(Long id);
}
