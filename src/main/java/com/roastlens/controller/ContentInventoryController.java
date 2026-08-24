package com.roastlens.controller;

import com.roastlens.model.dto.ContentItemResponse;
import com.roastlens.service.ContentInventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/content")
public class ContentInventoryController {
    private final ContentInventoryService inventory;

    public ContentInventoryController(ContentInventoryService inventory) { this.inventory = inventory; }

    @GetMapping
    public List<ContentItemResponse> recent(@RequestParam(defaultValue = "20") int limit) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("limit must be between 1 and 100");
        return inventory.recent(limit);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContentItemResponse> byId(@PathVariable String id) {
        return ResponseEntity.of(inventory.findById(id));
    }

    @GetMapping("/by-event/{sourceEventId}")
    public ResponseEntity<ContentItemResponse> byEvent(@PathVariable String sourceEventId) {
        return ResponseEntity.of(inventory.findBySourceEventId(sourceEventId));
    }
}
