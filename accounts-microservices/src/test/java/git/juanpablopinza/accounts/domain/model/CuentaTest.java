package git.juanpablopinza.accounts.domain.model;

import git.juanpablopinza.accounts.domain.exception.CuentaInactivaException;
import git.juanpablopinza.accounts.domain.exception.DatoInvalidoException;
import git.juanpablopinza.accounts.domain.exception.SaldoNoDisponibleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CuentaTest {

	private static final LocalDateTime AHORA = LocalDateTime.of(2026, 9, 23, 10, 0);

	private Cuenta cuenta(String numero, String saldoInicial) {
		return Cuenta.abrir(numero, TipoCuenta.AHORROS, new BigDecimal(saldoInicial), true, UUID.randomUUID());
	}

	@Test
	@DisplayName("Una cuenta nueva tiene saldo disponible igual al saldo inicial")
	void abrirCuenta() {
		Cuenta cuenta = cuenta("478758", "2000");

		assertThat(cuenta.getSaldoDisponible()).isEqualByComparingTo("2000");
		assertThat(cuenta.isEstado()).isTrue();
	}

	@Test
	@DisplayName("Un depósito (valor positivo) aumenta el saldo y no toca el saldo inicial")
	void deposito() {
		Cuenta cuenta = cuenta("225487", "100");

		Movimiento movimiento = cuenta.registrarMovimiento(new BigDecimal("600"), AHORA, null);

		assertThat(movimiento.getTipoMovimiento()).isEqualTo(TipoMovimiento.DEPOSITO);
		assertThat(movimiento.getSaldo()).isEqualByComparingTo("700");
		assertThat(cuenta.getSaldoDisponible()).isEqualByComparingTo("700");
		assertThat(cuenta.getSaldoInicial()).isEqualByComparingTo("100");
	}

	@Test
	@DisplayName("Un retiro (valor negativo) descuenta el saldo")
	void retiro() {
		Cuenta cuenta = cuenta("478758", "2000");

		Movimiento movimiento = cuenta.registrarMovimiento(new BigDecimal("-575"), AHORA, null);

		assertThat(movimiento.getTipoMovimiento()).isEqualTo(TipoMovimiento.RETIRO);
		assertThat(cuenta.getSaldoDisponible()).isEqualByComparingTo("1425");
	}

	@Test
	@DisplayName("Retirar exactamente todo el saldo está permitido y deja la cuenta en 0")
	void retiroDeTodoElSaldo() {
		Cuenta cuenta = cuenta("496825", "540");

		cuenta.registrarMovimiento(new BigDecimal("-540"), AHORA, null);

		assertThat(cuenta.getSaldoDisponible()).isEqualByComparingTo("0");
	}

	@Test
	@DisplayName("F3: un retiro sin saldo suficiente lanza 'Saldo no disponible' y no modifica el saldo")
	void retiroSinSaldo() {
		Cuenta cuenta = cuenta("495878", "0");

		assertThatThrownBy(() -> cuenta.registrarMovimiento(new BigDecimal("-0.01"), AHORA, null))
				.isInstanceOf(SaldoNoDisponibleException.class)
				.hasMessage("Saldo no disponible");
		assertThat(cuenta.getSaldoDisponible()).isEqualByComparingTo("0");
	}

	@Test
	@DisplayName("Un movimiento de valor cero no tiene sentido y se rechaza")
	void valorCero() {
		Cuenta cuenta = cuenta("478758", "100");

		assertThatThrownBy(() -> cuenta.registrarMovimiento(BigDecimal.ZERO, AHORA, null))
				.isInstanceOf(DatoInvalidoException.class);
	}

	@Test
	@DisplayName("Una cuenta inactiva no admite movimientos")
	void cuentaInactiva() {
		Cuenta cuenta = cuenta("478758", "100");
		cuenta.desactivar();

		assertThatThrownBy(() -> cuenta.registrarMovimiento(BigDecimal.TEN, AHORA, null))
				.isInstanceOf(CuentaInactivaException.class);
	}

	@Test
	@DisplayName("Corregir el último movimiento recalcula su saldo y el de la cuenta desde el saldo anterior")
	void corregirUltimoMovimiento() {
		Cuenta cuenta = cuenta("585545", "1000");
		Movimiento ultimo = cuenta.registrarMovimiento(new BigDecimal("100"), AHORA, null);

		cuenta.corregirUltimoMovimiento(ultimo, new BigDecimal("-300"));

		assertThat(ultimo.getTipoMovimiento()).isEqualTo(TipoMovimiento.RETIRO);
		assertThat(ultimo.getSaldo()).isEqualByComparingTo("700");
		assertThat(cuenta.getSaldoDisponible()).isEqualByComparingTo("700");
	}

	@Test
	@DisplayName("Una corrección que dejaría saldo negativo también responde 'Saldo no disponible'")
	void corregirSinSaldo() {
		Cuenta cuenta = cuenta("585545", "1000");
		Movimiento ultimo = cuenta.registrarMovimiento(new BigDecimal("100"), AHORA, null);

		assertThatThrownBy(() -> cuenta.corregirUltimoMovimiento(ultimo, new BigDecimal("-1001")))
				.isInstanceOf(SaldoNoDisponibleException.class);
		assertThat(cuenta.getSaldoDisponible()).isEqualByComparingTo("1100");
	}

	@Test
	@DisplayName("Revertir el último movimiento devuelve el saldo al valor previo")
	void revertirUltimoMovimiento() {
		Cuenta cuenta = cuenta("585545", "1000");
		Movimiento ultimo = cuenta.registrarMovimiento(new BigDecimal("-250"), AHORA, null);

		cuenta.revertirUltimoMovimiento(ultimo);

		assertThat(cuenta.getSaldoDisponible()).isEqualByComparingTo("1000");
	}
}
