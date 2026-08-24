package com.roastlens.controller;

import com.roastlens.content.ContentStatus;
import com.roastlens.model.dto.ContentCandidateResponse;
import com.roastlens.model.dto.ContentItemResponse;
import com.roastlens.service.ContentInventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContentInventoryController.class)
class ContentInventoryControllerTest {
    @Autowired MockMvc mvc;
    @MockBean ContentInventoryService inventory;

    @Test void listsRecentContent() throws Exception {
        when(inventory.recent(20)).thenReturn(List.of(item()));
        mvc.perform(get("/api/v1/content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sourceEventId").value("evt-1"))
                .andExpect(jsonPath("$[0].status").value("GENERATED"));
    }

    @Test void detailReturnsCandidates() throws Exception {
        when(inventory.findById("content-1")).thenReturn(Optional.of(item()));
        mvc.perform(get("/api/v1/content/content-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[0].text").value("saved joke"));
    }

    private ContentItemResponse item() {
        return new ContentItemResponse("content-1", "evt-1", "FINSTREAM", "BTCUSDT", "RAPID_DROP",
                null, null, .8, "zh-CN", ContentStatus.GENERATED, null, null,
                List.of(new ContentCandidateResponse("candidate-1", "saved joke", "dry", "low", null)));
    }
}
