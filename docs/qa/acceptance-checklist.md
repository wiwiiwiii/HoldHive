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

## Regression Checks

- [ ] `cd backend && ./mvnw verify`
- [ ] `cd frontend && npm run test -- --run`
- [ ] `cd frontend && npm run build`
