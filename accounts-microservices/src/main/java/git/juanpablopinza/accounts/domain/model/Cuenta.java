package git.juanpablopinza.accounts.domain.model;

import git.juanpablopinza.accounts.domain.exception.CuentaInactivaException;
import git.juanpablopinza.accounts.domain.exception.DatoInvalidoException;
import git.juanpablopinza.accounts.domain.exception.SaldoNoDisponibleException;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Cuenta {

	private final String numeroCuenta;
	private TipoCuenta tipoCuenta;
	private final BigDecimal saldoInicial;
	private BigDecimal saldoDisponible;
	private boolean estado;
	private final UUID clienteId;

	public Cuenta(String numeroCuenta, TipoCuenta tipoCuenta, BigDecimal saldoInicial, BigDecimal saldoDisponible,
			boolean estado, UUID clienteId) {
		this.numeroCuenta = numeroCuenta;
		this.tipoCuenta = tipoCuenta;
		this.saldoInicial = saldoInicial;
		this.saldoDisponible = saldoDisponible;
		this.estado = estado;
		this.clienteId = clienteId;
	}

	public static Cuenta abrir(String numeroCuenta, TipoCuenta tipoCuenta, BigDecimal saldoInicial, boolean estado,
			UUID clienteId) {
		if (saldoInicial.signum() < 0) {
			throw new DatoInvalidoException("El saldo inicial no puede ser negativo");
		}
		BigDecimal saldo = dinero(saldoInicial);
		return new Cuenta(numeroCuenta, tipoCuenta, saldo, saldo, estado, clienteId);
	}

	public Movimiento registrarMovimiento(BigDecimal valor, LocalDateTime fecha, String idempotencyKey) {
		validarActiva();
		BigDecimal monto = montoMovimiento(valor);
		saldoDisponible = saldoTras(saldoDisponible, monto);
		return Movimiento.nuevo(numeroCuenta, fecha, monto, saldoDisponible, idempotencyKey);
	}

	public void corregirUltimoMovimiento(Movimiento ultimo, BigDecimal nuevoValor) {
		validarActiva();
		validarPertenencia(ultimo);
		BigDecimal monto = montoMovimiento(nuevoValor);
		BigDecimal nuevoSaldo = saldoTras(ultimo.saldoAnterior(), monto);
		ultimo.corregir(monto, nuevoSaldo);
		saldoDisponible = nuevoSaldo;
	}

	public void revertirUltimoMovimiento(Movimiento ultimo) {
		validarActiva();
		validarPertenencia(ultimo);
		saldoDisponible = ultimo.saldoAnterior();
	}

	public void actualizar(TipoCuenta tipoCuenta, boolean estado) {
		this.tipoCuenta = tipoCuenta;
		this.estado = estado;
	}

	public void desactivar() {
		this.estado = false;
	}

	private static BigDecimal montoMovimiento(BigDecimal valor) {
		if (valor == null || valor.signum() == 0) {
			throw new DatoInvalidoException("El valor del movimiento debe ser distinto de cero");
		}
		return dinero(valor);
	}

	private static BigDecimal saldoTras(BigDecimal saldoBase, BigDecimal monto) {
		BigDecimal nuevoSaldo = saldoBase.add(monto);
		if (nuevoSaldo.signum() < 0) {
			throw new SaldoNoDisponibleException();
		}
		return nuevoSaldo;
	}

	private static BigDecimal dinero(BigDecimal monto) {
		try {
			return monto.setScale(2, RoundingMode.UNNECESSARY);
		} catch (ArithmeticException e) {
			throw new DatoInvalidoException("Los montos admiten máximo 2 decimales");
		}
	}

	private void validarActiva() {
		if (!estado) {
			throw new CuentaInactivaException(numeroCuenta);
		}
	}

	private void validarPertenencia(Movimiento movimiento) {
		if (!numeroCuenta.equals(movimiento.getNumeroCuenta())) {
			throw new IllegalArgumentException("El movimiento " + movimiento.getId() + " no pertenece a la cuenta "
					+ numeroCuenta);
		}
	}
}
