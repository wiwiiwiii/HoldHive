# HoldHive API Test Cases

Manual test requests for `qa` branch validation. Base URL: `http://localhost:8080`.

Only endpoints that currently exist in `backend/src/main/java` are listed below.

For a no-MySQL local smoke test, the backend can be started with the H2 test profile:

```bash
cd backend
BACKEND_PORT=8080 ./mvnw spring-boot:run \
  -Dspring-boot.run.profiles=test \
  -Dspring-boot.run.useTestClasspath=true
```

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

## Market Search

### GET /api/v1/market/search

```bash
curl "http://localhost:8080/api/v1/market/search?query=AAPL"
curl "http://localhost:8080/api/v1/market/search?query=apple&market=US"
curl "http://localhost:8080/api/v1/market/search?query=600&market=SH"
```

- `query` is required and matched against ticker, display name, or provider quote id.
- `market` is optional. Current demo values include `US`, `CN`, `SH`, `SZ`, `NASDAQ`, `NYSE`, `FUND`, `CRYPTO`, `CASH`, and `BANK`.
- Current implementation returns demo search data with `source: DEMO`; real provider integration can replace the catalog behind the same response contract.

Expected `200 OK`:

```json
{
  "query": "AAPL",
  "results": [
    {
      "ticker": "AAPL",
      "displayName": "Apple Inc.",
      "exchangeCode": "NASDAQ",
      "provider": "EASTMONEY",
      "providerQuoteId": "105.AAPL",
      "assetType": "STOCK"
    }
  ],
  "source": "DEMO",
  "cached": false
}
```

Cases to check:
- [ ] Missing `query` -> `400 Bad Request`, `code: VALIDATION_FAILED`
- [ ] Known ticker/name/query -> appears in `results`
- [ ] `market=US` -> only US exchange results, such as `NASDAQ` or `NYSE`
- [ ] `market=SH` -> Shanghai demo results, such as `600519`
- [ ] Unknown query -> `200 OK`, empty `results`

## Market Quotes

### GET /api/v1/market/quotes

```bash
curl "http://localhost:8080/api/v1/market/quotes?providerQuoteIds=AAPL,MSFT"
curl "http://localhost:8080/api/v1/market/quotes?providerQuoteIds=105.MSFT&priceMode=DEMO_ALLOWED"
curl "http://localhost:8080/api/v1/market/quotes?providerQuoteIds=AAPL,105.MSFT,UNKNOWN&priceMode=DEMO_ALLOWED"
```

- `providerQuoteIds` is required, comma-separated, whitespace around ids is trimmed.
- `priceMode` optional, default `BEST_AVAILABLE`. In the current demo adapter, use `DEMO_ALLOWED` when checking demo quote ids.

Cases to check:
- [ ] Missing `providerQuoteIds` -> `400 Bad Request`, `code: VALIDATION_FAILED`
- [ ] Known demo id with `DEMO_ALLOWED` -> appears in `quotes`
- [ ] Unknown/unavailable id -> appears in `unavailable`, not in `quotes`
- [ ] Mixed known + unknown ids in one request -> split correctly between `quotes` and `unavailable`

## Fund Lookthrough

### GET /api/v1/funds/{instrumentId}/lookthrough

```bash
curl "http://localhost:8080/api/v1/funds/102/lookthrough"
```

Expected `200 OK`:

```json
{
  "fundInstrumentId": 102,
  "ticker": "VOO",
  "displayName": "Vanguard S&P 500 ETF",
  "assetType": "ETF",
  "asOfDate": "2026-06-30",
  "source": "DEMO_DISCLOSURE",
  "coveragePercent": 41.15,
  "holdings": [
    {
      "ticker": "AAPL",
      "displayName": "Apple Inc.",
      "assetType": "STOCK",
      "weightPercent": 7.12
    }
  ],
  "warnings": [
    "Fund holdings are based on the latest available disclosure and may lag current positions."
  ]
}
```

Cases to check:
- [ ] Valid ETF id `102` -> `200 OK` with `holdings` populated
- [ ] Valid mutual fund id `103` -> `200 OK` with `holdings` populated
- [ ] `coveragePercent` < 100 -> `warnings` explains partial coverage
- [ ] Non-existent `instrumentId` -> `404 Not Found`, `code: FUND_LOOKTHROUGH_NOT_FOUND`

## Holding CRUD

### GET /api/v1/holdings

```bash
curl "http://localhost:8080/api/v1/holdings"
curl "http://localhost:8080/api/v1/holdings?sort=marketValue,desc&priceMode=DEMO_ALLOWED"
```

Expected: `200 OK`

```json
{ "items": [], "count": 0 }
```

### GET /api/v1/holdings/{holdingId}

```bash
curl "http://localhost:8080/api/v1/holdings/1?priceMode=DEMO_ALLOWED"
```

- Existing holding -> `200 OK`, response body is a `Holding`.
- Missing holding -> `404 Not Found`, `code: HOLDING_NOT_FOUND`.

### POST /api/v1/holdings

```bash
curl -i -X POST "http://localhost:8080/api/v1/holdings" \
  -H "Content-Type: application/json" \
  -d '{
    "assetType": "STOCK",
    "ticker": " msft ",
    "exchangeCode": "nasdaq",
    "providerQuoteId": "105.MSFT",
    "quantity": 5,
    "averagePurchasePrice": 300
  }'
```

Expected: `201 Created`, `Location: /api/v1/holdings/{id}`.

The create response uses default `BEST_AVAILABLE`; demo-only prices can therefore be `UNAVAILABLE` in the immediate response. Re-query with `priceMode=DEMO_ALLOWED` to verify demo valuation:

```bash
curl "http://localhost:8080/api/v1/holdings/{id}?priceMode=DEMO_ALLOWED"
```

Expected fields for the example above:

```json
{
  "ticker": "MSFT",
  "exchangeCode": "NASDAQ",
  "assetType": "STOCK",
  "provider": "EASTMONEY",
  "providerQuoteId": "105.MSFT",
  "quantity": 5,
  "averagePurchasePrice": 300,
  "currentPrice": 330,
  "priceStatus": "DEMO"
}
```

Fixed-value assets:

```bash
curl -i -X POST "http://localhost:8080/api/v1/holdings" \
  -H "Content-Type: application/json" \
  -d '{"assetType":"CASH","ticker":"usd","quantity":4500,"averagePurchasePrice":77}'

curl -i -X POST "http://localhost:8080/api/v1/holdings" \
  -H "Content-Type: application/json" \
  -d '{"assetType":"BANK_DEPOSIT","ticker":"usd_deposit","quantity":3000,"averagePurchasePrice":99}'
```

Expected:

- `CASH` defaults `exchangeCode` to `CASH`.
- `BANK_DEPOSIT` defaults `exchangeCode` to `BANK`.
- Both return `currentPrice: 1`, `averagePurchasePrice: 1`, `priceStatus: FIXED`.

Validation cases:

- [ ] Invalid `assetType` -> `400 Bad Request`, `code: VALIDATION_FAILED`, field `assetType`
- [ ] `quantity` missing, zero, or negative -> `400 Bad Request`, field `quantity`
- [ ] `averagePurchasePrice` negative -> `400 Bad Request`, field `averagePurchasePrice`
- [ ] Duplicate `assetType + ticker + exchangeCode` in the default portfolio -> `409 Conflict`, `code: HOLDING_ALREADY_EXISTS`

### DELETE /api/v1/holdings/{holdingId}

```bash
curl -i -X DELETE "http://localhost:8080/api/v1/holdings/1"
```

- Existing holding -> `204 No Content`
- Deleted or missing holding -> `404 Not Found`, `code: HOLDING_NOT_FOUND`
