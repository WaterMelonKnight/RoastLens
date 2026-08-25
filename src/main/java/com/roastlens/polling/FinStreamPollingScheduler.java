package com.roastlens.polling;

import com.roastlens.model.dto.RoastBatchResponse;
import com.roastlens.service.AbnormalEventRoastBatchService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "roastlens.polling", name = "enabled", havingValue = "true")
public class FinStreamPollingScheduler {
    private static final Logger log = LoggerFactory.getLogger(FinStreamPollingScheduler.class);

    private final AbnormalEventRoastBatchService batchService;
    private final PollingProperties properties;
    private final AtomicBoolean running = new AtomicBoolean();

    public FinStreamPollingScheduler(AbnormalEventRoastBatchService batchService, PollingProperties properties) {
        this.batchService = batchService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${roastlens.polling.interval-ms:3600000}",
            initialDelayString = "${roastlens.polling.initial-delay-ms:60000}")
    public void runPollingCycle() {
        if (!running.compareAndSet(false, true)) {
            log.info("FinStream polling skipped because previous run is still active");
            return;
        }

        long startedAt = System.nanoTime();
        log.info("FinStream polling started");
        try {
            RoastBatchResponse result = batchService.processAbnormalEvents(properties.getLanguage());
            log.info("FinStream polling completed processed={} generated={} skipped={} errors={} durationMs={}",
                    result.processed(), result.generated(), result.skipped(), result.errors(), elapsedMillis(startedAt));
        } catch (Exception exception) {
            log.error("FinStream polling failed durationMs={} reason={}", elapsedMillis(startedAt),
                    exception.getMessage());
        } finally {
            running.set(false);
        }
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
