package com.roastlens.generation;

import com.roastlens.config.RoastLensProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenerationOptionsResolverTest {
    @Test void defaultsToZhCn() {
        assertThat(new GenerationOptionsResolver(new RoastLensProperties()).resolve(null).language()).isEqualTo("zh-CN");
    }

    @Test void requestLanguageOverridesConfiguration() {
        RoastLensProperties properties = new RoastLensProperties();
        properties.getContent().setDefaultLanguage("en-US");
        GenerationOptionsResolver resolver = new GenerationOptionsResolver(properties);
        assertThat(resolver.resolve("zh-CN").language()).isEqualTo("zh-CN");
        assertThat(resolver.resolve("en-US").language()).isEqualTo("en-US");
    }

    @Test void rejectsUnsupportedLanguageWithStableMessage() {
        assertThatThrownBy(() -> new GenerationOptionsResolver(new RoastLensProperties()).resolve("abc"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Unsupported language: abc");
    }
}
