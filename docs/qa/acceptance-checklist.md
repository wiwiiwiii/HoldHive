# HoldHive Acceptance Checklist

Use this checklist for `qa` branch validation and before opening a `qa -> main` release PR.

## Local Startup

- [ ] `cp .env.example .env`
- [ ] `mysql -u root -p < scripts/mysql/init-local-mysql.sql`
- [ ] `cd backend && ./mvnw spring-boot:run`
- [ ] `GET http://localhost:8080/api/v1/health` returns `UP`
- [ ] `cd frontend && npm install && npm run dev`
- [ ] Frontend opens at `http://localhost:5173`
- [ ] Seeded demo holdings appear immediately on Dashboard/Holdings after backend startup

## MVP Functional Checks

- [ ] Browse holdings
- [ ] Add holding
- [ ] Edit holding quantity and average purchase price
- [ ] Delete holding
- [ ] View portfolio summary
- [ ] View allocation chart
- [ ] View lookthrough exposure with direct + fund-derived values
- [ ] Switch Settings data mode between `BEST_AVAILABLE`, `LIVE_ONLY`, and `DEMO_ALLOWED`; Dashboard/Holdings/Performance/Analysis refresh with the selected mode
- [ ] Analysis page loads structured facts and AI insights; if LLM is unavailable, structured cards still render
- [ ] See clear state when prices are demo, cached, or unavailable

### Asset Type Coverage

- [ ] Stock holding displays correctly
- [ ] ETF holding displays correctly
- [ ] Mutual fund holding displays correctly (and fund lookthrough works)
- [ ] Crypto holding displays correctly with `CRYPTO:BTC` / `CRYPTO:ETH` quote behavior
- [ ] Cash holding displays correctly
- [ ] Bank deposit holding displays correctly

### Edge Cases & UX States

- [ ] API failure shows a user-friendly error message (not a raw stack trace)
- [ ] Empty portfolio shows a clear "no holdings" state instead of a blank/broken screen
- [ ] Deleting a holding requires a confirmation step
- [ ] Add-holding form validates required fields and rejects bad input (e.g. negative quantity)
- [ ] Slow response or unavailable price shows a loading/unavailable indicator instead of hanging or crashing
- [ ] Fund overlap warning appears when direct holdings also appear inside fund lookthrough
- [ ] Unknown fund disclosure shows a warning, not a broken page
- [ ] Gateway hexagon fits without vertical scrolling on normal laptop viewport
- [ ] Sidebar can be resized but remains within min/max width
- [ ] Add/edit/delete success and failure messages are short human-readable messages, not raw JSON

## Regression Checks

- [ ] `cd backend && ./mvnw verify`
- [ ] `cd frontend && npm run test -- --run`
- [ ] `cd frontend && npm run build`
- [ ] GitHub Actions required checks pass: `backend`, `backend mysql smoke`, `frontend`, `docs`
