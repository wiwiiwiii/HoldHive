package com.holdhive.analysis.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.holdhive.analysis.api.dto.AnalyzePortfolioRequest;
import com.holdhive.analysis.api.dto.PortfolioAnalysisResponse;
import com.holdhive.analysis.application.PortfolioAnalysisService;

import jakarta.validation.Valid;

/**
 * Standalone demo endpoint for layered portfolio analysis. Not wired into the
 * main backend on purpose - see README.md for the migration plan.
 */
@RestController
@RequestMapping("/api/v1/portfolio")
public class AnalysisController {

    private final PortfolioAnalysisService portfolioAnalysisService;

    public AnalysisController(PortfolioAnalysisService portfolioAnalysisService) {
        this.portfolioAnalysisService = portfolioAnalysisService;
    }

    @PostMapping("/analysis")
    public ResponseEntity<PortfolioAnalysisResponse> analyze(@Valid @RequestBody AnalyzePortfolioRequest request) {
        return ResponseEntity.ok(portfolioAnalysisService.analyze(request));
    }
}
