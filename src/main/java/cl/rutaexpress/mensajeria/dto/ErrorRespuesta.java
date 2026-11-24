package cl.rutaexpress.mensajeria.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorRespuesta(
        Instant timestamp,
        int estado,
        String error,
        String mensaje,
        Map<String, String> campos,
        String ruta
) {}
