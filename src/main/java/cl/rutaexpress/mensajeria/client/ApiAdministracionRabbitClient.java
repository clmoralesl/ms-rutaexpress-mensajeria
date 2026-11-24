package cl.rutaexpress.mensajeria.client;

import cl.rutaexpress.mensajeria.dto.BindingResumen;
import cl.rutaexpress.mensajeria.dto.ColaResumen;
import cl.rutaexpress.mensajeria.dto.ExchangeResumen;
import cl.rutaexpress.mensajeria.exception.BrokerNoDisponibleException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class ApiAdministracionRabbitClient implements ConsultaTopologia {

    private final RestClient restClient;

    public ApiAdministracionRabbitClient(
            RestClient.Builder builder,
            @Value("${rutaexpress.mensajeria.admin-url}") String url,
            @Value("${spring.rabbitmq.username}") String usuario,
            @Value("${spring.rabbitmq.password}") String clave) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(Duration.ofSeconds(5));
        DefaultUriBuilderFactory uris = new DefaultUriBuilderFactory(url);
        uris.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        this.restClient = builder.requestFactory(fabrica).uriBuilderFactory(uris)
                .defaultHeaders(h -> h.setBasicAuth(usuario, clave)).build();
    }

    @Override
    public List<ColaResumen> listarColas() {
        try {
            List<ColaApi> respuesta = restClient.get()
                    .uri("/api/queues/%2F")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ColaApi>>() {});

            if (respuesta == null) {
                return Collections.emptyList();
            }

            return respuesta.stream()
                    .map(c -> new ColaResumen(
                            c.name(),
                            c.durable() != null && c.durable(),
                            c.messages() != null ? c.messages() : 0L,
                            c.consumers() != null ? c.consumers() : 0,
                            c.arguments() != null ? c.arguments() : Collections.emptyMap(),
                            false
                    ))
                    .toList();
        } catch (RestClientResponseException | ResourceAccessException ex) {
            throw new BrokerNoDisponibleException();
        }
    }

    @Override
    public List<ExchangeResumen> listarExchanges() {
        try {
            List<ExchangeApi> respuesta = restClient.get()
                    .uri("/api/exchanges/%2F")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ExchangeApi>>() {});

            if (respuesta == null) {
                return Collections.emptyList();
            }

            return respuesta.stream()
                    .map(e -> new ExchangeResumen(
                            e.name(),
                            e.type(),
                            e.durable() != null && e.durable(),
                            false
                    ))
                    .toList();
        } catch (RestClientResponseException | ResourceAccessException ex) {
            throw new BrokerNoDisponibleException();
        }
    }

    @Override
    public List<BindingResumen> listarBindings() {
        try {
            List<BindingApi> respuesta = restClient.get()
                    .uri("/api/bindings/%2F")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<BindingApi>>() {});

            if (respuesta == null) {
                return Collections.emptyList();
            }

            return respuesta.stream()
                    .filter(b -> b.destinationType() == null || "queue".equalsIgnoreCase(b.destinationType()))
                    .map(b -> new BindingResumen(
                            b.source() != null ? b.source() : "",
                            b.destination() != null ? b.destination() : "",
                            b.routingKey() != null ? b.routingKey() : "",
                            false
                    ))
                    .toList();
        } catch (RestClientResponseException | ResourceAccessException ex) {
            throw new BrokerNoDisponibleException();
        }
    }

    @Override
    public boolean existeCola(String nombre) {
        try {
            restClient.get()
                    .uri("/api/queues/%2F/{n}", nombre)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound ex) {
            return false;
        } catch (RestClientResponseException | ResourceAccessException ex) {
            throw new BrokerNoDisponibleException();
        }
    }

    @Override
    public boolean existeExchange(String nombre) {
        try {
            restClient.get()
                    .uri("/api/exchanges/%2F/{n}", nombre)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound ex) {
            return false;
        } catch (RestClientResponseException | ResourceAccessException ex) {
            throw new BrokerNoDisponibleException();
        }
    }

    @Override
    public boolean existeBinding(String exchange, String cola, String clave) {
        try {
            List<BindingApi> bindings = restClient.get()
                    .uri("/api/bindings/%2F/e/{ex}/q/{cola}", exchange, cola)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<BindingApi>>() {});

            if (bindings == null || bindings.isEmpty()) {
                return false;
            }

            String claveEsperada = clave != null ? clave : "";
            return bindings.stream().anyMatch(b -> {
                String k = b.routingKey() != null ? b.routingKey() : "";
                return k.equals(claveEsperada);
            });
        } catch (HttpClientErrorException.NotFound ex) {
            return false;
        } catch (RestClientResponseException | ResourceAccessException ex) {
            throw new BrokerNoDisponibleException();
        }
    }

    @Override
    public boolean tieneBindingsComoOrigen(String exchange) {
        try {
            List<BindingApi> bindings = restClient.get()
                    .uri("/api/exchanges/%2F/{ex}/bindings/source", exchange)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<BindingApi>>() {});

            return bindings != null && !bindings.isEmpty();
        } catch (HttpClientErrorException.NotFound ex) {
            return false;
        } catch (RestClientResponseException | ResourceAccessException ex) {
            throw new BrokerNoDisponibleException();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ColaApi(
            String name,
            Boolean durable,
            Long messages,
            Integer consumers,
            Map<String, Object> arguments
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ExchangeApi(
            String name,
            String type,
            Boolean durable
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BindingApi(
            String source,
            String destination,
            @JsonProperty("destination_type") String destinationType,
            @JsonProperty("routing_key") String routingKey
    ) {}
}
