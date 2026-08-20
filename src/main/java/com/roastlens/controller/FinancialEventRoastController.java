package com.roastlens.controller;

import com.roastlens.financial.FinancialEventInput;
import com.roastlens.model.dto.RoastResponse;
import com.roastlens.service.FinancialEventRoastService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/roasts")
public class FinancialEventRoastController {

    private final FinancialEventRoastService roastService;

    public FinancialEventRoastController(FinancialEventRoastService roastService) {
        this.roastService = roastService;
    }

    @PostMapping
    public RoastResponse create(@Valid @RequestBody FinancialEventInput event) {
        return roastService.generateCandidates(event);
    }
}
