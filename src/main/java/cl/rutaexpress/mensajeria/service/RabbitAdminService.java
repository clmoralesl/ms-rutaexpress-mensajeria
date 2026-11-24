package cl.rutaexpress.mensajeria.service;

import cl.rutaexpress.mensajeria.client.ConsultaTopologia;
import cl.rutaexpress.mensajeria.dto.*;
import cl.rutaexpress.mensajeria.exception.NombreInvalidoException;
import cl.rutaexpress.mensajeria.exception.RecursoEnConflictoException;
import cl.rutaexpress.mensajeria.exception.RecursoExistenteException;
import cl.rutaexpress.mensajeria.exception.RecursoNoEncontradoException;
import cl.rutaexpress.mensajeria.exception.RecursoProtegidoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.*;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);

    private final AmqpAdmin amqpAdmin;
    private final ConsultaTopologia consulta;
    private final TraductorErroresRabbit traductor;

    public RabbitAdminService(
            AmqpAdmin amqpAdmin,
            ConsultaTopologia consulta,
            TraductorErroresRabbit traductor) {
        this.amqpAdmin = amqpAdmin;
        this.consulta = consulta;
        this.traductor = traductor;
    }

    public List<ColaResumen> listarColas() {
        return consulta.listarColas().stream()
                .map(c -> new ColaResumen(
                        c.nombre(),
                        c.durable(),
                        c.mensajes(),
                        c.consumidores(),
                        c.argumentos(),
                        RecursosProtegidos.esProtegido(c.nombre())
                ))
                .toList();
    }

    public ColaResumen crearCola(CrearColaRequest request) {
        ReglasNombre.validarNombre("nombre", request.nombre());

        if (RecursosProtegidos.esProtegido(request.nombre())) {
            throw new RecursoProtegidoException("La cola '" + request.nombre() + "' pertenece a la topologia del sistema y no se puede crear");
        }

        if (request.exchangeMuertos() != null) {
            if (RecursosProtegidos.esProtegido(request.exchangeMuertos())) {
                throw new RecursoProtegidoException("El exchange dead-letter '" + request.exchangeMuertos() + "' pertenece a la topologia del sistema y no se puede utilizar");
            }
            if (!consulta.existeExchange(request.exchangeMuertos())) {
                throw new RecursoNoEncontradoException("El exchange dead-letter '" + request.exchangeMuertos() + "' no existe");
            }
        }

        if (consulta.existeCola(request.nombre())) {
            throw new RecursoExistenteException("La cola '" + request.nombre() + "' ya existe");
        }

        Map<String, Object> args = new LinkedHashMap<>();
        if (request.exchangeMuertos() != null) {
            args.put("x-dead-letter-exchange", request.exchangeMuertos());
        }
        if (request.claveMuertos() != null) {
            args.put("x-dead-letter-routing-key", request.claveMuertos());
        }
        if (request.ttlMs() != null) {
            args.put("x-message-ttl", request.ttlMs());
        }
        if (request.largoMaximo() != null) {
            args.put("x-max-length", request.largoMaximo());
        }

        boolean durable = request.durable() == null || request.durable();

        ejecutar("la cola '" + request.nombre() + "'", () ->
                amqpAdmin.declareQueue(new Queue(request.nombre(), durable, false, false, args))
        );

        log.info("Cola creada: {}", request.nombre());
        return new ColaResumen(request.nombre(), durable, 0L, 0, args, false);
    }

    public void eliminarCola(String nombre, boolean forzar) {
        ReglasNombre.validarNombre("nombre", nombre);

        if (RecursosProtegidos.esProtegido(nombre)) {
            throw new RecursoProtegidoException("La cola '" + nombre + "' pertenece a la topologia del sistema y no se puede eliminar");
        }

        if (!consulta.existeCola(nombre)) {
            throw new RecursoNoEncontradoException("La cola '" + nombre + "' no existe");
        }

        ejecutar("la cola '" + nombre + "'", () -> {
            if (forzar) {
                amqpAdmin.deleteQueue(nombre);
            } else {
                amqpAdmin.deleteQueue(nombre, true, true);
            }
        });

        log.info("Cola eliminada: {} (forzar={})", nombre, forzar);
    }

    public List<ExchangeResumen> listarExchanges() {
        return consulta.listarExchanges().stream()
                .filter(e -> e.nombre() != null && !e.nombre().isEmpty())
                .map(e -> new ExchangeResumen(
                        e.nombre(),
                        e.tipo(),
                        e.durable(),
                        RecursosProtegidos.esProtegido(e.nombre())
                ))
                .toList();
    }

    public ExchangeResumen crearExchange(CrearExchangeRequest request) {
        ReglasNombre.validarNombre("nombre", request.nombre());

        if (RecursosProtegidos.esProtegido(request.nombre())) {
            throw new RecursoProtegidoException("El exchange '" + request.nombre() + "' pertenece a la topologia del sistema y no se puede crear");
        }

        if (consulta.existeExchange(request.nombre())) {
            throw new RecursoExistenteException("El exchange '" + request.nombre() + "' ya existe");
        }

        boolean durable = request.durable() == null || request.durable();

        Exchange ex = switch (request.tipo().toLowerCase()) {
            case "direct" -> new DirectExchange(request.nombre(), durable, false);
            case "topic" -> new TopicExchange(request.nombre(), durable, false);
            case "fanout" -> new FanoutExchange(request.nombre(), durable, false);
            default -> throw new NombreInvalidoException("tipo: debe ser direct, topic o fanout");
        };

        ejecutar("el exchange '" + request.nombre() + "'", () -> amqpAdmin.declareExchange(ex));

        log.info("Exchange creado: {} (tipo={})", request.nombre(), request.tipo());
        return new ExchangeResumen(request.nombre(), request.tipo(), durable, false);
    }

    public void eliminarExchange(String nombre, boolean forzar) {
        ReglasNombre.validarNombre("nombre", nombre);

        if (RecursosProtegidos.esProtegido(nombre)) {
            throw new RecursoProtegidoException("El exchange '" + nombre + "' pertenece a la topologia del sistema y no se puede eliminar");
        }

        if (!consulta.existeExchange(nombre)) {
            throw new RecursoNoEncontradoException("El exchange '" + nombre + "' no existe");
        }

        if (!forzar && consulta.tieneBindingsComoOrigen(nombre)) {
            throw new RecursoEnConflictoException("El exchange '" + nombre + "' tiene enlaces asociados; use forzar=true para eliminarlo");
        }

        ejecutar("el exchange '" + nombre + "'", () -> amqpAdmin.deleteExchange(nombre));

        log.info("Exchange eliminado: {} (forzar={})", nombre, forzar);
    }

    public List<BindingResumen> listarBindings() {
        return consulta.listarBindings().stream()
                .filter(b -> b.exchange() != null && !b.exchange().isEmpty())
                .map(b -> new BindingResumen(
                        b.exchange(),
                        b.cola(),
                        b.clave(),
                        RecursosProtegidos.esProtegido(b.exchange()) || RecursosProtegidos.esProtegido(b.cola())
                ))
                .toList();
    }

    public BindingResumen crearBinding(BindingRequest request) {
        ReglasNombre.validarNombre("exchange", request.exchange());
        ReglasNombre.validarNombre("cola", request.cola());
        ReglasNombre.validarClave(request.clave());

        if (RecursosProtegidos.esProtegido(request.exchange()) || RecursosProtegidos.esProtegido(request.cola())) {
            throw new RecursoProtegidoException("No se permite enlazar recursos protegidos del sistema");
        }

        if (!consulta.existeExchange(request.exchange())) {
            throw new RecursoNoEncontradoException("El exchange '" + request.exchange() + "' no existe");
        }

        if (!consulta.existeCola(request.cola())) {
            throw new RecursoNoEncontradoException("La cola '" + request.cola() + "' no existe");
        }

        if (consulta.existeBinding(request.exchange(), request.cola(), request.clave())) {
            throw new RecursoExistenteException("El enlace ya existe");
        }

        ejecutar("el enlace entre '" + request.exchange() + "' y '" + request.cola() + "'", () ->
                amqpAdmin.declareBinding(new Binding(request.cola(), Binding.DestinationType.QUEUE, request.exchange(), request.clave(), null))
        );

        log.info("Enlace creado: {} -> {} con clave '{}'", request.exchange(), request.cola(), request.clave());
        return new BindingResumen(request.exchange(), request.cola(), request.clave(), false);
    }

    public void eliminarBinding(String exchange, String cola, String clave) {
        ReglasNombre.validarNombre("exchange", exchange);
        ReglasNombre.validarNombre("cola", cola);
        ReglasNombre.validarClave(clave);

        if (RecursosProtegidos.esProtegido(exchange) || RecursosProtegidos.esProtegido(cola)) {
            throw new RecursoProtegidoException("No se permite modificar enlaces sobre recursos protegidos del sistema");
        }

        if (!consulta.existeBinding(exchange, cola, clave)) {
            throw new RecursoNoEncontradoException("El enlace no existe");
        }

        ejecutar("el enlace entre '" + exchange + "' y '" + cola + "'", () ->
                amqpAdmin.removeBinding(new Binding(cola, Binding.DestinationType.QUEUE, exchange, clave, null))
        );

        log.info("Enlace eliminado: {} -> {} con clave '{}'", exchange, cola, clave);
    }

    private void ejecutar(String contexto, Runnable accion) {
        try {
            accion.run();
        } catch (AmqpException ex) {
            throw traductor.traducir(ex, contexto);
        }
    }
}
