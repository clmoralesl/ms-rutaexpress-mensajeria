package cl.rutaexpress.mensajeria.controller;

import cl.rutaexpress.mensajeria.dto.*;
import cl.rutaexpress.mensajeria.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mensajeria")
public class MensajeriaController {

    private final RabbitAdminService adminService;

    public MensajeriaController(RabbitAdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/colas")
    public List<ColaResumen> listarColas() {
        return adminService.listarColas();
    }

    @PostMapping("/colas")
    public ResponseEntity<ColaResumen> crearCola(@Valid @RequestBody CrearColaRequest request) {
        ColaResumen resumen = adminService.crearCola(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resumen);
    }

    @DeleteMapping("/colas/{nombre}")
    public ResponseEntity<Void> eliminarCola(
            @PathVariable String nombre,
            @RequestParam(defaultValue = "false") boolean forzar) {
        adminService.eliminarCola(nombre, forzar);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/exchanges")
    public List<ExchangeResumen> listarExchanges() {
        return adminService.listarExchanges();
    }

    @PostMapping("/exchanges")
    public ResponseEntity<ExchangeResumen> crearExchange(@Valid @RequestBody CrearExchangeRequest request) {
        ExchangeResumen resumen = adminService.crearExchange(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resumen);
    }

    @DeleteMapping("/exchanges/{nombre}")
    public ResponseEntity<Void> eliminarExchange(
            @PathVariable String nombre,
            @RequestParam(defaultValue = "false") boolean forzar) {
        adminService.eliminarExchange(nombre, forzar);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bindings")
    public List<BindingResumen> listarBindings() {
        return adminService.listarBindings();
    }

    @PostMapping("/bindings")
    public ResponseEntity<BindingResumen> crearBinding(@Valid @RequestBody BindingRequest request) {
        BindingResumen resumen = adminService.crearBinding(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resumen);
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Void> eliminarBinding(
            @RequestParam String exchange,
            @RequestParam String cola,
            @RequestParam String clave) {
        adminService.eliminarBinding(exchange, cola, clave);
        return ResponseEntity.noContent().build();
    }
}
