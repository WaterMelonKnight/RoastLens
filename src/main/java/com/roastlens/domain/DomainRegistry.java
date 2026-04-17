package com.roastlens.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.roastlens.model.config.DomainDefinition;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
public class DomainRegistry {

    private final Map<String, DomainDefinition> domainByName = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            DomainConfigFile file = mapper.readValue(new ClassPathResource("domains.yml").getInputStream(), DomainConfigFile.class);
            if (file.getDomains() != null) {
                for (DomainDefinition domain : file.getDomains()) {
                    if (domain.getName() != null && !domain.getName().isBlank()) {
                        domainByName.put(normalize(domain.getName()), domain);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load domains.yml", e);
        }
    }

    public Optional<DomainDefinition> findByName(String name) {
        return Optional.ofNullable(domainByName.get(normalize(name)));
    }

    public List<String> names() {
        return new ArrayList<>(domainByName.keySet());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public static class DomainConfigFile {
        private List<DomainDefinition> domains;

        public List<DomainDefinition> getDomains() {
            return domains;
        }

        public void setDomains(List<DomainDefinition> domains) {
            this.domains = domains;
        }
    }
}
