package cl.rutaexpress.mensajeria.dto;

import cl.rutaexpress.mensajeria.service.ReglasNombre;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BindingRequest(
        @NotBlank @Size(max = 255) @Pattern(regexp = ReglasNombre.PATRON_NOMBRE) String exchange,
        @NotBlank @Size(max = 255) @Pattern(regexp = ReglasNombre.PATRON_NOMBRE) String cola,
        @NotNull @Size(max = 255) @Pattern(regexp = ReglasNombre.PATRON_CLAVE) String clave
) {}
