package com.holdhive.portfolio.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.holdhive.portfolio.domain.PortfolioCalculator;
import com.holdhive.portfolio.domain.ValuationStatus;
import com.holdhive.pricing.application.PriceMode;
import com.holdhive.pricing.application.PricingService;
import com.holdhive.pricing.domain.MarketQuote;
import com.holdhive.pricing.domain.PriceStatus;

@ExtendWith(MockitoExtension.class)
class PortfolioSummaryServiceTest {

    private static final Instant OBSERVED_AT = Instant.parse("2026-07-24T08:29:00Z");

    @Mock
    private PortfolioHoldingReader holdingReader;

    @Mock
    private PricingService pricingService;

    private PortfolioSummaryService service;

    @BeforeEach
    void setUp() {
        service = new PortfolioSummaryService(holdingReader, pricingService, new PortfolioCalculator());
    }

    @Test
    void combinesHoldingsWithMarketQuotesIntoPortfolioSummary() {
        when(holdingReader.findDefaultPortfolio()).thenReturn(new PortfolioSnapshot(
            1L,
            "My Portfolio",
            "USD",
            List.of(
                new HoldingPosition(101L, "AAPL", "105.AAPL", new BigDecimal("10"), new BigDecimal("175.50")),
                new HoldingPosition(102L, "MSFT", "105.MSFT", new BigDecimal("5"), new BigDecimal("300.00")),
                new HoldingPosition(103L, "UNKNOWN", "UNKNOWN", new BigDecimal("2"), new BigDecimal("500.00"))
            )
        ));
        when(pricingService.getQuotes(List.of("105.AAPL", "105.MSFT", "UNKNOWN"))).thenReturn(List.of(
            new MarketQuote(
                "EASTMONEY",
                "105.AAPL",
                "AAPL",
                "Apple Inc.",
                "USD",
                new BigDecimal("210.25"),
                PriceStatus.LIVE,
                OBSERVED_AT
            ),
            new MarketQuote(
                "EASTMONEY",
                "105.MSFT",
                "MSFT",
                "Microsoft",
                "USD",
                new BigDecimal("330.00"),
                PriceStatus.CACHED,
                OBSERVED_AT
            )
        ));

        PortfolioSummary summary = service.getSummary(PriceMode.BEST_AVAILABLE);

        assertThat(summary.portfolioId()).isEqualTo(1L);
        assertThat(summary.portfolioName()).isEqualTo("My Portfolio");
        assertThat(summary.baseCurrency()).isEqualTo("USD");
        assertThat(summary.valuationStatus()).isEqualTo(ValuationStatus.PARTIAL);
        assertThat(summary.holdingCount()).isEqualTo(3);
        assertThat(summary.pricedHoldingCount()).isEqualTo(2);
        assertThat(summary.totalCostBasis()).isEqualByComparingTo("4255.00000000");
        assertThat(summary.totalMarketValue()).isEqualByComparingTo("3752.50000000");
        assertThat(summary.totalUnrealizedGainLoss()).isEqualByComparingTo("497.50000000");
        assertThat(summary.totalUnrealizedGainLossPercent()).isEqualByComparingTo("11.69212691");
        assertThat(summary.priceAsOf()).isEqualTo(OBSERVED_AT);
        assertThat(summary.allocations()).hasSize(2);
        assertThat(summary.unpricedHoldings()).singleElement()
            .satisfies(unpriced -> {
                assertThat(unpriced.holdingId()).isEqualTo(103L);
                assertThat(unpriced.ticker()).isEqualTo("UNKNOWN");
                assertThat(unpriced.reason()).isEqualTo("PRICE_UNAVAILABLE");
            });
        verify(pricingService).getQuotes(List.of("105.AAPL", "105.MSFT", "UNKNOWN"));
    }
}
