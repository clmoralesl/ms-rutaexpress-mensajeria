package cl.rutaexpress.mensajeria.exception;

public class RecursoExistenteException extends RuntimeException {
    public RecursoExistenteException(String mensaje) {
        super(mensaje);
    }
}
