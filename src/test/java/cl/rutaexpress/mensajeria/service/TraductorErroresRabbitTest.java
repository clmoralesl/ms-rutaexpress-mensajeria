package cl.rutaexpress.mensajeria.service;

import cl.rutaexpress.mensajeria.exception.BrokerNoDisponibleException;
import cl.rutaexpress.mensajeria.exception.RecursoEnConflictoException;
import cl.rutaexpress.mensajeria.exception.RecursoNoEncontradoException;
import cl.rutaexpress.mensajeria.exception.RecursoProtegidoException;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.impl.AMQImpl;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.AmqpIOException;

import java.io.IOException;
import java.net.ConnectException;

import static org.junit.jupiter.api.Assertions.*;

class TraductorErroresRabbitTest {

    private final TraductorErroresRabbit traductor = new TraductorErroresRabbit();

    static AmqpIOException errorAmqp(int codigo) {
        AMQImpl.Channel.Close cierre = new AMQImpl.Channel.Close(codigo, "TEXTO_INTERNO", 50, 20);
        return new AmqpIOException(new IOException(new ShutdownSignalException(false, false, cierre, null)));
    }

    @Test
    void traduce406AConflicto() {
        RuntimeException ex = traductor.traducir(errorAmqp(406), "la cola 'demo'");
        assertInstanceOf(RecursoEnConflictoException.class, ex);
        assertFalse(ex.getMessage().contains("TEXTO_INTERNO"));
    }

    @Test
    void traduce404ANoEncontrado() {
        RuntimeException ex = traductor.traducir(errorAmqp(404), "la cola 'demo'");
        assertInstanceOf(RecursoNoEncontradoException.class, ex);
        assertFalse(ex.getMessage().contains("TEXTO_INTERNO"));
    }

    @Test
    void traduce403AProtegido() {
        RuntimeException ex = traductor.traducir(errorAmqp(403), "la cola 'demo'");
        assertInstanceOf(RecursoProtegidoException.class, ex);
        assertFalse(ex.getMessage().contains("TEXTO_INTERNO"));
    }

    @Test
    void traduceAmqpConnectExceptionABrokerNoDisponible() {
        RuntimeException ex = traductor.traducir(new AmqpConnectException(new ConnectException("fallo de red")), "contexto");
        assertInstanceOf(BrokerNoDisponibleException.class, ex);
        assertEquals("El broker de mensajeria no esta disponible; intente nuevamente", ex.getMessage());
    }

    @Test
    void traduceAmqpExceptionSinCausaABrokerNoDisponible() {
        RuntimeException ex = traductor.traducir(new AmqpException("error generico") {}, "contexto");
        assertInstanceOf(BrokerNoDisponibleException.class, ex);
        assertEquals("El broker de mensajeria no esta disponible; intente nuevamente", ex.getMessage());
    }

    @Test
    void amqpConnectExceptionConCierreDeCanal406EsConflicto() {
        AMQImpl.Channel.Close cierre = new AMQImpl.Channel.Close(406, "TEXTO_INTERNO", 50, 20);
        RuntimeException ex = traductor.traducir(
                new AmqpConnectException(new ShutdownSignalException(false, false, cierre, null)), "la cola 'demo'");
        assertInstanceOf(RecursoEnConflictoException.class, ex);
    }
}
