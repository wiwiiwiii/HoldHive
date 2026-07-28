package com.holdhive.portfolio.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.holdhive.portfolio.application.PortfolioExposure;
import com.holdhive.portfolio.application.PortfolioExposureService;
import com.holdhive.pricing.application.PriceMode;

@RestController
@RequestMapping("/api/v1/portfolio")
public class PortfolioExposureController {

    private final PortfolioExposureService portfolioExposureService;

    public PortfolioExposureController(PortfolioExposureService portfolioExposureService) {
        this.portfolioExposureService = portfolioExposureService;
    }

    @GetMapping("/exposure")
    public PortfolioExposure exposure(
        @RequestParam(defaultValue = "false") boolean lookthrough,
        @RequestParam(defaultValue = "BEST_AVAILABLE") PriceMode priceMode
    ) {
        return portfolioExposureService.getExposure(lookthrough, priceMode);
    }
}
