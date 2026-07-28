package com.holdhive.portfolio.api.dto;

import java.util.List;

public record HoldingListResponse(
    List<HoldingResponse> items,
    int count
) {
}
