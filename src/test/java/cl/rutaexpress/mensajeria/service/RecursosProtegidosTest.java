package cl.rutaexpress.mensajeria.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecursosProtegidosTest {

    @ParameterizedTest
    @ValueSource(strings = {"q.cmd.email", "q.cmd.email.dlq", "cmd.direct", "cmd.topic", "cmd.dead.dlx", "amq.direct", "amq.topic", ""})
    void reconoceProtegidos(String nombre) {
        assertTrue(RecursosProtegidos.esProtegido(nombre));
    }

    @ParameterizedTest
    @ValueSource(strings = {"demo.cola", "q.cmdx", "cmdb.cola", "amqp-cola", "mi.amq.cola"})
    void noProtegeNombresDeUsuario(String nombre) {
        assertFalse(RecursosProtegidos.esProtegido(nombre));
    }
}
