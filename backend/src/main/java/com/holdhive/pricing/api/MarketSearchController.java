package com.holdhive.pricing.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.holdhive.pricing.api.dto.MarketSearchResultResponse;
import com.holdhive.pricing.application.MarketSearchQueryService;

@RestController
@RequestMapping("/api/v1/market")
public class MarketSearchController {

    private final MarketSearchQueryService marketSearchQueryService;

    public MarketSearchController(MarketSearchQueryService marketSearchQueryService) {
        this.marketSearchQueryService = marketSearchQueryService;
    }

    @GetMapping("/search")
    public MarketSearchResultResponse search(
        @RequestParam String query,
        @RequestParam(required = false) String market
    ) {
        return MarketSearchResultResponse.from(marketSearchQueryService.search(query, market));
    }
}
