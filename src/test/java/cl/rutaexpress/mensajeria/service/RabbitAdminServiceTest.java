package cl.rutaexpress.mensajeria.service;

import cl.rutaexpress.mensajeria.client.ConsultaTopologia;
import cl.rutaexpress.mensajeria.dto.BindingRequest;
import cl.rutaexpress.mensajeria.dto.CrearColaRequest;
import cl.rutaexpress.mensajeria.dto.ExchangeResumen;
import cl.rutaexpress.mensajeria.exception.*;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.impl.AMQImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RabbitAdminServiceTest {

    @Mock
    private AmqpAdmin amqpAdmin;

    @Mock
    private ConsultaTopologia consulta;

    private RabbitAdminService servicio;

    @BeforeEach
    void setUp() {
        TraductorErroresRabbit traductor = new TraductorErroresRabbit();
        servicio = new RabbitAdminService(amqpAdmin, consulta, traductor);
    }

    static AmqpIOException errorAmqp(int codigo) {
        AMQImpl.Channel.Close cierre = new AMQImpl.Channel.Close(codigo, "TEXTO_INTERNO", 50, 20);
        return new AmqpIOException(new IOException(new ShutdownSignalException(false, false, cierre, null)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"q.cmd.email", "cmd.direct", "amq.direct"})
    void eliminarColaProtegidaRetornaConflictoSinTocarBroker(String nombre) {
        assertThrows(RecursoProtegidoException.class, () -> servicio.eliminarCola(nombre, true));
        verifyNoInteractions(amqpAdmin, consulta);
    }

    @Test
    void crearColaConPrefijoProtegido() {
        assertThrows(RecursoProtegidoException.class, () -> servicio.crearCola(new CrearColaRequest("q.cmd.nueva", true, null, null, null, null)));
        verifyNoInteractions(amqpAdmin, consulta);
    }

    @Test
    void crearColaConDlxProtegido() {
        assertThrows(RecursoProtegidoException.class,
                () -> servicio.crearCola(new CrearColaRequest("demo", true, "cmd.dead.dlx", null, null, null)));
        verifyNoInteractions(amqpAdmin);
    }

    @Test
    void crearColaConDlxInexistente() {
        when(consulta.existeExchange("demo.dlx")).thenReturn(false);
        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.crearCola(new CrearColaRequest("demo", true, "demo.dlx", null, null, null)));
        verifyNoInteractions(amqpAdmin);
    }

    @Test
    void crearColaExistente() {
        when(consulta.existeCola("demo")).thenReturn(true);
        assertThrows(RecursoExistenteException.class, () -> servicio.crearCola(new CrearColaRequest("demo", true, null, null, null, null)));
        verify(amqpAdmin, never()).declareQueue(any());
    }

    @Test
    void crearColaDeclaraArgumentos() {
        when(consulta.existeCola("demo")).thenReturn(false);
        when(consulta.existeExchange("demo.dlx")).thenReturn(true);
        servicio.crearCola(new CrearColaRequest("demo", false, "demo.dlx", "demo.muerta", 60000, 10));
        ArgumentCaptor<Queue> cola = ArgumentCaptor.forClass(Queue.class);
        verify(amqpAdmin).declareQueue(cola.capture());
        assertEquals("demo", cola.getValue().getName());
        assertFalse(cola.getValue().isDurable());
        assertEquals(Map.of("x-dead-letter-exchange", "demo.dlx", "x-dead-letter-routing-key", "demo.muerta",
                "x-message-ttl", 60000, "x-max-length", 10), cola.getValue().getArguments());
    }

    @Test
    void eliminarColaInexistente() {
        when(consulta.existeCola("demo")).thenReturn(false);
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.eliminarCola("demo", false));
        verify(amqpAdmin, never()).deleteQueue(anyString(), anyBoolean(), anyBoolean());
    }

    @Test
    void eliminarColaSinForzarUsaIfUnusedIfEmpty() {
        when(consulta.existeCola("demo")).thenReturn(true);
        servicio.eliminarCola("demo", false);
        verify(amqpAdmin).deleteQueue("demo", true, true);
        verify(amqpAdmin, never()).deleteQueue("demo");
    }

    @Test
    void eliminarColaConMensajesRetornaConflicto() {
        when(consulta.existeCola("demo")).thenReturn(true);
        doThrow(errorAmqp(406)).when(amqpAdmin).deleteQueue("demo", true, true);
        assertThrows(RecursoEnConflictoException.class, () -> servicio.eliminarCola("demo", false));
    }

    @Test
    void eliminarExchangeConBindingsSinForzar() {
        when(consulta.existeExchange("demo.ex")).thenReturn(true);
        when(consulta.tieneBindingsComoOrigen("demo.ex")).thenReturn(true);
        assertThrows(RecursoEnConflictoException.class, () -> servicio.eliminarExchange("demo.ex", false));
        verify(amqpAdmin, never()).deleteExchange(anyString());
    }

    @Test
    void bindingHaciaColaProtegida() {
        assertThrows(RecursoProtegidoException.class, () -> servicio.crearBinding(new BindingRequest("demo.ex", "q.cmd.email", "x")));
        verifyNoInteractions(amqpAdmin, consulta);
    }

    @Test
    void bindingDesdeExchangeProtegido() {
        assertThrows(RecursoProtegidoException.class, () -> servicio.crearBinding(new BindingRequest("cmd.topic", "demo", "email.*")));
        verifyNoInteractions(amqpAdmin, consulta);
    }

    @Test
    void bindingConColaInexistente() {
        when(consulta.existeExchange("demo.ex")).thenReturn(true);
        when(consulta.existeCola("demo")).thenReturn(false);
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.crearBinding(new BindingRequest("demo.ex", "demo", "k")));
        verify(amqpAdmin, never()).declareBinding(any());
    }

    @Test
    void bindingDuplicado() {
        when(consulta.existeExchange("demo.ex")).thenReturn(true);
        when(consulta.existeCola("demo")).thenReturn(true);
        when(consulta.existeBinding("demo.ex", "demo", "k")).thenReturn(true);
        assertThrows(RecursoExistenteException.class, () -> servicio.crearBinding(new BindingRequest("demo.ex", "demo", "k")));
        verify(amqpAdmin, never()).declareBinding(any());
    }

    @Test
    void eliminarColaNombreInvalidoEnRuta() {
        assertThrows(NombreInvalidoException.class, () -> servicio.eliminarCola("Demo", false));
        verifyNoInteractions(amqpAdmin, consulta);
    }

    @Test
    void listarExchangesOmitePredeterminadoYMarcaProtegidos() {
        when(consulta.listarExchanges()).thenReturn(List.of(
                new ExchangeResumen("", "direct", true, false),
                new ExchangeResumen("cmd.direct", "direct", true, false),
                new ExchangeResumen("demo.ex", "fanout", true, false)));
        List<ExchangeResumen> r = servicio.listarExchanges();
        assertEquals(List.of("cmd.direct", "demo.ex"), r.stream().map(ExchangeResumen::nombre).toList());
        assertTrue(r.get(0).protegido());
        assertFalse(r.get(1).protegido());
    }
}
