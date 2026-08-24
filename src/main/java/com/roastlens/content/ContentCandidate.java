package com.roastlens.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "content_candidates")
public class ContentCandidate {
    @Id
    @Column(nullable = false, length = 36)
    private String id;
    @Column(nullable = false, length = 4000)
    private String text;
    @Column(nullable = false, length = 100)
    private String style;
    @Column(nullable = false, length = 50)
    private String riskLevel;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected ContentCandidate() {}

    public ContentCandidate(String text, String style, String riskLevel) {
        this.id = UUID.randomUUID().toString();
        this.text = text;
        this.style = style;
        this.riskLevel = riskLevel;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getText() { return text; }
    public String getStyle() { return style; }
    public String getRiskLevel() { return riskLevel; }
    public Instant getCreatedAt() { return createdAt; }
}
