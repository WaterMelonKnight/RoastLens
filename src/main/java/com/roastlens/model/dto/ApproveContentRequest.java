package com.roastlens.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApproveContentRequest(
        @NotBlank(message = "candidateId must not be blank") String candidateId,
        @Size(max = 4000, message = "reviewedText must be at most 4000 characters") String reviewedText) {}
