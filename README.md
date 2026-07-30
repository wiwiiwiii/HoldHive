# HoldHive

All your holds in one hive.

HoldHive is a training project for building a multi-asset portfolio dashboard with a Spring Boot backend, MySQL persistence, and a React frontend.

Current release: `1.0.0` from `main`. Ongoing fixes and documentation updates should branch from `qa`.

## What It Does

- Tracks stocks, ETFs, mutual funds, crypto, cash, and bank deposits.
- Shows portfolio value, cost basis, unrealized P&L, allocation, and price status.
- Supports holding create, edit, delete, market search, market quotes, fund lookthrough, and portfolio exposure.
- Includes an Analysis page with deterministic portfolio facts plus optional DeepSeek-compatible AI insights.
- Seeds a demo portfolio through Flyway so the frontend can read useful data immediately after startup.

## Current App Experience

- Gateway opens with a hive-style hexagonal portal map and day/night logo theme support.
- The left navigation can be resized within fixed bounds, so each member can test on different screen widths.
- Holdings displays purchase price, market price, market value, cost basis, and floating P&L from backend-calculated fields.
- Dashboard and Analysis use backend exposure data, including fund lookthrough where available.
- Analysis refreshes automatically after holding changes and can also be refreshed manually.
- Loading states use a hive-style thinking animation instead of blank panels.
- User-facing add/edit/delete messages summarize the actual result without exposing raw JSON.

## Quick Start

Prerequisites:

- Java 21
- Node.js 20+
- MySQL 8.x running locally

No Docker installation is required for local development.

Create the local MySQL database and user once:

```bash
cp .env.example .env
mysql -u root -p < scripts/mysql/init-local-mysql.sql
```

Optional: enable live AI insights by adding your local key to `.env`:

```bash
DEEPSEEK_API_KEY=<your-local-key>
```

Start the backend and frontend:

```bash
make backend
make frontend
```

Default URLs:

- Frontend: `http://localhost:5173`
- Backend health: `http://localhost:8080/api/v1/health`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

If `make` is unavailable, run the same commands manually:

```bash
cd backend
./mvnw spring-boot:run

cd ../frontend
npm install
npm run dev
```

## Demo Data

Flyway migration `V4__seed_demo_portfolio_data.sql` seeds the default portfolio with:

- Stocks: `AAPL`, `MSFT`, `NVDA`, `600519`
- ETFs / mutual funds: `VOO`, `QQQ`, `FXAIX`, `005827`
- Crypto: `BTC`, `ETH`
- Cash and bank deposit: `USD`, `USD_DEPOSIT`
- One unpriced holding for unavailable-price UX

Use Settings -> Data mode to switch between `BEST_AVAILABLE`, `LIVE_ONLY`, and `DEMO_ALLOWED`.

Data mode is passed through the frontend API layer to holdings, summary, performance, exposure, quotes, and analysis endpoints.

## Backend API Surface

The frontend currently depends on these backend areas:

- `GET /api/v1/health`
- `GET /api/v1/holdings`
- `POST /api/v1/holdings`
- `PATCH /api/v1/holdings/{holdingId}`
- `DELETE /api/v1/holdings/{holdingId}`
- `GET /api/v1/portfolio/summary`
- `GET /api/v1/portfolio/exposure?lookthrough=true`
- `GET /api/v1/portfolio/analysis/insights/full`
- `GET /api/v1/market/search`
- `GET /api/v1/market/quotes`
- `GET /api/v1/funds/{instrumentId}/lookthrough`

Detailed request/response examples are maintained in `docs/guideline/project/api_documentation_zh.md` and smoke cases are in `docs/qa/api-test-cases.md`.

## Project Layout

```text
HoldHive/
├── backend/                 # Java 21 + Spring Boot API
├── frontend/                # React + TypeScript + Vite UI
├── docs/
│   ├── guideline/           # Project plan, API, database, Git/CI docs, final PDF
│   ├── design/              # Lanhu design boards and visual assets
│   ├── demo/                # Demo walkthrough
│   ├── presentation/        # Presentation notes and assets
│   └── qa/                  # Acceptance checklist and API smoke cases
├── scripts/mysql/           # Local MySQL bootstrap script
├── static/img/              # Logo assets
└── .env.example             # Local environment template
```

## Common Commands

Backend:

```bash
cd backend
./mvnw test
./mvnw verify
./mvnw spring-boot:run
```

Frontend:

```bash
cd frontend
npm install
npm run test -- --run
npm run build
npm run dev
```

Full local check:

```bash
(cd backend && ./mvnw verify)
(cd frontend && npm install && npm run test -- --run && npm run build)
```

API smoke examples:

```bash
curl http://localhost:8080/api/v1/health
curl "http://localhost:8080/api/v1/holdings?priceMode=DEMO_ALLOWED"
curl "http://localhost:8080/api/v1/portfolio/exposure?lookthrough=true&priceMode=DEMO_ALLOWED"
curl -N "http://localhost:8080/api/v1/portfolio/analysis/insights/full?priceMode=DEMO_ALLOWED"
```

## Branching

- Create feature branches from `qa`: `feature/<story-id>-<short-name>`.
- Create documentation branches from `qa`: `docs/<topic>`.
- Open pull requests back into `qa`.
- Promote `qa` to `main` with a release PR after integration testing.
- Tag releases from `main`; current release tag is `1.0.0`.
- Use `hotfix/*` only for urgent fixes from a released `main`.

Detailed process: `docs/guideline/project/git_branching_ci_zh.md`.

## Daily Sync

Before starting work:

```bash
git switch qa
git pull origin qa
git switch -c feature/<your-task>
```

After your PR is merged:

```bash
git switch qa
git pull origin qa
git branch -d feature/<your-task>
```

If a teammate merges backend or frontend changes while you are working, pull the latest `qa`, then merge or rebase it into your feature branch before continuing.

## Team Contribution

- Start here for local setup and directory ownership: `CONTRIBUTING.md`.
- Member-specific implementation map: `docs/guideline/project/member_directory_map_zh.md`.

## Documentation

- Technical guideline index: `docs/guideline/README.md`
- Product and execution plan: `docs/guideline/project/team_project_guideline_zh.md`
- Database design: `docs/guideline/project/database_design_zh.md`
- REST API contract: `docs/guideline/project/api_documentation_zh.md`
- Design boards: `docs/design/lanhu/README.md`
- QA checklist and API smoke cases: `docs/qa/README.md`
- Demo walkthrough: `docs/demo/demo-script.md`
