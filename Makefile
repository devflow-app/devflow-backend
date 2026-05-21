# ============================================================
#  DevFlow — Developer Commands
#  Requires: GNU Make (Windows: choco install make)
# ============================================================

.PHONY: help infra-up infra-down infra-clean build test \
        auth-run gateway-run project-run notification-run ai-run \
        lint format

## ── Help ──────────────────────────────────────────────────
help:
	@echo ""
	@echo "  DevFlow Developer Commands"
	@echo "  ─────────────────────────────────────────"
	@echo "  make infra-up          Start all infrastructure (Docker)"
	@echo "  make infra-down        Stop infrastructure"
	@echo "  make infra-clean       Stop + remove volumes"
	@echo "  make build             Build all modules (skip tests)"
	@echo "  make test              Run all tests"
	@echo "  make auth-run          Run auth-service locally"
	@echo "  make gateway-run       Run api-gateway locally"
	@echo "  make project-run       Run project-service locally"
	@echo "  make notification-run  Run notification-service locally"
	@echo "  make ai-run            Run ai-service locally"
	@echo ""

## ── Infrastructure ───────────────────────────────────────
infra-up:
	@echo "🐳 Starting DevFlow infrastructure..."
	docker-compose up -d
	@echo "✅ Infrastructure ready!"
	@echo "   PostgreSQL  → localhost:5432"
	@echo "   Redis       → localhost:6379"
	@echo "   Kafka       → localhost:9092"
	@echo "   Kafka UI    → http://localhost:8090"
	@echo "   Elasticsearch → http://localhost:9200"
	@echo "   MailHog     → http://localhost:8025"
	@echo "   Prometheus  → http://localhost:9090"
	@echo "   Grafana     → http://localhost:3000"

infra-down:
	@echo "🛑 Stopping infrastructure..."
	docker-compose down

infra-clean:
	@echo "🧹 Stopping infrastructure and removing volumes..."
	docker-compose down -v

## ── Build ─────────────────────────────────────────────────
build:
	@echo "🔨 Building all modules..."
	./mvnw clean install -DskipTests

build-common:
	./mvnw clean install -pl common -DskipTests

## ── Test ──────────────────────────────────────────────────
test:
	@echo "🧪 Running all tests..."
	./mvnw test

test-auth:
	./mvnw test -pl auth-service

test-project:
	./mvnw test -pl project-service

## ── Run Services ─────────────────────────────────────────
auth-run:
	./mvnw spring-boot:run -pl auth-service

gateway-run:
	./mvnw spring-boot:run -pl api-gateway

project-run:
	./mvnw spring-boot:run -pl project-service

notification-run:
	./mvnw spring-boot:run -pl notification-service

ai-run:
	./mvnw spring-boot:run -pl ai-service

## ── Code Quality ──────────────────────────────────────────
format:
	./mvnw spotless:apply

lint:
	./mvnw checkstyle:check
