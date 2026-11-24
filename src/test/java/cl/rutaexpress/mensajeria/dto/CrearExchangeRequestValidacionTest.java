package cl.rutaexpress.mensajeria.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrearExchangeRequestValidacionTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private Set<String> camposConError(CrearExchangeRequest r) {
        return validator.validate(r).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @ParameterizedTest
    @ValueSource(strings = {"headers", "DIRECT", "", "otro"})
    void tipoInvalido(String tipo) {
        assertTrue(camposConError(new CrearExchangeRequest("demo.ex", tipo, true)).contains("tipo"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"direct", "topic", "fanout"})
    void tipoValido(String tipo) {
        assertFalse(camposConError(new CrearExchangeRequest("demo.ex", tipo, true)).contains("tipo"));
    }

    @Test
    void nombreInvalido() {
        assertTrue(camposConError(new CrearExchangeRequest("Demo", "direct", true)).contains("nombre"));
    }
}
