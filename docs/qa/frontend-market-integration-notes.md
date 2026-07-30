# Frontend Market and Exposure Integration Notes

Use this as a regression reference for the implemented market, exposure, and analysis integration in `1.0.0`.

Implementation status:

- Add Holding uses market search results and preserves `providerQuoteId`, `assetType`, `exchangeCode`, and `displayName`.
- Dashboard and Analysis call `/portfolio/exposure?lookthrough=true`.
- Settings data mode (`BEST_AVAILABLE`, `LIVE_ONLY`, `DEMO_ALLOWED`) is propagated to summary, holdings, performance, exposure, and analysis requests.
- Analysis uses `/portfolio/analysis/insights/full` and can auto-refresh after holdings change or refresh manually.

## New/Updated Backend Contracts

### Market search

```http
GET /api/v1/market/search?query=AAPL&market=US
```

- Use this for the add-holding search box.
- Debounce user input in the UI.
- Display `ticker`, `displayName`, `exchangeCode`, and `assetType`.
- Submit `providerQuoteId` from the selected result when creating holdings.

### Market quotes

```http
GET /api/v1/market/quotes?providerQuoteIds=105.AAPL,CRYPTO:BTC&priceMode=BEST_AVAILABLE
```

- `BEST_AVAILABLE` now means live external quote, fresh DB cache, then unavailable.
- Use `DEMO_ALLOWED` only for deterministic classroom demo data.
- If a quote appears in `unavailable`, show a non-blocking message and keep the holding editable.

### Fund lookthrough

```http
GET /api/v1/funds/{instrumentId}/lookthrough
```

- Call when a user selects or opens an `ETF` / `MUTUAL_FUND`.
- Use `HoldingResponse.instrumentId` from `/api/v1/holdings`, not `HoldingResponse.id`, when opening lookthrough for an existing holding.
- Show a hint that fund holdings may overlap with direct stock holdings.
- If `holdings` is empty and warnings exist, display the warning instead of treating it as a hard error.

### Portfolio exposure

```http
GET /api/v1/portfolio/exposure?lookthrough=true&priceMode=DEMO_ALLOWED
```

- Use this for an exposure chart/table rather than recalculating fund weights in React.
- `directMarketValue` is exposure from direct holdings.
- `fundLookthroughMarketValue` is exposure derived from fund disclosures.
- `sources` explains whether the row comes from `DIRECT`, `FUND:{ticker}`, or an undisclosed residual.
- Show `warnings` near the chart or as callouts.

## Frontend Regression Notes

1. Search and add a US stock, a crypto asset, an ETF/mutual fund, cash, and bank deposit.
2. Confirm `priceStatus` labels are visible: `LIVE`, `CACHED`, `DEMO`, `FIXED`, `UNAVAILABLE`.
3. Confirm fund lookthrough uses `HoldingResponse.instrumentId`, not `HoldingResponse.id`.
4. Confirm fund exposure warnings display as callouts, not errors.
5. Confirm Dashboard exposure table stays within the page width and can scroll horizontally if needed.
6. Confirm Analysis table text is readable and consistent with Holdings table sizing.
7. Confirm unavailable market/fund data degrades the affected card only, not the whole app.

## UX Requirements

- Do not show raw JSON errors to users.
- Use neutral copy for unavailable quotes: “Price is unavailable; saved holding is still editable.”
- Use clear labels for demo data: “Demo price”.
- Use clear labels for cached data: “Cached price”.
- Do not present fund lookthrough or AI-style analysis as investment advice.

## AI Analysis Integration

Recommended frontend endpoint:

```http
GET /api/v1/portfolio/analysis/insights/full?priceMode=BEST_AVAILABLE
Accept: text/event-stream
```

SSE events:

- `facts`: deterministic structured analysis facts; render cards immediately.
- `token`: English Markdown insight chunks; append to the AI Insights card.
- `done`: close the stream.

If the stream fails after `facts`, keep the structured analysis visible and show a concise AI fallback message.
