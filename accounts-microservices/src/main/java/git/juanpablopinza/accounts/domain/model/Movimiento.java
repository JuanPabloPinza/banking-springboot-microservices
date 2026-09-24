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

	public Movimiento(Long id, String numeroCuenta, LocalDateTime fecha, TipoMovimiento tipoMovimiento,
			BigDecimal valor, BigDecimal saldo) {
		this.id = id;
		this.numeroCuenta = numeroCuenta;
		this.fecha = fecha;
		this.tipoMovimiento = tipoMovimiento;
		this.valor = valor;
		this.saldo = saldo;
	}

	static Movimiento nuevo(String numeroCuenta, LocalDateTime fecha, BigDecimal valor, BigDecimal saldo) {
		return new Movimiento(null, numeroCuenta, fecha, TipoMovimiento.segun(valor), valor, saldo);
	}

	public BigDecimal saldoAnterior() {
		return saldo.subtract(valor);
	}

	void corregir(BigDecimal nuevoValor, BigDecimal nuevoSaldo) {
		this.valor = nuevoValor;
		this.tipoMovimiento = TipoMovimiento.segun(nuevoValor);
		this.saldo = nuevoSaldo;
	}
}
