# HoldHive

All your holds in one hive.

HoldHive is a training project for building a portfolio dashboard with a Spring Boot backend, MySQL persistence, and a React frontend.

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

Start the backend:

```bash
cd backend
./mvnw spring-boot:run
```

Start the frontend:

```bash
cd frontend
npm install
npm run dev
```

Default URLs:

- Frontend: `http://localhost:5173`
- Backend health: `http://localhost:8080/api/v1/health`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Project Layout

```text
HoldHive/
├── backend/                 # Java 21 + Spring Boot API
├── frontend/                # React + TypeScript + Vite UI
├── docs/
│   ├── guideline/           # Project plan, API, database, Git/CI docs, final PDF
│   └── design/              # Lanhu design boards and visual assets
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

## Branching

- Create feature branches from `qa`: `feature/<story-id>-<short-name>`.
- Open pull requests back into `qa`.
- Promote `qa` to `main` after integration testing.
- Use `hotfix/*` only for urgent fixes from `main` or `prod`.

Detailed process: `docs/guideline/project/git_branching_ci_zh.md`.

## Team Contribution

- Start here for local setup and directory ownership: `CONTRIBUTING.md`.
- Member-specific implementation map: `docs/guideline/project/member_directory_map_zh.md`.

## Documentation

- Technical guideline index: `docs/guideline/README.md`
- Product and execution plan: `docs/guideline/project/team_project_guideline_zh.md`
- Database design: `docs/guideline/project/database_design_zh.md`
- REST API contract: `docs/guideline/project/api_documentation_zh.md`
- Design boards: `docs/design/lanhu/README.md`
