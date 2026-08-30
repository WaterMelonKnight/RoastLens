package com.roastlens.controller;

import com.roastlens.model.dto.ApproveContentRequest;
import com.roastlens.model.dto.ContentItemResponse;
import com.roastlens.model.dto.RejectContentRequest;
import com.roastlens.service.ContentInventoryService;
import com.roastlens.service.ContentCardService;
import com.roastlens.service.ContentReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/content")
public class ContentInventoryController {
    private final ContentInventoryService inventory;
    private final ContentReviewService review;
    private final ContentCardService cards;

    public ContentInventoryController(ContentInventoryService inventory, ContentReviewService review, ContentCardService cards) {
        this.inventory = inventory;
        this.review = review;
        this.cards = cards;
    }

    @GetMapping
    public List<ContentItemResponse> recent(@RequestParam(defaultValue = "20") int limit) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("limit must be between 1 and 100");
        return inventory.recent(limit);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContentItemResponse> byId(@PathVariable String id) {
        return ResponseEntity.of(inventory.findById(id));
    }

    @GetMapping(value = "/{id}/card.svg", produces = "image/svg+xml")
    public ResponseEntity<String> card(@PathVariable String id) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("image/svg+xml")).body(cards.renderSvg(id));
    }

    @PostMapping("/{id}/approve")
    public ContentItemResponse approve(@PathVariable String id, @Valid @RequestBody ApproveContentRequest request) {
        return review.approve(id, request.candidateId(), request.reviewedText());
    }

    @PostMapping("/{id}/reject")
    public ContentItemResponse reject(@PathVariable String id, @Valid @RequestBody(required = false) RejectContentRequest request) {
        return review.reject(id, request == null ? null : request.reason());
    }

    @GetMapping("/by-event/{sourceEventId}")
    public ResponseEntity<ContentItemResponse> byEvent(@PathVariable String sourceEventId) {
        return ResponseEntity.of(inventory.findBySourceEventId(sourceEventId));
    }
}
