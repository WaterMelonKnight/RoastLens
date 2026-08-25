package com.roastlens.model.dto;

import jakarta.validation.constraints.Size;

public record RejectContentRequest(
        @Size(max = 500, message = "reason must be at most 500 characters") String reason) {}
