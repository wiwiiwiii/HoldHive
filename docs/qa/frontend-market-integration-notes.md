# Frontend Market and Exposure Integration Notes

Use this after the backend market-completion PR is merged into `qa`.

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

## Member C Implementation Steps

1. Add `fetchMarketQuotes`, `fetchFundLookthrough`, and `fetchPortfolioExposure` functions in `frontend/src/api/portfolioApi.ts`.
2. Add TypeScript types matching the response fields above in `frontend/src/api/types.ts`.
3. Update Add Holding search so selected results preserve `providerQuoteId`, `assetType`, `exchangeCode`, and `displayName`.
4. Add a fund warning panel when `assetType` is `ETF` or `MUTUAL_FUND`.
5. Add an exposure card/chart using `/portfolio/exposure?lookthrough=true`.
6. Render `priceStatus` labels visibly: `LIVE`, `CACHED`, `DEMO`, `FIXED`, `UNAVAILABLE`.

## UX Requirements

- Do not show raw JSON errors to users.
- Use neutral copy for unavailable quotes: “Price is unavailable; saved holding is still editable.”
- Use clear labels for demo data: “Demo price”.
- Use clear labels for cached data: “Cached price”.
- Do not present fund lookthrough or AI-style analysis as investment advice.
