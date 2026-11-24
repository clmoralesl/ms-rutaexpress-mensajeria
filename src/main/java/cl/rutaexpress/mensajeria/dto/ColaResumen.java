package cl.rutaexpress.mensajeria.dto;

import java.util.Map;

public record ColaResumen(
        String nombre,
        boolean durable,
        long mensajes,
        int consumidores,
        Map<String, Object> argumentos,
        boolean protegido
) {}
