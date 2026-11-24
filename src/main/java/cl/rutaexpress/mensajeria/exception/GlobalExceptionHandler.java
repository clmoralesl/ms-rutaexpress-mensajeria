package cl.rutaexpress.mensajeria.exception;

import cl.rutaexpress.mensajeria.dto.ErrorRespuesta;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NombreInvalidoException.class)
    public ResponseEntity<ErrorRespuesta> handleNombreInvalido(NombreInvalidoException ex, HttpServletRequest request) {
        log.warn("Nombre o argumento invalido en {}: {}", request.getRequestURI(), ex.getMessage());
        return construirRespuesta(HttpStatus.BAD_REQUEST, ex.getMessage(), null, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> handleValidacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Fallo de validacion en {}: {}", request.getRequestURI(), ex.getMessage());
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            campos.put(fe.getField(), fe.getDefaultMessage());
        }
        return construirRespuesta(HttpStatus.BAD_REQUEST, "Error de validacion en los campos de la solicitud", campos, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespuesta> handleJsonMalformado(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Cuerpo JSON no legible en {}: {}", request.getRequestURI(), ex.getMessage());
        return construirRespuesta(HttpStatus.BAD_REQUEST, "Cuerpo JSON invalido o con tipos incorrectos", null, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorRespuesta> handleFaltaParametro(MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("Falta parametro de consulta en {}: {}", request.getRequestURI(), ex.getParameterName());
        return construirRespuesta(HttpStatus.BAD_REQUEST, "Falta el parametro requerido: " + ex.getParameterName(), null, request);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorRespuesta> handleNoEncontrado(RecursoNoEncontradoException ex, HttpServletRequest request) {
        log.warn("Recurso no encontrado en {}: {}", request.getRequestURI(), ex.getMessage());
        return construirRespuesta(HttpStatus.NOT_FOUND, ex.getMessage(), null, request);
    }

    @ExceptionHandler(RecursoProtegidoException.class)
    public ResponseEntity<ErrorRespuesta> handleProtegido(RecursoProtegidoException ex, HttpServletRequest request) {
        log.warn("Intento de acceso a recurso protegido en {}: {}", request.getRequestURI(), ex.getMessage());
        return construirRespuesta(HttpStatus.CONFLICT, ex.getMessage(), null, request);
    }

    @ExceptionHandler(RecursoExistenteException.class)
    public ResponseEntity<ErrorRespuesta> handleExistente(RecursoExistenteException ex, HttpServletRequest request) {
        log.warn("Conflicto por recurso existente en {}: {}", request.getRequestURI(), ex.getMessage());
        return construirRespuesta(HttpStatus.CONFLICT, ex.getMessage(), null, request);
    }

    @ExceptionHandler(RecursoEnConflictoException.class)
    public ResponseEntity<ErrorRespuesta> handleConflicto(RecursoEnConflictoException ex, HttpServletRequest request) {
        log.warn("Conflicto en operacion sobre recurso en {}: {}", request.getRequestURI(), ex.getMessage());
        return construirRespuesta(HttpStatus.CONFLICT, ex.getMessage(), null, request);
    }

    @ExceptionHandler(BrokerNoDisponibleException.class)
    public ResponseEntity<ErrorRespuesta> handleBrokerNoDisponible(BrokerNoDisponibleException ex, HttpServletRequest request) {
        log.error("Broker de mensajeria no disponible en {}: {}", request.getRequestURI(), ex.getMessage());
        return construirRespuesta(HttpStatus.SERVICE_UNAVAILABLE, "El broker de mensajeria no esta disponible; intente nuevamente", null, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorRespuesta> handleTipoInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("Parametro con tipo invalido en {}: {}", request.getRequestURI(), ex.getName());
        return construirRespuesta(HttpStatus.BAD_REQUEST, "El parametro '" + ex.getName() + "' tiene un valor invalido", null, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> handleGenerico(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse errorSpring) {
            HttpStatus estado = HttpStatus.valueOf(errorSpring.getStatusCode().value());
            log.warn("Solicitud rechazada por el framework en {}: {}", request.getRequestURI(), estado);
            return construirRespuesta(estado, mensajePara(estado), null, request);
        }
        log.error("Error no controlado en {}: ", request.getRequestURI(), ex);
        return construirRespuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Ha ocurrido un error interno del servidor", null, request);
    }

    private static String mensajePara(HttpStatus estado) {
        return switch (estado) {
            case NOT_FOUND -> "El recurso solicitado no existe";
            case METHOD_NOT_ALLOWED -> "Metodo HTTP no permitido para esta ruta";
            case UNSUPPORTED_MEDIA_TYPE -> "Tipo de contenido no soportado; use application/json";
            default -> "Solicitud invalida";
        };
    }

    private ResponseEntity<ErrorRespuesta> construirRespuesta(
            HttpStatus status,
            String mensaje,
            Map<String, String> campos,
            HttpServletRequest request) {
        ErrorRespuesta error = new ErrorRespuesta(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                mensaje,
                campos,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(error);
    }
}
