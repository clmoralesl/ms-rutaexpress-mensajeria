package cl.rutaexpress.mensajeria.dto;

import cl.rutaexpress.mensajeria.service.ReglasNombre;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CrearExchangeRequest(
        @NotBlank @Size(max = 255) @Pattern(regexp = ReglasNombre.PATRON_NOMBRE,
                message = "debe estar en minusculas y usar solo letras, digitos, '.', '-' o '_'") String nombre,
        @NotBlank @Pattern(regexp = ReglasNombre.PATRON_TIPO, message = "debe ser direct, topic o fanout") String tipo,
        Boolean durable
) {}
