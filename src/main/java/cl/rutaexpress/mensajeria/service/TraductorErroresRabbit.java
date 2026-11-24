package cl.rutaexpress.mensajeria.service;

import cl.rutaexpress.mensajeria.exception.BrokerNoDisponibleException;
import cl.rutaexpress.mensajeria.exception.RecursoEnConflictoException;
import cl.rutaexpress.mensajeria.exception.RecursoNoEncontradoException;
import cl.rutaexpress.mensajeria.exception.RecursoProtegidoException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ShutdownSignalException;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.stereotype.Component;

@Component
public class TraductorErroresRabbit {

    public RuntimeException traducir(AmqpException ex, String contexto) {
        Integer codigo = codigoAmqp(ex);
        if (codigo == null) return new BrokerNoDisponibleException();
        return switch (codigo) {
            case 406 -> new RecursoEnConflictoException("No se pudo completar la operacion sobre " + contexto
                    + ": existe con otra configuracion o tiene mensajes, consumidores o bindings");
            case 404 -> new RecursoNoEncontradoException("No existe " + contexto);
            case 403 -> new RecursoProtegidoException("RabbitMQ no permite modificar " + contexto);
            default -> new BrokerNoDisponibleException();
        };
    }

    static Integer codigoAmqp(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof ShutdownSignalException sse && sse.getReason() instanceof AMQP.Channel.Close cierre) {
                return cierre.getReplyCode();
            }
        }
        return null;
    }
}
