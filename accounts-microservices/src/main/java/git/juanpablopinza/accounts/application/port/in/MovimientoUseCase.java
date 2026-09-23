package git.juanpablopinza.accounts.application.port.in;

import git.juanpablopinza.accounts.application.port.Pagina;
import git.juanpablopinza.accounts.domain.model.Movimiento;

import java.math.BigDecimal;

public interface MovimientoUseCase {

	ResultadoMovimiento registrar(RegistrarMovimientoCommand command);

	Movimiento obtener(Long id);

	Pagina<Movimiento> listar(String numeroCuenta, int pagina, int tamanio);

	Movimiento corregir(Long id, BigDecimal nuevoValor);

	void eliminar(Long id);
}
