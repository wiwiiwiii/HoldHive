package com.holdhive.portfolio.api.dto;

import com.holdhive.portfolio.application.HoldingList;
import com.holdhive.portfolio.application.HoldingView;

public final class HoldingMapper {

    private HoldingMapper() {
    }

    public static HoldingListResponse toResponse(HoldingList holdingList) {
        return new HoldingListResponse(
            holdingList.items().stream()
                .map(HoldingMapper::toResponse)
                .toList(),
            holdingList.count()
        );
    }

    public static HoldingResponse toResponse(HoldingView holding) {
        return new HoldingResponse(
            holding.id(),
            holding.instrumentId(),
            holding.ticker(),
            holding.exchangeCode(),
            holding.displayName(),
            holding.assetType(),
            holding.provider(),
            holding.providerQuoteId(),
            holding.currency(),
            holding.quantity(),
            holding.averagePurchasePrice(),
            holding.averagePurchasePrice(),
            holding.averagePurchasePrice(),
            holding.currentPrice(),
            holding.currentPrice(),
            holding.marketValue(),
            holding.costBasis(),
            holding.unrealizedGainLoss(),
            holding.unrealizedGainLoss(),
            holding.unrealizedGainLossPercent(),
            holding.unrealizedGainLossPercent(),
            holding.allocationPercent(),
            holding.priceStatus(),
            holding.priceObservedAt(),
            holding.createdAt(),
            holding.updatedAt()
        );
    }
}
