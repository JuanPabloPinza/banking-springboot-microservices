package git.juanpablopinza.clients.domain.model;

import git.juanpablopinza.clients.domain.exception.DatoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentificacionTest {

	@ParameterizedTest
	@ValueSource(strings = {"1710034065", "1712345675", "0102030400"})
	@DisplayName("Acepta cédulas con dígito verificador correcto (datos usados en Postman)")
	void cedulasValidas(String cedula) {
		assertThat(Identificacion.esValida(cedula)).isTrue();
		assertThat(new Identificacion(cedula).valor()).isEqualTo(cedula);
	}

    //Algunos valores en los que la cédula es incorrecta
	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {
			"1710034066", // dígito verificador incorrecto
			"2510034065", // provincia inexistente
			"1760034065", // tercer dígito >= 6 (no es persona natural)
			"171003406",  // 9 dígitos
			"17100340655", // 11 dígitos
			"17100340AB"  // no numérica
	})
	@DisplayName("Rechaza cédulas inválidas")
	void cedulasInvalidas(String cedula) {
		assertThat(Identificacion.esValida(cedula)).isFalse();
		assertThatThrownBy(() -> new Identificacion(cedula)).isInstanceOf(DatoInvalidoException.class);
	}
}
