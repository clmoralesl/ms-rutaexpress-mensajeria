package cl.rutaexpress.mensajeria.exception;

public class BrokerNoDisponibleException extends RuntimeException {
    public BrokerNoDisponibleException() {
        super("El broker de mensajeria no esta disponible; intente nuevamente");
    }

    public BrokerNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
