package com.roastlens.controller;

import com.roastlens.content.ContentStatus;
import com.roastlens.content.ContentReviewStatus;
import com.roastlens.model.dto.ContentCandidateResponse;
import com.roastlens.model.dto.ContentItemResponse;
import com.roastlens.service.ContentInventoryService;
import com.roastlens.service.ContentReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContentInventoryController.class)
class ContentInventoryControllerTest {
    @Autowired MockMvc mvc;
    @MockBean ContentInventoryService inventory;
    @MockBean ContentReviewService review;

    @Test void listsRecentContent() throws Exception {
        when(inventory.recent(20)).thenReturn(List.of(item()));
        mvc.perform(get("/api/v1/content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sourceEventId").value("evt-1"))
                .andExpect(jsonPath("$[0].status").value("GENERATED"))
                .andExpect(jsonPath("$[0].reviewStatus").value("PENDING"));
    }

    @Test void detailReturnsCandidates() throws Exception {
        when(inventory.findById("content-1")).thenReturn(Optional.of(item()));
        mvc.perform(get("/api/v1/content/content-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[0].text").value("saved joke"));
    }


    @Test void approvesContent() throws Exception {
        when(review.approve("content-1", "candidate-1", "edited")).thenReturn(approvedItem());
        mvc.perform(post("/api/v1/content/content-1/approve").contentType(APPLICATION_JSON)
                        .content("{\"candidateId\":\"candidate-1\",\"reviewedText\":\"edited\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewStatus").value("APPROVED"))
                .andExpect(jsonPath("$.selectedCandidateId").value("candidate-1"))
                .andExpect(jsonPath("$.reviewedText").value("edited"));
    }

    @Test void rejectsContent() throws Exception {
        when(review.reject("content-1", "weak")).thenReturn(item());
        mvc.perform(post("/api/v1/content/content-1/reject").contentType(APPLICATION_JSON)
                        .content("{\"reason\":\"weak\"}"))
                .andExpect(status().isOk());
    }

    @Test void rejectsBlankCandidateId() throws Exception {
        mvc.perform(post("/api/v1/content/content-1/approve").contentType(APPLICATION_JSON)
                        .content("{\"candidateId\":\" \"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("candidateId must not be blank"));
    }

    private ContentItemResponse approvedItem() {
        return new ContentItemResponse("content-1", "evt-1", "FINSTREAM", "BTCUSDT", "RAPID_DROP",
                null, null, .8, "zh-CN", ContentStatus.GENERATED, ContentReviewStatus.APPROVED,
                "candidate-1", "edited", null, null, null, null,
                List.of(new ContentCandidateResponse("candidate-1", "saved joke", "dry", "low", null)));
    }

    private ContentItemResponse item() {
        return new ContentItemResponse("content-1", "evt-1", "FINSTREAM", "BTCUSDT", "RAPID_DROP",
                null, null, .8, "zh-CN", ContentStatus.GENERATED, null, null,
                List.of(new ContentCandidateResponse("candidate-1", "saved joke", "dry", "low", null)));
    }
}
