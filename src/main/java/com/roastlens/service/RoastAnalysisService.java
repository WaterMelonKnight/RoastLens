package com.roastlens.service;

import com.roastlens.model.dto.AnalyzeRequest;
import com.roastlens.model.dto.AnalyzeResponse;

public interface RoastAnalysisService {

    AnalyzeResponse analyze(AnalyzeRequest request);
}
