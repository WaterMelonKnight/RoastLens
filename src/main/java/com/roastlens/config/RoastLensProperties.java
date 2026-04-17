package com.roastlens.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "roastlens")
public class RoastLensProperties {

    private final Llm llm = new Llm();

    public Llm getLlm() {
        return llm;
    }

    public static class Llm {
        private String provider = "openai-compatible";
        private String baseUrl = "https://api.openai.com";
        private String apiKey = "";
        private String model = "gpt-4o-mini";
        private double temperature = 0.4;
        private int timeoutSeconds = 45;
        private boolean useJsonResponseFormat = false;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public boolean isUseJsonResponseFormat() {
            return useJsonResponseFormat;
        }

        public void setUseJsonResponseFormat(boolean useJsonResponseFormat) {
            this.useJsonResponseFormat = useJsonResponseFormat;
        }
    }
}
