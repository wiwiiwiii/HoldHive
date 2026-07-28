package com.holdhive.pricing.application;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.holdhive.pricing.domain.MarketQuote;
import com.holdhive.pricing.domain.PriceStatus;

@Service
public class MarketQuoteQueryService {

    private final PricingService pricingService;

    public MarketQuoteQueryService(PricingService pricingService) {
        this.pricingService = Objects.requireNonNull(pricingService, "pricingService must not be null");
    }

    public MarketQuoteResult getQuotes(List<String> providerQuoteIds, PriceMode priceMode) {
        PriceMode resolvedPriceMode = priceMode == null ? PriceMode.BEST_AVAILABLE : priceMode;
        List<String> requestedIds = normalize(providerQuoteIds);
        if (requestedIds.isEmpty()) {
            return new MarketQuoteResult("MIXED", resolvedPriceMode, List.of(), List.of());
        }

        Map<String, MarketQuote> quotesById = pricingService.getQuotes(requestedIds).stream()
            .collect(Collectors.toMap(
                quote -> normalize(quote.providerQuoteId()),
                Function.identity(),
                (first, ignored) -> first
            ));

        List<MarketQuote> acceptedQuotes = new ArrayList<>();
        List<UnavailableQuote> unavailableQuotes = new ArrayList<>();

        for (String requestedId : requestedIds) {
            MarketQuote quote = quotesById.get(normalize(requestedId));
            if (isUnavailable(quote)) {
                unavailableQuotes.add(new UnavailableQuote(requestedId, "PRICE_UNAVAILABLE"));
            } else if (!isAcceptedForMode(quote, resolvedPriceMode)) {
                unavailableQuotes.add(new UnavailableQuote(requestedId, "PRICE_MODE_REJECTED"));
            } else {
                acceptedQuotes.add(quote);
            }
        }

        return new MarketQuoteResult("MIXED", resolvedPriceMode, acceptedQuotes, unavailableQuotes);
    }

    private static boolean isUnavailable(MarketQuote quote) {
        return quote == null
            || quote.currentPrice() == null
            || quote.priceStatus() == PriceStatus.UNAVAILABLE;
    }

    private static boolean isAcceptedForMode(MarketQuote quote, PriceMode priceMode) {
        if (priceMode == PriceMode.LIVE_ONLY) {
            return quote.priceStatus() == PriceStatus.LIVE;
        }
        if (priceMode != PriceMode.DEMO_ALLOWED) {
            return quote.priceStatus() != PriceStatus.DEMO;
        }
        return true;
    }

    private static List<String> normalize(List<String> providerQuoteIds) {
        if (providerQuoteIds == null) {
            return List.of();
        }
        return providerQuoteIds.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(providerQuoteId -> !providerQuoteId.isBlank())
            .collect(Collectors.collectingAndThen(
                Collectors.toCollection(LinkedHashSet::new),
                List::copyOf
            ));
    }

    private static String normalize(String providerQuoteId) {
        return providerQuoteId == null ? "" : providerQuoteId.trim().toUpperCase(Locale.ROOT);
    }
}
