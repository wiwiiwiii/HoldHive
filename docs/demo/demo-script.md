# HoldHive Demo Script

## Setup

1. Start local MySQL and run `scripts/mysql/init-local-mysql.sql` once if the database/user does not exist.
2. Start the backend.
3. Start the frontend.
4. Confirm `/api/v1/health` returns `UP`.

## Story

1. Introduce HoldHive as a simple portfolio snapshot tool.
2. Show the dashboard skeleton and explain where summary, holdings, and allocation charts will appear.
3. Explain the team split:
   - Member A: backend CRUD and MySQL.
   - Member B: valuation, pricing, errors, tests.
   - Member C: frontend dashboard and interaction.
   - Member D: QA, CI, demo, documentation.
4. Walk through planned API and database design in `docs/guideline/project/`.

## Fallback

If live services fail, use the final PDF in `docs/guideline/output/` and Lanhu boards in `docs/design/lanhu/png/`.
