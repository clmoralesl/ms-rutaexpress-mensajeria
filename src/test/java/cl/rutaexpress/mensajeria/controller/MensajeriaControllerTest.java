package cl.rutaexpress.mensajeria.controller;

import cl.rutaexpress.mensajeria.config.WebMvcSecurityConfig;
import cl.rutaexpress.mensajeria.dto.ColaResumen;
import cl.rutaexpress.mensajeria.exception.*;
import cl.rutaexpress.mensajeria.security.AutenticacionInternaInterceptor;
import cl.rutaexpress.mensajeria.service.RabbitAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MensajeriaController.class)
@Import({WebMvcSecurityConfig.class, AutenticacionInternaInterceptor.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "rutaexpress.secreto.interno=prueba-secreto-interno-de-32-caracteres-min")
class MensajeriaControllerTest {

    private static final String CABECERA_SECRETO = "X-Servicio-Interno";
    private static final String SECRETO_VALIDO = "prueba-secreto-interno-de-32-caracteres-min";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RabbitAdminService servicio;

    @Test
    void sinCabeceraInternaRetorna401() throws Exception {
        mockMvc.perform(get("/api/mensajeria/colas"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(servicio);
    }

    @Test
    void cabeceraIncorrectaRetorna401() throws Exception {
        mockMvc.perform(get("/api/mensajeria/colas")
                        .header(CABECERA_SECRETO, "secreto-completamente-incorrecto-de-32-caracteres"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(servicio);
    }

    @Test
    void crearColaNombreVacioRetorna400() throws Exception {
        mockMvc.perform(post("/api/mensajeria/colas")
                        .header(CABECERA_SECRETO, SECRETO_VALIDO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"durable\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nombre").exists());
        verifyNoInteractions(servicio);
    }

    @Test
    void ttlFueraDeRangoRetorna400() throws Exception {
        mockMvc.perform(post("/api/mensajeria/colas")
                        .header(CABECERA_SECRETO, SECRETO_VALIDO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"demo\",\"ttlMs\":99999999999}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Cuerpo JSON invalido o con tipos incorrectos"));
    }

    @Test
    void jsonMalformadoRetorna400() throws Exception {
        mockMvc.perform(post("/api/mensajeria/colas")
                        .header(CABECERA_SECRETO, SECRETO_VALIDO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Cuerpo JSON invalido o con tipos incorrectos"));
    }

    @Test
    void tipoHeadersRetorna400() throws Exception {
        mockMvc.perform(post("/api/mensajeria/exchanges")
                        .header(CABECERA_SECRETO, SECRETO_VALIDO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"demo.ex\",\"tipo\":\"headers\",\"durable\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.tipo").exists());
    }

    @Test
    void crearColaRetorna201() throws Exception {
        when(servicio.crearCola(any())).thenReturn(new ColaResumen("demo", true, 0L, 0, Map.of(), false));

        mockMvc.perform(post("/api/mensajeria/colas")
                        .header(CABECERA_SECRETO, SECRETO_VALIDO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"demo\",\"durable\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("demo"));
    }

    @Test
    void eliminarColaRetorna204() throws Exception {
        // Sin parametro forzar (default false)
        mockMvc.perform(delete("/api/mensajeria/colas/demo")
                        .header(CABECERA_SECRETO, SECRETO_VALIDO))
                .andExpect(status().isNoContent());
        verify(servicio).eliminarCola("demo", false);

        // Con parametro forzar=true
        mockMvc.perform(delete("/api/mensajeria/colas/demo")
                        .param("forzar", "true")
                        .header(CABECERA_SECRETO, SECRETO_VALIDO))
                .andExpect(status().isNoContent());
        verify(servicio).eliminarCola("demo", true);
    }

    @Test
    void eliminarBindingSinParametroRetorna400() throws Exception {
        mockMvc.perform(delete("/api/mensajeria/bindings")
                        .param("exchange", "demo.ex")
                        .param("clave", "k")
                        .header(CABECERA_SECRETO, SECRETO_VALIDO))
                .andExpect(status().isBadRequest());
    }

    @Test
    void servicioLanzaRecursoProtegidoRetorna409() throws Exception {
        doThrow(new RecursoProtegidoException("Protegido")).when(servicio).eliminarCola("cmd.direct", false);

        mockMvc.perform(delete("/api/mensajeria/colas/cmd.direct")
                        .header(CABECERA_SECRETO, SECRETO_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Protegido"));
    }

    @Test
    void servicioLanzaRecursoNoEncontradoRetorna404() throws Exception {
        doThrow(new RecursoNoEncontradoException("No existe")).when(servicio).eliminarCola("inexistente", false);

        mockMvc.perform(delete("/api/mensajeria/colas/inexistente")
                        .header(CABECERA_SECRETO, SECRETO_VALIDO))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No existe"));
    }

    @Test
    void servicioLanzaRecursoExistenteRetorna409() throws Exception {
        doThrow(new RecursoExistenteException("Ya existe")).when(servicio).crearCola(any());

        mockMvc.perform(post("/api/mensajeria/colas")
                        .header(CABECERA_SECRETO, SECRETO_VALIDO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"demo\",\"durable\":true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe"));
    }

    @Test
    void servicioLanzaBrokerNoDisponibleRetorna503() throws Exception {
        doThrow(new BrokerNoDisponibleException()).when(servicio).listarColas();

        mockMvc.perform(get("/api/mensajeria/colas")
                        .header(CABECERA_SECRETO, SECRETO_VALIDO))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensaje").value("El broker de mensajeria no esta disponible; intente nuevamente"));
    }

    @Test
    void errorNoControladoRetorna500SinDetalleInterno() throws Exception {
        doThrow(new RuntimeException("detalle interno sensible")).when(servicio).listarColas();

        mockMvc.perform(get("/api/mensajeria/colas")
                        .header(CABECERA_SECRETO, SECRETO_VALIDO))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.mensaje").value("Ha ocurrido un error interno del servidor"))
                .andExpect(content().string(not(containsString("detalle interno sensible"))));
    }

    @Test
    void rutaNoMapeadaRetorna404() throws Exception {
        mockMvc.perform(get("/api/mensajeria/inexistente").header(CABECERA_SECRETO, SECRETO_VALIDO))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("El recurso solicitado no existe"));
        verifyNoInteractions(servicio);
    }

    @Test
    void metodoNoSoportadoRetorna405() throws Exception {
        mockMvc.perform(put("/api/mensajeria/colas").header(CABECERA_SECRETO, SECRETO_VALIDO))
                .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(servicio);
    }

    @Test
    void tipoDeContenidoNoSoportadoRetorna415() throws Exception {
        mockMvc.perform(post("/api/mensajeria/colas").header(CABECERA_SECRETO, SECRETO_VALIDO)
                        .contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType());
        verifyNoInteractions(servicio);
    }

    @Test
    void forzarNoBooleanoRetorna400() throws Exception {
        mockMvc.perform(delete("/api/mensajeria/colas/demo").param("forzar", "abc").header(CABECERA_SECRETO, SECRETO_VALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El parametro 'forzar' tiene un valor invalido"));
        verifyNoInteractions(servicio);
    }
}
