.DEFAULT_GOAL := help

FRONTEND_DIR := frontend
BACKEND_DIR := backend
DOCS_DIR := docs
DOCS_PORT ?= 8000

.PHONY: help frontend backend docs

help: ## Show available facade commands.
	@awk 'BEGIN {FS = ":.*##"; printf "HoldHive facade commands:\n"} /^[a-zA-Z0-9_-]+:.*##/ {printf "  %-12s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

frontend: ## Start the React/Vite frontend dev server.
	cd $(FRONTEND_DIR) && npm run dev

backend: ## Start the Spring Boot backend API server.
	cd $(BACKEND_DIR) && set -a; [ ! -f ../.env ] || . ../.env; set +a; ./mvnw spring-boot:run

docs: ## Serve the docs directory (default: http://localhost:8000).
	python3 -m http.server $(DOCS_PORT) --directory $(DOCS_DIR)
