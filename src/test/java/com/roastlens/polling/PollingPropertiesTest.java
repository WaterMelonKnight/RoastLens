package com.roastlens.polling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class PollingPropertiesTest {
    @Test
    void bindsAllPollingConfiguration() {
        var source = new MapConfigurationPropertySource(Map.of(
                "roastlens.polling.enabled", "true",
                "roastlens.polling.interval-ms", "45000",
                "roastlens.polling.initial-delay-ms", "2500",
                "roastlens.polling.language", "en-US"));

        PollingProperties properties = new Binder(source)
                .bind("roastlens.polling", PollingProperties.class)
                .orElseThrow(() -> new AssertionError("polling properties did not bind"));

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getIntervalMs()).isEqualTo(45_000);
        assertThat(properties.getInitialDelayMs()).isEqualTo(2_500);
        assertThat(properties.getLanguage()).isEqualTo("en-US");
    }

    @Test
    void hasSafeDefaults() {
        PollingProperties properties = new PollingProperties();

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getIntervalMs()).isEqualTo(3_600_000);
        assertThat(properties.getInitialDelayMs()).isEqualTo(60_000);
        assertThat(properties.getLanguage()).isEqualTo("zh-CN");
    }
}
