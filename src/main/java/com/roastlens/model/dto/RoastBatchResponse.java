package com.roastlens.model.dto;

import java.util.List;

public record RoastBatchResponse(int processed, int generated, int skipped, int errors,
                                 List<RoastBatchItem> results) {
    public RoastBatchResponse {
        results = results == null ? List.of() : List.copyOf(results);
    }
}
