package com.roastlens.polling;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "roastlens.polling")
public class PollingProperties {
    private boolean enabled;

    @Min(1)
    private long intervalMs = 3_600_000;

    @Min(0)
    private long initialDelayMs = 60_000;

    @NotBlank
    private String language = "zh-CN";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public long getIntervalMs() { return intervalMs; }
    public void setIntervalMs(long intervalMs) { this.intervalMs = intervalMs; }
    public long getInitialDelayMs() { return initialDelayMs; }
    public void setInitialDelayMs(long initialDelayMs) { this.initialDelayMs = initialDelayMs; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
