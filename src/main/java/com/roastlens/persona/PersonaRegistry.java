package com.roastlens.persona;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.roastlens.model.config.PersonaDefinition;
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
public class PersonaRegistry {

    private final Map<String, PersonaDefinition> personaByName = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            PersonaConfigFile file = mapper.readValue(new ClassPathResource("personas.yml").getInputStream(), PersonaConfigFile.class);
            if (file.getPersonas() != null) {
                for (PersonaDefinition persona : file.getPersonas()) {
                    if (persona.getName() != null && !persona.getName().isBlank()) {
                        personaByName.put(normalize(persona.getName()), persona);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load personas.yml", e);
        }
    }

    public Optional<PersonaDefinition> findByName(String name) {
        return Optional.ofNullable(personaByName.get(normalize(name)));
    }

    public List<String> names() {
        return new ArrayList<>(personaByName.keySet());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public static class PersonaConfigFile {
        private List<PersonaDefinition> personas;

        public List<PersonaDefinition> getPersonas() {
            return personas;
        }

        public void setPersonas(List<PersonaDefinition> personas) {
            this.personas = personas;
        }
    }
}
