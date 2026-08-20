package com.roastlens.model.dto;

public class RoastCandidate {

    private final String text;
    private final String style;
    private final String riskLevel;

    public RoastCandidate(String text, String style, String riskLevel) {
        this.text = text;
        this.style = style;
        this.riskLevel = riskLevel;
    }

    public String getText() { return text; }
    public String getStyle() { return style; }
    public String getRiskLevel() { return riskLevel; }
}
