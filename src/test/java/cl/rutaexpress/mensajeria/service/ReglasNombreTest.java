package cl.rutaexpress.mensajeria.service;

import cl.rutaexpress.mensajeria.exception.NombreInvalidoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReglasNombreTest {

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "Demo", "con espacio", ".empieza-punto", "-empieza-guion", "tilde.colaá", "a/b"})
    void rechazaNombresInvalidos(String nombre) {
        assertThrows(NombreInvalidoException.class, () -> ReglasNombre.validarNombre("nombre", nombre));
    }

    @Test
    void rechazaNombreDe256() {
        assertThrows(NombreInvalidoException.class, () -> ReglasNombre.validarNombre("nombre", "a".repeat(256)));
    }

    @Test
    void aceptaNombreDe255() {
        assertDoesNotThrow(() -> ReglasNombre.validarNombre("nombre", "a".repeat(255)));
    }

    @Test
    void aceptaClaveVaciaYComodines() {
        assertDoesNotThrow(() -> ReglasNombre.validarClave(""));
        assertDoesNotThrow(() -> ReglasNombre.validarClave("envio.*.#"));
    }
}
