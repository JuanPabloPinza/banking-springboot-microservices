package git.juanpablopinza.accounts.domain.model;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class Movimiento {

	private final Long id;
	private final String numeroCuenta;
	private final LocalDateTime fecha;
	private TipoMovimiento tipoMovimiento;
	private BigDecimal valor;
	private BigDecimal saldo;
	private final String idempotencyKey;

	public Movimiento(Long id, String numeroCuenta, LocalDateTime fecha, TipoMovimiento tipoMovimiento,
			BigDecimal valor, BigDecimal saldo, String idempotencyKey) {
		this.id = id;
		this.numeroCuenta = numeroCuenta;
		this.fecha = fecha;
		this.tipoMovimiento = tipoMovimiento;
		this.valor = valor;
		this.saldo = saldo;
		this.idempotencyKey = idempotencyKey;
	}

	static Movimiento nuevo(String numeroCuenta, LocalDateTime fecha, BigDecimal valor, BigDecimal saldo,
			String idempotencyKey) {
		return new Movimiento(null, numeroCuenta, fecha, TipoMovimiento.segun(valor), valor, saldo, idempotencyKey);
	}

	public BigDecimal saldoAnterior() {
		return saldo.subtract(valor);
	}

	public boolean esMismaSolicitud(String numeroCuenta, BigDecimal valor) {
		return this.numeroCuenta.equals(numeroCuenta) && this.valor.compareTo(valor) == 0;
	}

	void corregir(BigDecimal nuevoValor, BigDecimal nuevoSaldo) {
		this.valor = nuevoValor;
		this.tipoMovimiento = TipoMovimiento.segun(nuevoValor);
		this.saldo = nuevoSaldo;
	}
}
