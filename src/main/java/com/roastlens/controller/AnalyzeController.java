package com.roastlens.controller;

import com.roastlens.domain.DomainRegistry;
import com.roastlens.model.dto.AnalyzeMetaResponse;
import com.roastlens.model.dto.AnalyzeRequest;
import com.roastlens.model.dto.AnalyzeResponse;
import com.roastlens.persona.PersonaRegistry;
import com.roastlens.service.RoastAnalysisService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AnalyzeController {

    private final RoastAnalysisService roastAnalysisService;
    private final DomainRegistry domainRegistry;
    private final PersonaRegistry personaRegistry;

    public AnalyzeController(RoastAnalysisService roastAnalysisService,
                             DomainRegistry domainRegistry,
                             PersonaRegistry personaRegistry) {
        this.roastAnalysisService = roastAnalysisService;
        this.domainRegistry = domainRegistry;
        this.personaRegistry = personaRegistry;
    }

    @GetMapping("/meta")
    public AnalyzeMetaResponse meta() {
        return new AnalyzeMetaResponse(domainRegistry.names(), personaRegistry.names());
    }

    @PostMapping("/analyze")
    public AnalyzeResponse analyze(@Valid @RequestBody AnalyzeRequest request) {
        return roastAnalysisService.analyze(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleServerError(IllegalStateException ex) {
        return ResponseEntity.internalServerError().body(Map.of("error", ex.getMessage()));
    }
}
