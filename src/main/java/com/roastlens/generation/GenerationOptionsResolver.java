package com.roastlens.generation;

import com.roastlens.config.RoastLensProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class GenerationOptionsResolver {
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("zh-CN", "en-US");
    private final String defaultLanguage;

    public GenerationOptionsResolver(RoastLensProperties properties) {
        this.defaultLanguage = requireSupported(properties.getContent().getDefaultLanguage());
    }

    public GenerationOptions resolve(String requestLanguage) {
        return new GenerationOptions(requireSupported(
                requestLanguage == null || requestLanguage.isBlank() ? defaultLanguage : requestLanguage));
    }

    private String requireSupported(String language) {
        if (!SUPPORTED_LANGUAGES.contains(language)) {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }
        return language;
    }
}
