package com.roastlens.controller;

import com.roastlens.model.dto.RoastResponse;
import com.roastlens.service.FinStreamRoastService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/roasts/from-finstream")
public class FinStreamRoastController {
    private final FinStreamRoastService roastService;

    public FinStreamRoastController(FinStreamRoastService roastService) {
        this.roastService = roastService;
    }

    @PostMapping("/{eventId}")
    public RoastResponse create(@PathVariable String eventId) {
        return roastService.generateFromFinStream(eventId);
    }
}
