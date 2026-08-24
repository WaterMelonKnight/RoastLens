package com.roastlens.controller;

import com.roastlens.model.dto.RoastResponse;
import com.roastlens.model.dto.RoastBatchResponse;
import com.roastlens.service.AbnormalEventRoastBatchService;
import com.roastlens.service.FinStreamRoastService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/roasts/from-finstream")
public class FinStreamRoastController {
    private final FinStreamRoastService roastService;
    private final AbnormalEventRoastBatchService batchService;

    public FinStreamRoastController(FinStreamRoastService roastService,
                                    AbnormalEventRoastBatchService batchService) {
        this.roastService = roastService;
        this.batchService = batchService;
    }

    @PostMapping("/abnormal")
    public RoastBatchResponse createAbnormalBatch(@RequestParam(required = false) String lang) {
        return batchService.processAbnormalEvents(lang);
    }

    @PostMapping("/{eventId}")
    public RoastResponse create(@PathVariable String eventId, @RequestParam(required = false) String lang) {
        return roastService.generateFromFinStream(eventId, lang);
    }
}
