package com.roastlens.roastability;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "roastlens.roastability")
public class RoastabilityProperties {
    private double threshold = 0.6;
    private int maxBatchSize = 20;

    public double getThreshold() { return threshold; }
    public void setThreshold(double threshold) {
        if (threshold < 0 || threshold > 1) throw new IllegalArgumentException("roastability threshold must be between 0 and 1");
        this.threshold = threshold;
    }
    public int getMaxBatchSize() { return maxBatchSize; }
    public void setMaxBatchSize(int maxBatchSize) {
        if (maxBatchSize < 1) throw new IllegalArgumentException("roastability max batch size must be at least 1");
        this.maxBatchSize = maxBatchSize;
    }
}
