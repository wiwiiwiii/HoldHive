package com.holdhive.analysis.application;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.holdhive.analysis.domain.model.HoldingFact;
import com.holdhive.portfolio.application.HoldingQueryService;
import com.holdhive.portfolio.application.PortfolioHoldingReader;
import com.holdhive.pricing.application.PriceMode;

/**
 * Builds the {@link HoldingFact} list the analysis calculators need directly
 * from the current default portfolio, instead of trusting a caller-supplied
 * request body. Market values and cost basis come from
 * {@link HoldingQueryService} (the same pricing pipeline backing
 * {@code GET /api/v1/holdings}), so a client can never inject an arbitrary
 * market value into the analysis - only holdings actually priced by the
 * server are considered.
 */
@Component
public class CurrentPortfolioFactsProvider {

    private final HoldingQueryService holdingQueryService;
    private final PortfolioHoldingReader portfolioHoldingReader;

    public CurrentPortfolioFactsProvider(
            HoldingQueryService holdingQueryService,
            PortfolioHoldingReader portfolioHoldingReader) {
        this.holdingQueryService = Objects.requireNonNull(holdingQueryService, "holdingQueryService must not be null");
        this.portfolioHoldingReader = Objects.requireNonNull(portfolioHoldingReader, "portfolioHoldingReader must not be null");
    }

    /**
     * @return the current default portfolio's base currency plus the facts for
     *         every priced holding (unpriced holdings are excluded - a null
     *         market value cannot feed the L0-L4 calculators).
     */
    public CurrentPortfolioHoldings currentHoldings() {
        String baseCurrency = portfolioHoldingReader.findDefaultPortfolio().baseCurrency();
        List<HoldingFact> facts = holdingQueryService.listHoldings(null, PriceMode.BEST_AVAILABLE).items().stream()
                .filter(holding -> holding.marketValue() != null)
                .map(holding -> new HoldingFact(
                        holding.ticker(),
                        AssetTypeMapper.toAnalysisAssetType(holding.assetType()),
                        holding.quantity(),
                        holding.marketValue(),
                        holding.costBasis()))
                .toList();
        return new CurrentPortfolioHoldings(baseCurrency, facts);
    }

    public record CurrentPortfolioHoldings(String baseCurrency, List<HoldingFact> holdings) {
    }
}
