package com.roastlens.model.dto;

import java.util.List;

public class RoastResponse {

    private final String eventId;
    private final List<RoastCandidate> candidates;

    public RoastResponse(String eventId, List<RoastCandidate> candidates) {
        this.eventId = eventId;
        this.candidates = candidates;
    }

    public String getEventId() { return eventId; }
    public List<RoastCandidate> getCandidates() { return candidates; }
}
