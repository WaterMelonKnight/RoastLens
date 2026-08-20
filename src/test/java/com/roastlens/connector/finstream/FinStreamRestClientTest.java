package com.roastlens.connector.finstream;

import com.roastlens.financial.FinancialEventInput;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FinStreamRestClientTest {
    private MockWebServer server;
    private FinStreamRestClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        client = new FinStreamRestClient(WebClient.builder().baseUrl(server.url("/").toString()).build(),
                Duration.ofSeconds(1));
    }

    @AfterEach
    void tearDown() throws IOException { server.shutdown(); }

    @Test
    void mapsRapidDropAndMetrics() throws InterruptedException {
        enqueue(200, validJson("RAPID_DROP", "\"metrics\":{\"return5m\":-5.8,\"volumeRatio\":7.3}"));
        FinancialEventInput event = client.getEvent("evt/123");

        assertThat(event.getId()).isEqualTo("evt-123");
        assertThat(event.getEventType()).isEqualTo("RAPID_DROP");
        assertThat(event.getEventTime()).hasToString("2026-08-20T10:30:00Z");
        assertThat(event.getMetrics()).containsEntry("return5m", -5.8).containsEntry("volumeRatio", 7.3);
        assertThat(server.takeRequest().getPath()).isEqualTo("/api/v1/events/evt%2F123");
    }

    @Test
    void acceptsUnknownEventType() {
        enqueue(200, validJson("FLASH_CRASH", "\"metrics\":{}"));
        assertThat(client.getEvent("evt-123").getEventType()).isEqualTo("FLASH_CRASH");
    }

    @Test
    void mapsMissingMetrics() {
        enqueue(200, validJson("RAPID_PUMP", ""));
        assertThat(client.getEvent("evt-123").getMetrics()).isNull();
    }

    @Test
    void mapsNotFound() {
        enqueue(404, "not found");
        assertThatThrownBy(() -> client.getEvent("missing"))
                .isInstanceOf(FinStreamEventNotFoundException.class)
                .hasMessage("FinStream event not found: missing");
    }

    @Test
    void mapsServerErrorWithoutLeakingBody() {
        enqueue(500, "database password and a very large stack trace");
        assertThatThrownBy(() -> client.getEvent("evt"))
                .isInstanceOf(FinStreamClientException.class)
                .hasMessage("FinStream service failed to process the request");
    }

    @Test
    void mapsOtherClientErrorWithoutLeakingBody() {
        enqueue(400, "sensitive validation details");
        assertThatThrownBy(() -> client.getEvent("evt"))
                .isInstanceOf(FinStreamClientException.class)
                .hasMessage("FinStream rejected the event request");
    }

    @Test
    void mapsTimeout() {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
        assertThatThrownBy(() -> client.getEvent("evt"))
                .isInstanceOf(FinStreamClientException.class).hasMessage("FinStream is unavailable");
    }

    @Test
    void mapsNetworkFailure() throws IOException {
        server.shutdown();
        assertThatThrownBy(() -> client.getEvent("evt"))
                .isInstanceOf(FinStreamClientException.class).hasMessage("FinStream is unavailable");
    }

    @Test
    void rejectsEmptyBody() {
        enqueue(200, "");
        assertIncompatibleResponse();
    }

    @Test
    void rejectsMalformedJson() {
        enqueue(200, "{not-json");
        assertIncompatibleResponse();
    }

    @Test
    void rejectsMissingRequiredContractField() {
        enqueue(200, "{\"id\":\"evt\",\"source\":\"BINANCE\"}");
        assertIncompatibleResponse();
    }

    private void assertIncompatibleResponse() {
        assertThatThrownBy(() -> client.getEvent("evt"))
                .isInstanceOf(FinStreamClientException.class)
                .hasMessage("FinStream returned an incompatible event response");
    }

    private void enqueue(int status, String body) {
        server.enqueue(new MockResponse().setResponseCode(status)
                .setHeader("Content-Type", "application/json").setBody(body));
    }

    private String validJson(String type, String optionalTail) {
        String tail = optionalTail.isEmpty() ? "" : "," + optionalTail;
        return """
                {"id":"evt-123","source":"BINANCE","symbol":"BTCUSDT","eventType":"%s",
                 "eventTime":"2026-08-20T10:30:00Z","detectedAt":"2026-08-20T10:30:03Z",
                 "severity":0.9,"anomalyScore":1.8,"summary":"BTC moved rapidly"%s,
                 "futureFinStreamField":"ignored"}
                """.formatted(type, tail);
    }
}
