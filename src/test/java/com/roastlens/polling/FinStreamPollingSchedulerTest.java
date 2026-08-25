package com.roastlens.polling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.roastlens.model.dto.RoastBatchResponse;
import com.roastlens.service.AbnormalEventRoastBatchService;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class FinStreamPollingSchedulerTest {
    private static final RoastBatchResponse RESULT = new RoastBatchResponse(4, 1, 2, 1, List.of());

    @Test
    void pollingDisabledDoesNotCreateSchedulerOrCallBatchService() {
        new ApplicationContextRunner()
                .withUserConfiguration(TestConfiguration.class, FinStreamPollingScheduler.class)
                .withPropertyValues("roastlens.polling.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(FinStreamPollingScheduler.class);
                    verify(context.getBean(AbnormalEventRoastBatchService.class), never())
                            .processAbnormalEvents(Mockito.anyString());
                });
    }

    @Test
    void enabledExecutionPassesConfiguredLanguageAndCanRunAgainAfterSuccess() {
        AbnormalEventRoastBatchService service = Mockito.mock(AbnormalEventRoastBatchService.class);
        when(service.processAbnormalEvents("en-US")).thenReturn(RESULT);
        FinStreamPollingScheduler scheduler = scheduler(service, "en-US");

        scheduler.runPollingCycle();
        scheduler.runPollingCycle();

        verify(service, times(2)).processAbnormalEvents("en-US");
    }

    @Test
    void failureIsCaughtAndGuardIsResetForFutureExecution() {
        AbnormalEventRoastBatchService service = Mockito.mock(AbnormalEventRoastBatchService.class);
        when(service.processAbnormalEvents("zh-CN"))
                .thenThrow(new IllegalStateException("upstream unavailable"))
                .thenReturn(RESULT);
        FinStreamPollingScheduler scheduler = scheduler(service, "zh-CN");

        scheduler.runPollingCycle();
        scheduler.runPollingCycle();

        verify(service, times(2)).processAbnormalEvents("zh-CN");
    }

    @Test
    void overlappingExecutionIsSkipped() throws Exception {
        AbnormalEventRoastBatchService service = Mockito.mock(AbnormalEventRoastBatchService.class);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(service.processAbnormalEvents("zh-CN")).thenAnswer(invocation -> {
            entered.countDown();
            assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
            return RESULT;
        });
        FinStreamPollingScheduler scheduler = scheduler(service, "zh-CN");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            var firstRun = executor.submit(scheduler::runPollingCycle);
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();

            scheduler.runPollingCycle();
            release.countDown();
            firstRun.get(5, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }

        verify(service).processAbnormalEvents("zh-CN");
    }

    private FinStreamPollingScheduler scheduler(AbnormalEventRoastBatchService service, String language) {
        PollingProperties properties = new PollingProperties();
        properties.setLanguage(language);
        return new FinStreamPollingScheduler(service, properties);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(PollingProperties.class)
    static class TestConfiguration {
        @Bean
        AbnormalEventRoastBatchService abnormalEventRoastBatchService() {
            return Mockito.mock(AbnormalEventRoastBatchService.class);
        }
    }
}
