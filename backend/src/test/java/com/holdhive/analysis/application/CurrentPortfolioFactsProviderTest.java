package com.holdhive.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.holdhive.portfolio.application.HoldingList;
import com.holdhive.portfolio.application.HoldingQueryService;
import com.holdhive.portfolio.application.HoldingView;
import com.holdhive.portfolio.application.PortfolioHoldingReader;
import com.holdhive.portfolio.application.PortfolioSnapshot;
import com.holdhive.portfolio.domain.AssetType;
import com.holdhive.pricing.application.PriceMode;
import com.holdhive.pricing.domain.PriceStatus;

@ExtendWith(MockitoExtension.class)
class CurrentPortfolioFactsProviderTest {

    @Mock
    private HoldingQueryService holdingQueryService;

    @Mock
    private PortfolioHoldingReader portfolioHoldingReader;

    private CurrentPortfolioFactsProvider provider;

    @BeforeEach
    void setUp() {
        provider = new CurrentPortfolioFactsProvider(holdingQueryService, portfolioHoldingReader);
    }

    @Test
    void readsHoldingsUsingRequestedPriceMode() {
        when(portfolioHoldingReader.findDefaultPortfolio()).thenReturn(new PortfolioSnapshot(
            1L,
            "Demo Portfolio",
            "USD",
            List.of()
        ));
        when(holdingQueryService.listHoldings(null, PriceMode.LIVE_ONLY)).thenReturn(new HoldingList(List.of(
            new HoldingView(
                101L,
                201L,
                "AAPL",
                "NASDAQ",
                "Apple Inc.",
                AssetType.STOCK,
                "EASTMONEY",
                "105.AAPL",
                "USD",
                new BigDecimal("2.00000000"),
                new BigDecimal("100.00000000"),
                new BigDecimal("150.00000000"),
                new BigDecimal("300.00000000"),
                new BigDecimal("200.00000000"),
                new BigDecimal("100.00000000"),
                new BigDecimal("50.00000000"),
                new BigDecimal("100.00000000"),
                PriceStatus.LIVE,
                null,
                null,
                null
            )
        ), 1));

        CurrentPortfolioFactsProvider.CurrentPortfolioHoldings result = provider.currentHoldings(PriceMode.LIVE_ONLY);

        assertThat(result.baseCurrency()).isEqualTo("USD");
        assertThat(result.holdings()).singleElement()
            .satisfies(holding -> {
                assertThat(holding.ticker()).isEqualTo("AAPL");
                assertThat(holding.marketValue()).isEqualByComparingTo("300.00000000");
            });
        verify(holdingQueryService).listHoldings(null, PriceMode.LIVE_ONLY);
    }
}
