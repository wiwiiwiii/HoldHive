# HoldHive Acceptance Checklist

Use this checklist for `qa` branch validation.

## Local Startup

- [ ] `cp .env.example .env`
- [ ] `mysql -u root -p < scripts/mysql/init-local-mysql.sql`
- [ ] `cd backend && ./mvnw spring-boot:run`
- [ ] `GET http://localhost:8080/api/v1/health` returns `UP`
- [ ] `cd frontend && npm install && npm run dev`
- [ ] Frontend opens at `http://localhost:5173`

## MVP Functional Checks

- [ ] Browse holdings
- [ ] Add holding
- [ ] Delete holding
- [ ] View portfolio summary
- [ ] View allocation chart
- [ ] See clear state when prices are demo, cached, or unavailable

### Asset Type Coverage

- [ ] Stock holding displays correctly
- [ ] ETF holding displays correctly
- [ ] Mutual fund holding displays correctly (and fund lookthrough works)
- [ ] Crypto holding displays correctly
- [ ] Cash holding displays correctly
- [ ] Bank deposit holding displays correctly

### Edge Cases & UX States

- [ ] API failure shows a user-friendly error message (not a raw stack trace)
- [ ] Empty portfolio shows a clear "no holdings" state instead of a blank/broken screen
- [ ] Deleting a holding requires a confirmation step
- [ ] Add-holding form validates required fields and rejects bad input (e.g. negative quantity)
- [ ] Slow response or unavailable price shows a loading/unavailable indicator instead of hanging or crashing

## Regression Checks

- [ ] `cd backend && ./mvnw verify`
- [ ] `cd frontend && npm run test -- --run`
- [ ] `cd frontend && npm run build`
