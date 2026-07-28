package com.holdhive.portfolio.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.holdhive.portfolio.domain.AssetType;
import com.holdhive.pricing.application.PriceMode;
import com.holdhive.pricing.application.PricingService;
import com.holdhive.pricing.domain.MarketQuote;
import com.holdhive.pricing.domain.PriceStatus;

@ExtendWith(MockitoExtension.class)
class PortfolioExposureServiceTest {

    private static final Instant OBSERVED_AT = Instant.parse("2026-07-29T00:00:00Z");

    @Mock
    private PortfolioHoldingReader holdingReader;

    @Mock
    private PricingService pricingService;

    @Mock
    private FundLookthroughService fundLookthroughService;

    private PortfolioExposureService service;

    @BeforeEach
    void setUp() {
        service = new PortfolioExposureService(holdingReader, pricingService, fundLookthroughService);
    }

    @Test
    void combinesDirectHoldingsWithFundLookthroughExposure() {
        when(holdingReader.findDefaultPortfolio()).thenReturn(new PortfolioSnapshot(
            1L,
            "My Portfolio",
            "USD",
            List.of(
                new HoldingPosition(101L, 201L, AssetType.STOCK, "AAPL", "105.AAPL", new BigDecimal("10"), new BigDecimal("100")),
                new HoldingPosition(102L, 202L, AssetType.ETF, "VOO", "105.VOO", new BigDecimal("2"), new BigDecimal("400"))
            )
        ));
        when(pricingService.getQuotes(List.of("105.AAPL", "105.VOO"))).thenReturn(List.of(
            quote("105.AAPL", "AAPL", "150.00"),
            quote("105.VOO", "VOO", "500.00")
        ));
        when(fundLookthroughService.getLookthrough(202L, "VOO", AssetType.ETF)).thenReturn(new FundLookthrough(
            202L,
            "VOO",
            "Vanguard S&P 500 ETF",
            AssetType.ETF,
            java.time.LocalDate.parse("2026-06-30"),
            "DEMO_DISCLOSURE",
            new BigDecimal("50.00000000"),
            List.of(new FundComponent("AAPL", "Apple Inc.", AssetType.STOCK, new BigDecimal("10.00000000"))),
            List.of("partial disclosure")
        ));

        PortfolioExposure exposure = service.getExposure(true, PriceMode.BEST_AVAILABLE);

        assertThat(exposure.totalMarketValue()).isEqualByComparingTo("2500.00000000");
        assertThat(exposure.items()).extracting(PortfolioExposureItem::ticker)
            .contains("AAPL", "VOO_UNDISCLOSED");
        PortfolioExposureItem apple = exposure.items().stream()
            .filter(item -> item.ticker().equals("AAPL"))
            .findFirst()
            .orElseThrow();
        assertThat(apple.directMarketValue()).isEqualByComparingTo("1500.00000000");
        assertThat(apple.fundLookthroughMarketValue()).isEqualByComparingTo("100.00000000");
        assertThat(apple.totalExposureValue()).isEqualByComparingTo("1600.00000000");
        assertThat(apple.exposurePercent()).isEqualByComparingTo("64.00000000");
        assertThat(exposure.warnings()).anySatisfy(warning -> assertThat(warning).contains("AAPL", "direct", "fund"));
    }

    private static MarketQuote quote(String providerQuoteId, String ticker, String price) {
        return new MarketQuote(
            "EASTMONEY",
            providerQuoteId,
            ticker,
            ticker,
            "USD",
            new BigDecimal(price),
            PriceStatus.LIVE,
            OBSERVED_AT
        );
    }
}
