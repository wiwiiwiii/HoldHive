# Demo Seed Data

The backend now seeds a ready-to-read demo portfolio through Flyway migration `V4__seed_demo_portfolio_data.sql`.

When a teammate pulls the latest branch and starts the backend against a new H2 test database or local MySQL database, Flyway creates the default portfolio and inserts representative holdings automatically.

## What Is Seeded

Default portfolio: `My Portfolio`, base currency `USD`.

| Asset type | Tickers | Purpose |
| --- | --- | --- |
| STOCK | `AAPL`, `MSFT`, `NVDA`, `600519` | Direct equity holdings, US and China market examples |
| ETF | `VOO`, `QQQ` | Fund-like assets for look-through and undisclosed residual warnings |
| MUTUAL_FUND | `FXAIX`, `005827` | On/off-market fund examples; `FXAIX` has demo look-through |
| CRYPTO | `BTC`, `ETH` | Crypto allocation and quote examples |
| CASH | `USD` | Fixed-value cash position |
| BANK_DEPOSIT | `USD_DEPOSIT` | Fixed-value bank deposit position |
| STOCK / unpriced | `PRIVATE_NOTE` | Demonstrates unavailable price handling |

Seeded `price_snapshot` rows use provider `SEED_CACHE`, status `CACHED`, and the migration execution timestamp. Local and test cache TTL is `30d` so the default `BEST_AVAILABLE` mode can read these prices consistently during development.

## Fast Smoke Checks

Start backend with the deterministic H2 profile:

```bash
cd backend
BACKEND_PORT=8080 ./mvnw spring-boot:run \
  -Dspring-boot.run.profiles=test \
  -Dspring-boot.run.useTestClasspath=true
```

Then verify the seed data:

```bash
curl "http://localhost:8080/api/v1/holdings?sort=marketValue,desc"
curl "http://localhost:8080/api/v1/portfolio/summary"
curl "http://localhost:8080/api/v1/portfolio/exposure?lookthrough=true"
curl "http://localhost:8080/api/v1/market/search?query=btc&market=CRYPTO"
```

Frontend can use the first response to populate dashboard cards immediately. Use `instrumentId` from any `VOO` or `FXAIX` holding to call `/api/v1/funds/{instrumentId}/lookthrough`.
