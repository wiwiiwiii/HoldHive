package com.holdhive.portfolio.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.holdhive.portfolio.api.dto.PortfolioSummaryResponse;
import com.holdhive.portfolio.application.PortfolioSummaryService;
import com.holdhive.pricing.application.PriceMode;

@RestController
@RequestMapping("/api/v1/portfolio")
public class PortfolioSummaryController {

    private final PortfolioSummaryService portfolioSummaryService;

    public PortfolioSummaryController(PortfolioSummaryService portfolioSummaryService) {
        this.portfolioSummaryService = portfolioSummaryService;
    }

    @GetMapping("/summary")
    public PortfolioSummaryResponse summary(
        @RequestParam(defaultValue = "BEST_AVAILABLE") PriceMode priceMode
    ) {
        return PortfolioSummaryResponse.from(portfolioSummaryService.getSummary(priceMode));
    }
}
