package com.roastlens.service;

import com.roastlens.model.dto.RoastResponse;

public interface FinStreamRoastService {
    RoastResponse generateFromFinStream(String eventId);
    RoastResponse generateFromFinStream(String eventId, String language);
}
