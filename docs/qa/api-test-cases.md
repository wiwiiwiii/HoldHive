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

The test profile disables external market calls for deterministic local checks. To manually try live EastMoney/CoinGecko quotes, start the backend with `local` profile and keep `HOLDHIVE_MARKET_EXTERNAL_ENABLED=true` or omit the variable.

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
- Backend can combine EastMoney live search results with the local demo catalog. When external market calls are disabled, demo results still keep the same response contract.

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
- `priceMode` optional, default `BEST_AVAILABLE`.
- Price lookup order is live external quote -> fresh `price_snapshot` cache -> demo quote -> unavailable.
- In deterministic local tests using the H2 test profile, use `DEMO_ALLOWED` when checking demo quote ids.

Cases to check:
- [ ] Missing `providerQuoteIds` -> `400 Bad Request`, `code: VALIDATION_FAILED`
- [ ] Known demo id with `DEMO_ALLOWED` -> appears in `quotes`
- [ ] Known EastMoney id with external market enabled and `BEST_AVAILABLE` -> appears with `priceStatus: LIVE`
- [ ] Known crypto id `CRYPTO:BTC` with external market enabled and `BEST_AVAILABLE` -> appears with `provider: COINGECKO`, `priceStatus: LIVE`
- [ ] External provider unavailable but fresh DB snapshot exists -> appears with `priceStatus: CACHED`
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
- [ ] Existing DB instrument whose ticker is `VOO` or `FXAIX` -> `200 OK` with that instrument id and populated demo holdings
- [ ] Existing DB fund instrument with unknown ticker -> `200 OK`, empty `holdings`, `source: DISCLOSURE_UNAVAILABLE`, warning explains missing disclosure
- [ ] `coveragePercent` < 100 -> `warnings` explains partial coverage
- [ ] Non-existent `instrumentId` -> `404 Not Found`, `code: FUND_LOOKTHROUGH_NOT_FOUND`

## Portfolio Exposure

### GET /api/v1/portfolio/exposure

```bash
curl "http://localhost:8080/api/v1/portfolio/exposure"
curl "http://localhost:8080/api/v1/portfolio/exposure?lookthrough=true&priceMode=DEMO_ALLOWED"
```

- `lookthrough=false` returns direct holdings as exposure items.
- `lookthrough=true` decomposes known ETF/mutual-fund holdings into disclosed stock components and an `_UNDISCLOSED` residual item.
- `priceMode` follows the same rules as holdings and summary.

Expected `200 OK` shape:

```json
{
  "portfolioId": 1,
  "portfolioName": "My Portfolio",
  "baseCurrency": "USD",
  "lookthrough": true,
  "priceMode": "DEMO_ALLOWED",
  "totalMarketValue": 2500.00000000,
  "items": [
    {
      "ticker": "AAPL",
      "displayName": "Apple Inc.",
      "assetType": "STOCK",
      "directMarketValue": 1500.00000000,
      "fundLookthroughMarketValue": 100.00000000,
      "totalExposureValue": 1600.00000000,
      "exposurePercent": 64.00000000,
      "sources": ["DIRECT", "FUND:VOO"]
    }
  ],
  "warnings": [
    "AAPL appears both as direct holding and inside fund holdings."
  ]
}
```

Cases to check:

- [ ] Empty portfolio -> `items: []`, `totalMarketValue: 0`
- [ ] Direct stock + fund containing the same stock -> warning mentions overlap
- [ ] Fund coverage below 100% -> `_UNDISCLOSED` residual item appears
- [ ] Unpriced holding -> excluded from exposure with warning

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
- `Holding.instrumentId` must be present; use it, not `Holding.id`, when opening `/api/v1/funds/{instrumentId}/lookthrough`.

### PATCH /api/v1/holdings/{holdingId}

```bash
curl -i -X PATCH "http://localhost:8080/api/v1/holdings/{id}?priceMode=DEMO_ALLOWED" \
  -H "Content-Type: application/json" \
  -d '{
    "quantity": 8,
    "averagePurchasePrice": 320
  }'
```

Expected: `200 OK`, response body is the updated `Holding`.

Rules:

- `quantity` is required and must be greater than 0.
- `averagePurchasePrice` is required and must be 0 or greater.
- `priceMode` is optional. Use `DEMO_ALLOWED` during local demo testing when checking demo quotes.
- For `CASH` and `BANK_DEPOSIT`, backend keeps `averagePurchasePrice: 1.00000000` regardless of the submitted value.

Cases to check:

- [ ] Existing stock holding -> updates quantity, average purchase price, cost basis, and valuation.
- [ ] Existing cash or bank-deposit holding -> updates quantity but keeps fixed price and fixed average purchase price at `1.00000000`.
- [ ] Invalid quantity or average price -> `400 Bad Request`, `code: VALIDATION_FAILED`.
- [ ] Missing holding -> `404 Not Found`, `code: HOLDING_NOT_FOUND`.

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
  "id": 1,
  "instrumentId": 2,
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
