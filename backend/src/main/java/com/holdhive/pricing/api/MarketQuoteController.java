package com.holdhive.pricing.api;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.holdhive.pricing.api.dto.MarketQuoteResultResponse;
import com.holdhive.pricing.application.MarketQuoteQueryService;
import com.holdhive.pricing.application.PriceMode;

@RestController
@RequestMapping("/api/v1/market")
public class MarketQuoteController {

    private final MarketQuoteQueryService marketQuoteQueryService;

    public MarketQuoteController(MarketQuoteQueryService marketQuoteQueryService) {
        this.marketQuoteQueryService = marketQuoteQueryService;
    }

    @GetMapping("/quotes")
    public MarketQuoteResultResponse quotes(
        @RequestParam String providerQuoteIds,
        @RequestParam(defaultValue = "BEST_AVAILABLE") PriceMode priceMode
    ) {
        List<String> requestedIds = Arrays.stream(providerQuoteIds.split(","))
            .map(String::trim)
            .filter(providerQuoteId -> !providerQuoteId.isBlank())
            .toList();
        return MarketQuoteResultResponse.from(marketQuoteQueryService.getQuotes(requestedIds, priceMode));
    }
}
