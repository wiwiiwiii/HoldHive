# HoldHive API Test Cases

Manual test requests for `qa` branch validation. Base URL: `http://localhost:8080`.

Only endpoints that currently exist in `backend/src/main/java` are listed below.
Holding CRUD endpoints (create/list/delete a holding) are owned by Member A and are
**not implemented yet** — add their test cases here once the endpoints are merged.

## Health

### GET /api/v1/health

```bash
curl http://localhost:8080/api/v1/health
```

Expected: `200 OK`

```json
{ "status": "UP", "service": "HoldHive API", "timestamp": "..." }
```

## Portfolio Summary

### GET /api/v1/portfolio/summary

```bash
curl "http://localhost:8080/api/v1/portfolio/summary"
curl "http://localhost:8080/api/v1/portfolio/summary?priceMode=DEMO_ALLOWED"
curl "http://localhost:8080/api/v1/portfolio/summary?priceMode=LIVE_ONLY"
```

- `priceMode` is optional, default `BEST_AVAILABLE`. Valid values: `BEST_AVAILABLE`, `LIVE_ONLY`, `DEMO_ALLOWED`.
- Expected `200 OK` with `valuationStatus` one of `EMPTY`, `COMPLETE`, `PARTIAL`, `UNAVAILABLE`.

Cases to check:
- [ ] Empty portfolio -> `valuationStatus: EMPTY`, empty `allocations`
- [ ] Portfolio with all holdings priced -> `valuationStatus: COMPLETE`
- [ ] Portfolio with some holdings unpriced -> `valuationStatus: PARTIAL`, `unpricedHoldings` not empty
- [ ] Invalid `priceMode` value (e.g. `priceMode=NOT_A_MODE`) -> `400 Bad Request`

## Market Quotes

### GET /api/v1/market/quotes

```bash
curl "http://localhost:8080/api/v1/market/quotes?providerQuoteIds=AAPL,MSFT"
curl "http://localhost:8080/api/v1/market/quotes?providerQuoteIds=AAPL&priceMode=DEMO_ALLOWED"
```

- `providerQuoteIds` is required, comma-separated, whitespace around ids is trimmed.
- `priceMode` optional, default `BEST_AVAILABLE`.

Cases to check:
- [ ] Missing `providerQuoteIds` -> `400 Bad Request`
- [ ] Known id -> appears in `quotes`
- [ ] Unknown/unavailable id -> appears in `unavailable`, not in `quotes`
- [ ] Mixed known + unknown ids in one request -> split correctly between `quotes` and `unavailable`

## Fund Lookthrough

### GET /api/v1/funds/{instrumentId}/lookthrough

```bash
curl "http://localhost:8080/api/v1/funds/1/lookthrough"
```

Expected `200 OK`:

```json
{
  "fundInstrumentId": 1,
  "ticker": "...",
  "displayName": "...",
  "assetType": "MUTUAL_FUND",
  "asOfDate": "...",
  "source": "...",
  "coveragePercent": 0,
  "holdings": [],
  "warnings": []
}
```

Cases to check:
- [ ] Valid mutual fund/ETF instrument id -> `200 OK` with `holdings` populated
- [ ] `coveragePercent` < 100 -> `warnings` explains partial coverage
- [ ] Non-existent `instrumentId` -> `404 Not Found` (confirm actual behavior; not yet verified)
- [ ] `instrumentId` for a non-fund asset type (e.g. stock) -> confirm actual behavior (not yet verified)

## Holding CRUD (TODO — blocked on Member A)

- [ ] `POST` create holding — happy path
- [ ] `POST` create holding — invalid `assetType`
- [ ] `POST` create holding — invalid/negative `quantity`
- [ ] `POST` create holding — duplicate holding
- [ ] `GET` list holdings
- [ ] `DELETE` remove holding — happy path
- [ ] `DELETE` remove holding — non-existent id

Fill in real request/response examples once the endpoints and DTOs are merged into `qa`.
