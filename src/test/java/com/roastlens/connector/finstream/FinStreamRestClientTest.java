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
import java.util.List;

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

    @Test
    void mapsAbnormalEventsUsingFinStreamArrayContract() throws InterruptedException {
        enqueue(200, "[" + actualContractJson("evt-1", "RAPID_DROP") + "]");
        List<FinancialEventInput> events = client.getAbnormalEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getSeverity()).isEqualTo("HIGH");
        assertThat(events.get(0).getMetrics()).containsEntry("return5m", -4);
        assertThat(server.takeRequest().getPath()).isEqualTo("/api/v1/events/abnormal");
    }

    @Test
    void mapsMultipleAbnormalEventsIncludingUnknownType() {
        enqueue(200, "[" + actualContractJson("evt-1", "RAPID_PUMP") + ","
                + actualContractJson("evt-2", "NEW_SIGNAL") + "]");
        assertThat(client.getAbnormalEvents()).extracting(FinancialEventInput::getEventType)
                .containsExactly("RAPID_PUMP", "NEW_SIGNAL");
    }

    @Test
    void mapsEmptyAbnormalList() {
        enqueue(200, "[]");
        assertThat(client.getAbnormalEvents()).isEmpty();
    }

    @Test
    void mapsAbnormalEventMissingOptionalFields() {
        enqueue(200, "[{\"id\":\"evt\",\"source\":\"BINANCE\",\"symbol\":\"BTCUSDT\","
                + "\"eventType\":\"OTHER\",\"summary\":\"summary\"}]");
        FinancialEventInput event = client.getAbnormalEvents().get(0);
        assertThat(event.getSeverity()).isNull();
        assertThat(event.getAnomalyScore()).isNull();
        assertThat(event.getMetrics()).isNull();
    }

    @Test
    void abnormalServerErrorIsStable() {
        enqueue(500, "sensitive upstream body");
        assertThatThrownBy(client::getAbnormalEvents).isInstanceOf(FinStreamClientException.class)
                .hasMessage("FinStream service failed to process the request");
    }

    @Test
    void abnormalTimeoutIsStable() {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
        assertThatThrownBy(client::getAbnormalEvents).isInstanceOf(FinStreamClientException.class)
                .hasMessage("FinStream is unavailable");
    }

    @Test
    void malformedAbnormalJsonIsIncompatible() {
        enqueue(200, "[not-json");
        assertAbnormalIncompatible();
    }

    @Test
    void wrapperObjectIsRejectedAsIncompatibleContract() {
        enqueue(200, "{\"events\":[]}");
        assertAbnormalIncompatible();
    }

    @Test
    void abnormalItemMissingRequiredFieldIsIncompatible() {
        enqueue(200, "[{\"id\":\"evt\"}]");
        assertAbnormalIncompatible();
    }

    private void assertAbnormalIncompatible() {
        assertThatThrownBy(client::getAbnormalEvents).isInstanceOf(FinStreamClientException.class)
                .hasMessage("FinStream returned an incompatible event response");
    }

    private String actualContractJson(String id, String type) {
        return """
                {"id":"%s","source":"BINANCE","symbol":"BTCUSDT","eventType":"%s",
                 "eventTime":"2026-08-20T10:30:00Z","detectedAt":"2026-08-20T10:30:03Z",
                 "severity":"HIGH","anomalyScore":2.0,"summary":"abnormal event",
                 "metrics":{"return5m":-4},"evidence":{"price":100}}
                """.formatted(id, type);
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
