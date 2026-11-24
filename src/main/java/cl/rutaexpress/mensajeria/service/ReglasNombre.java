package cl.rutaexpress.mensajeria.service;

import cl.rutaexpress.mensajeria.exception.NombreInvalidoException;

public final class ReglasNombre {
    public static final String PATRON_NOMBRE = "^[a-z0-9][a-z0-9._-]*$";
    public static final String PATRON_CLAVE = "^[a-z0-9._*#-]*$";
    public static final String PATRON_TIPO = "^(direct|topic|fanout)$";
    public static final int LARGO_MAXIMO = 255;

    private ReglasNombre() {}

    public static void validarNombre(String campo, String valor) {
        if (valor == null || valor.isBlank() || valor.length() > LARGO_MAXIMO || !valor.matches(PATRON_NOMBRE)) {
            throw new NombreInvalidoException(campo + ": debe tener entre 1 y 255 caracteres, "
                    + "en minusculas, digitos, punto, guion o guion bajo, y comenzar con letra o digito");
        }
    }

    public static void validarClave(String valor) {
        if (valor == null || valor.length() > LARGO_MAXIMO || !valor.matches(PATRON_CLAVE)) {
            throw new NombreInvalidoException("clave: hasta 255 caracteres en minusculas, digitos, '.', '-', '_', '*' o '#'");
        }
    }
}
