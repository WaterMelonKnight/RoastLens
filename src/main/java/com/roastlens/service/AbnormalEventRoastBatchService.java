package com.roastlens.service;

import com.roastlens.model.dto.RoastBatchResponse;

public interface AbnormalEventRoastBatchService {
    RoastBatchResponse processAbnormalEvents();
    RoastBatchResponse processAbnormalEvents(String language);
}
