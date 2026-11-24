package cl.rutaexpress.mensajeria.dto;

public record BindingResumen(
        String exchange,
        String cola,
        String clave,
        boolean protegido
) {}
