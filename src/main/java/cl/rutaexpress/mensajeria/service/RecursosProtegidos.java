package cl.rutaexpress.mensajeria.service;

import java.util.List;

public final class RecursosProtegidos {
    private static final List<String> PREFIJOS = List.of("q.cmd.", "cmd.", "amq.");

    private RecursosProtegidos() {}

    public static boolean esProtegido(String nombre) {
        return nombre == null || nombre.isEmpty() || PREFIJOS.stream().anyMatch(nombre::startsWith);
    }
}
