package com.roastlens.content;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "content_items", uniqueConstraints =
        @UniqueConstraint(name = "uk_content_item_source_event", columnNames = "source_event_id"))
public class ContentItem {
    @Id @Column(nullable = false, length = 36)
    private String id;
    @Column(name = "source_event_id", nullable = false, length = 255)
    private String sourceEventId;
    private String source;
    private String symbol;
    private String eventType;
    private Instant eventTime;
    private Instant detectedAt;
    @Column(nullable = false)
    private double roastabilityScore;
    @Column(nullable = false, length = 20)
    private String language;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ContentStatus status;
    @Enumerated(EnumType.STRING) @Column(length = 20)
    private ContentReviewStatus reviewStatus;
    @Column(length = 36)
    private String selectedCandidateId;
    @Column(length = 4000)
    private String reviewedText;
    private Instant reviewedAt;
    @Column(length = 500)
    private String rejectionReason;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "content_item_id", nullable = false)
    private List<ContentCandidate> candidates = new ArrayList<>();

    protected ContentItem() {}

    public ContentItem(String sourceEventId, String source, String symbol, String eventType,
                       Instant eventTime, Instant detectedAt, double roastabilityScore,
                       String language, ContentStatus status, List<ContentCandidate> candidates) {
        this.id = UUID.randomUUID().toString();
        this.sourceEventId = sourceEventId;
        this.source = source;
        this.symbol = symbol;
        this.eventType = eventType;
        this.eventTime = eventTime;
        this.detectedAt = detectedAt;
        this.roastabilityScore = roastabilityScore;
        this.language = language;
        this.status = status;
        this.reviewStatus = status == ContentStatus.GENERATED ? ContentReviewStatus.PENDING : null;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
        if (candidates != null) this.candidates.addAll(candidates);
    }

    public String getId() { return id; }
    public String getSourceEventId() { return sourceEventId; }
    public String getSource() { return source; }
    public String getSymbol() { return symbol; }
    public String getEventType() { return eventType; }
    public Instant getEventTime() { return eventTime; }
    public Instant getDetectedAt() { return detectedAt; }
    public double getRoastabilityScore() { return roastabilityScore; }
    public String getLanguage() { return language; }
    public ContentStatus getStatus() { return status; }
    public ContentReviewStatus getReviewStatus() {
        // Rows written before review support have null here; generated legacy rows await review.
        return reviewStatus == null && status == ContentStatus.GENERATED ? ContentReviewStatus.PENDING : reviewStatus;
    }
    public String getSelectedCandidateId() { return selectedCandidateId; }
    public String getReviewedText() { return reviewedText; }
    public Instant getReviewedAt() { return reviewedAt; }
    public String getRejectionReason() { return rejectionReason; }

    public void approve(String candidateId, String finalText, Instant reviewedAt) {
        this.reviewStatus = ContentReviewStatus.APPROVED;
        this.selectedCandidateId = candidateId;
        this.reviewedText = finalText;
        this.reviewedAt = reviewedAt;
        this.rejectionReason = null;
        this.updatedAt = reviewedAt;
    }

    public void reject(String reason, Instant reviewedAt) {
        this.reviewStatus = ContentReviewStatus.REJECTED;
        this.selectedCandidateId = null;
        this.reviewedText = null;
        this.reviewedAt = reviewedAt;
        this.rejectionReason = reason;
        this.updatedAt = reviewedAt;
    }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<ContentCandidate> getCandidates() { return List.copyOf(candidates); }
}
