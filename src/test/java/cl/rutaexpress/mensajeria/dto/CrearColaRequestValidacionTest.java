package cl.rutaexpress.mensajeria.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrearColaRequestValidacionTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private Set<String> camposConError(CrearColaRequest r) {
        return validator.validate(r).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    void valido() {
        assertTrue(camposConError(new CrearColaRequest("demo.cola", true, null, null, 60000, 100)).isEmpty());
    }

    @Test
    void nombreVacio() {
        assertTrue(camposConError(new CrearColaRequest("", true, null, null, null, null)).contains("nombre"));
    }

    @Test
    void nombreMayusculas() {
        assertTrue(camposConError(new CrearColaRequest("Demo", true, null, null, null, null)).contains("nombre"));
    }

    @Test
    void nombre256() {
        assertTrue(camposConError(new CrearColaRequest("a".repeat(256), true, null, null, null, null)).contains("nombre"));
    }

    @Test
    void ttlCero() {
        assertTrue(camposConError(new CrearColaRequest("demo", true, null, null, 0, null)).contains("ttlMs"));
    }

    @Test
    void ttlNegativo() {
        assertTrue(camposConError(new CrearColaRequest("demo", true, null, null, -1, null)).contains("ttlMs"));
    }

    @Test
    void largoMaximoCero() {
        assertTrue(camposConError(new CrearColaRequest("demo", true, null, null, null, 0)).contains("largoMaximo"));
    }

    @Test
    void claveMuertosSinExchange() {
        assertTrue(camposConError(new CrearColaRequest("demo", true, null, "x.dead", null, null)).contains("claveMuertosConExchange"));
    }

    @Test
    void claveMuertosConExchangeValido() {
        assertFalse(camposConError(new CrearColaRequest("demo", true, "demo.dlx", "x.dead", null, null)).contains("claveMuertosConExchange"));
    }
}
