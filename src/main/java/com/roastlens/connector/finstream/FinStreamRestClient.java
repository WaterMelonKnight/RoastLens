package com.roastlens.connector.finstream;

import com.roastlens.financial.FinancialEventInput;
import com.roastlens.financial.FinancialEventSource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Component
public class FinStreamRestClient implements FinancialEventSource {
    private final WebClient webClient;
    private final Duration timeout;

    @Autowired
    public FinStreamRestClient(FinStreamProperties properties) {
        this(WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                        .responseTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))))
                .build(), Duration.ofSeconds(properties.getTimeoutSeconds()));
    }

    FinStreamRestClient(WebClient webClient, Duration timeout) {
        this.webClient = webClient;
        this.timeout = timeout;
    }

    @Override
    public FinancialEventInput getEvent(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId is required");
        }
        try {
            FinStreamEventResponse response = webClient.get()
                    .uri(builder -> builder.path("/api/v1/events/{eventId}").build(eventId))
                    .exchangeToMono(result -> decodeResponse(eventId, result.statusCode(), result.bodyToMono(FinStreamEventResponse.class)))
                    .timeout(timeout)
                    .block();
            if (response == null) {
                throw invalidResponse();
            }
            return map(response);
        } catch (FinStreamClientException ex) {
            throw ex;
        } catch (Exception ex) {
            Throwable cause = Exceptions.unwrap(ex);
            throw new FinStreamClientException("FinStream is unavailable", cause);
        }
    }

    private Mono<FinStreamEventResponse> decodeResponse(String eventId, HttpStatusCode status,
                                                         Mono<FinStreamEventResponse> body) {
        if (status.value() == 404) {
            return Mono.error(new FinStreamEventNotFoundException(eventId));
        }
        if (status.is4xxClientError()) {
            return Mono.error(new FinStreamClientException("FinStream rejected the event request"));
        }
        if (status.is5xxServerError()) {
            return Mono.error(new FinStreamClientException("FinStream service failed to process the request"));
        }
        if (!status.is2xxSuccessful()) {
            return Mono.error(new FinStreamClientException("FinStream returned an unexpected response"));
        }
        return body.switchIfEmpty(Mono.error(invalidResponse()))
                .onErrorMap(ex -> ex instanceof FinStreamClientException ? ex : invalidResponse(ex));
    }

    private FinancialEventInput map(FinStreamEventResponse source) {
        if (isBlank(source.id()) || isBlank(source.source()) || isBlank(source.symbol())
                || isBlank(source.eventType()) || isBlank(source.summary())) {
            throw invalidResponse();
        }
        FinancialEventInput event = new FinancialEventInput();
        event.setId(source.id());
        event.setSource(source.source());
        event.setSymbol(source.symbol());
        event.setEventType(source.eventType());
        event.setEventTime(source.eventTime());
        event.setDetectedAt(source.detectedAt());
        event.setSeverity(source.severity());
        event.setAnomalyScore(source.anomalyScore());
        event.setSummary(source.summary());
        event.setMetrics(source.metrics());
        return event;
    }

    private boolean isBlank(String value) { return value == null || value.isBlank(); }
    private FinStreamClientException invalidResponse() {
        return new FinStreamClientException("FinStream returned an incompatible event response");
    }
    private FinStreamClientException invalidResponse(Throwable cause) {
        return new FinStreamClientException("FinStream returned an incompatible event response", cause);
    }
}
