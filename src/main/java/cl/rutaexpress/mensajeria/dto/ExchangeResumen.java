package cl.rutaexpress.mensajeria.dto;

public record ExchangeResumen(
        String nombre,
        String tipo,
        boolean durable,
        boolean protegido
) {}
