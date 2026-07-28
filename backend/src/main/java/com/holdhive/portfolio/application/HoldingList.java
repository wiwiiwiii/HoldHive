package com.holdhive.portfolio.application;

import java.util.List;

public record HoldingList(
    List<HoldingView> items,
    int count
) {

    public HoldingList {
        items = List.copyOf(items);
    }
}
