package com.holdhive.portfolio.domain;

public record UnpricedHolding(
    Long holdingId,
    String ticker,
    String reason
) {
}
