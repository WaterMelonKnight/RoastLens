package com.roastlens.model.config;

import java.util.ArrayList;
import java.util.List;

public class DomainDefinition {

    private String name;
    private String analysisFocus;
    private List<String> evidencePriority = new ArrayList<>();
    private String disclaimerStyle;
    private List<String> forbiddenClaims = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAnalysisFocus() {
        return analysisFocus;
    }

    public void setAnalysisFocus(String analysisFocus) {
        this.analysisFocus = analysisFocus;
    }

    public List<String> getEvidencePriority() {
        return evidencePriority;
    }

    public void setEvidencePriority(List<String> evidencePriority) {
        this.evidencePriority = evidencePriority;
    }

    public String getDisclaimerStyle() {
        return disclaimerStyle;
    }

    public void setDisclaimerStyle(String disclaimerStyle) {
        this.disclaimerStyle = disclaimerStyle;
    }

    public List<String> getForbiddenClaims() {
        return forbiddenClaims;
    }

    public void setForbiddenClaims(List<String> forbiddenClaims) {
        this.forbiddenClaims = forbiddenClaims;
    }
}
