package com.roastlens.model.dto;

import java.util.List;

public class AnalyzeMetaResponse {

    private List<String> domains;
    private List<String> personas;

    public AnalyzeMetaResponse(List<String> domains, List<String> personas) {
        this.domains = domains;
        this.personas = personas;
    }

    public List<String> getDomains() {
        return domains;
    }

    public List<String> getPersonas() {
        return personas;
    }
}
