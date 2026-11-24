package cl.rutaexpress.mensajeria.dto;

import cl.rutaexpress.mensajeria.service.ReglasNombre;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CrearColaRequest(
        @NotBlank @Size(max = 255) @Pattern(regexp = ReglasNombre.PATRON_NOMBRE,
                message = "debe estar en minusculas y usar solo letras, digitos, '.', '-' o '_'") String nombre,
        Boolean durable,
        @Size(max = 255) @Pattern(regexp = ReglasNombre.PATRON_NOMBRE) String exchangeMuertos,
        @Size(max = 255) @Pattern(regexp = ReglasNombre.PATRON_CLAVE) String claveMuertos,
        @Positive(message = "debe ser mayor que cero") Integer ttlMs,
        @Positive(message = "debe ser mayor que cero") Integer largoMaximo) {

    @AssertTrue(message = "claveMuertos requiere exchangeMuertos")
    public boolean isClaveMuertosConExchange() {
        return claveMuertos == null || exchangeMuertos != null;
    }
}
