package cl.rutaexpress.mensajeria.client;

import cl.rutaexpress.mensajeria.dto.BindingResumen;
import cl.rutaexpress.mensajeria.dto.ColaResumen;
import cl.rutaexpress.mensajeria.dto.ExchangeResumen;

import java.util.List;

public interface ConsultaTopologia {
    List<ColaResumen> listarColas();
    List<ExchangeResumen> listarExchanges();
    List<BindingResumen> listarBindings();
    boolean existeCola(String nombre);
    boolean existeExchange(String nombre);
    boolean existeBinding(String exchange, String cola, String clave);
    boolean tieneBindingsComoOrigen(String exchange);
}
