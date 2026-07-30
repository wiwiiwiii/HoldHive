# HoldHive Demo Script

## Setup

1. Start local MySQL and run `scripts/mysql/init-local-mysql.sql` once if the database/user does not exist.
2. Copy `.env.example` to `.env`; optionally set `DEEPSEEK_API_KEY` for live AI insights.
3. Start the backend with `make backend` or `cd backend && ./mvnw spring-boot:run`.
4. Start the frontend with `make frontend` or `cd frontend && npm install && npm run dev`.
5. Confirm `/api/v1/health` returns `UP`.

The default MySQL/Flyway startup seeds a demo portfolio with stock, ETF, mutual fund, crypto, cash, bank deposit, and an unpriced holding. The UI should have readable data immediately after startup.

## Story

1. Start at Gateway. Explain the honeycomb metaphor: each vertex opens one focused workspace in the same portfolio.
2. Open Dashboard. Show total value, allocation, data status, direct holdings, and look-through exposure. Point out cached/fixed/unavailable price labels.
3. Open Holdings. Edit a holding quantity or average purchase price, then show that market value and floating P&L come back from the backend.
4. Open Add Holding. Search a stock or crypto, add it, and show the user-friendly success toast. For ETF/mutual fund, mention the look-through overlap warning.
5. Open Analysis. Show allocation X-Ray, concentration, sector exposure, profit/loss, and AI Insights. Explain that AI interprets backend-computed facts and is not investment advice.
6. Open Settings. Switch day/night theme and data mode; return to Dashboard to show refresh behavior.
7. Explain the team split:
   - Member A: backend CRUD and MySQL.
   - Member B: valuation, pricing, errors, tests.
   - Member C: frontend dashboard and interaction.
   - Member D: QA, CI, demo, documentation.
8. Close with the release flow: features entered `qa` by PR, `qa` was promoted to `main`, and the final snapshot is tagged `1.0.0`.

## Fallback

If live market services fail, use `DEMO_ALLOWED` data mode or start with `HOLDHIVE_MARKET_EXTERNAL_ENABLED=false`; seed/cache data still supports the demo.

If LLM fails, continue with structured Analysis cards and explain that AI text degrades independently from deterministic portfolio calculations.

If the application cannot run, use the final PDF in `docs/guideline/output/` and Lanhu boards in `docs/design/lanhu/png/`.
