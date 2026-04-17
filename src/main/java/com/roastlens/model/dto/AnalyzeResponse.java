package com.roastlens.model.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AnalyzeResponse {

    private String summary;
    private List<String> evidencePoints = new ArrayList<>();
    private String counterPoint;
    private String confidenceNote;
    private String disclaimer;
    private Map<String, Object> styleMeta;

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getEvidencePoints() {
        return evidencePoints;
    }

    public void setEvidencePoints(List<String> evidencePoints) {
        this.evidencePoints = evidencePoints;
    }

    public String getCounterPoint() {
        return counterPoint;
    }

    public void setCounterPoint(String counterPoint) {
        this.counterPoint = counterPoint;
    }

    public String getConfidenceNote() {
        return confidenceNote;
    }

    public void setConfidenceNote(String confidenceNote) {
        this.confidenceNote = confidenceNote;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    public Map<String, Object> getStyleMeta() {
        return styleMeta;
    }

    public void setStyleMeta(Map<String, Object> styleMeta) {
        this.styleMeta = styleMeta;
    }
}
