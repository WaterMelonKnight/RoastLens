package com.roastlens.model.config;

import java.util.ArrayList;
import java.util.List;

public class PersonaDefinition {

    private String name;
    private String tone;
    private List<String> boundaries = new ArrayList<>();
    private String systemPromptStyle;
    private List<String> forbiddenPatterns = new ArrayList<>();
    private List<String> outputHints = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTone() {
        return tone;
    }

    public void setTone(String tone) {
        this.tone = tone;
    }

    public List<String> getBoundaries() {
        return boundaries;
    }

    public void setBoundaries(List<String> boundaries) {
        this.boundaries = boundaries;
    }

    public String getSystemPromptStyle() {
        return systemPromptStyle;
    }

    public void setSystemPromptStyle(String systemPromptStyle) {
        this.systemPromptStyle = systemPromptStyle;
    }

    public List<String> getForbiddenPatterns() {
        return forbiddenPatterns;
    }

    public void setForbiddenPatterns(List<String> forbiddenPatterns) {
        this.forbiddenPatterns = forbiddenPatterns;
    }

    public List<String> getOutputHints() {
        return outputHints;
    }

    public void setOutputHints(List<String> outputHints) {
        this.outputHints = outputHints;
    }
}
