# =============================================
# Order Processing Platform - Makefile
# =============================================

.PHONY: help dev-up dev-down build test k3d-up k3d-down helm-install helm-upgrade clean

# Colors for output
CYAN := \033[36m
GREEN := \033[32m
YELLOW := \033[33m
RESET := \033[0m

help:
	@echo "$(CYAN)📦 Order Processing Platform - Development Commands$(RESET)"
	@echo ""
	@echo "$(GREEN)Local Development (Docker Compose):$(RESET)"
	@echo "  make dev-up          Start all services"
	@echo "  make dev-down        Stop all services"
	@echo "  make dev-logs        Stream logs from all services"
	@echo "  make dev-ps          Show running containers"
	@echo ""
	@echo "$(GREEN)Build & Test:$(RESET)"
	@echo "  make build           Build all services"
	@echo "  make test            Run all tests"
	@echo "  make test-unit       Run unit tests only"
	@echo "  make test-integration Run integration tests"
	@echo "  make test-contract   Run contract tests"
	@echo "  make coverage        Generate coverage report"
	@echo ""
	@echo "$(GREEN)Kubernetes (k3d):$(RESET)"
	@echo "  make k3d-up          Create k3d cluster"
	@echo "  make k3d-down        Delete k3d cluster"
	@echo "  make helm-install    Install Helm chart"
	@echo "  make helm-upgrade    Upgrade Helm chart"
	@echo "  make helm-uninstall  Uninstall Helm chart"
	@echo "  make k8s-status      Show cluster status"
	@echo "  make port-forward    Forward API Gateway port"
	@echo ""
	@echo "$(GREEN)Utilities:$(RESET)"
	@echo "  make clean           Clean build artifacts"
	@echo "  make docker-build    Build all Docker images"
	@echo "  make docker-push     Push images to registry"

# =============================================
# Local Development
# =============================================

dev-up:
	@echo "$(CYAN)🚀 Starting services with Docker Compose...$(RESET)"
	docker-compose up -d
	@echo "$(GREEN)✅ Services started!$(RESET)"
	@echo "  API Gateway: http://localhost:8080"
	@echo "  Swagger UI:  http://localhost:8080/swagger-ui.html"
	@echo "  Kafka UI:    http://localhost:8088"

dev-down:
	@echo "$(YELLOW)🛑 Stopping services...$(RESET)"
	docker-compose down -v
	@echo "$(GREEN)✅ Services stopped$(RESET)"

dev-logs:
	docker-compose logs -f

dev-ps:
	docker-compose ps

dev-infra:
	@echo "$(CYAN)🚀 Starting infrastructure only...$(RESET)"
	docker-compose -f docker-compose-infra.yml up -d

# =============================================
# Build & Test
# =============================================

build:
	@echo "$(CYAN)🔨 Building all services...$(RESET)"
	./gradlew build -x test --parallel
	@echo "$(GREEN)✅ Build complete$(RESET)"

test:
	@echo "$(CYAN)🧪 Running all tests...$(RESET)"
	./gradlew test --parallel
	@echo "$(GREEN)✅ Tests complete$(RESET)"

test-unit:
	@echo "$(CYAN)🧪 Running unit tests...$(RESET)"
	./gradlew test --parallel -x ':order-service:contractTest' -x ':product-service:contractTest'

test-integration:
	@echo "$(CYAN)🧪 Running integration tests...$(RESET)"
	./gradlew test --tests '*IntegrationTest' --parallel

test-contract:
	@echo "$(CYAN)📜 Running contract tests...$(RESET)"
	./gradlew :order-service:contractTest :product-service:contractTest

coverage:
	@echo "$(CYAN)📊 Generating coverage report...$(RESET)"
	./gradlew test jacocoTestReport --parallel
	@echo "$(GREEN)✅ Coverage report generated$(RESET)"
	@echo "  Open: order-service/build/reports/jacoco/test/html/index.html"

# =============================================
# Kubernetes (k3d)
# =============================================

k3d-up:
	@echo "$(CYAN)🏗️ Creating k3d cluster...$(RESET)"
	k3d cluster create order-platform \
		--agents 2 \
		--port "8080:80@loadbalancer" \
		--port "8443:443@loadbalancer" \
		--api-port 6550 \
		--wait
	@echo "$(YELLOW)⏳ Waiting for cluster to be ready...$(RESET)"
	kubectl wait --for=condition=Ready nodes --all --timeout=120s
	kubectl create namespace order-platform-dev --dry-run=client -o yaml | kubectl apply -f -
	@echo "$(GREEN)✅ k3d cluster created!$(RESET)"
	@echo "  Namespace: order-platform-dev"

k3d-down:
	@echo "$(YELLOW)🗑️ Deleting k3d cluster...$(RESET)"
	k3d cluster delete order-platform
	@echo "$(GREEN)✅ Cluster deleted$(RESET)"

helm-deps:
	@echo "$(CYAN)📦 Updating Helm dependencies...$(RESET)"
	helm dependency update ./helm/order-platform

helm-install: helm-deps
	@echo "$(CYAN)📦 Installing Helm chart...$(RESET)"
	helm install order-platform ./helm/order-platform \
		-n order-platform-dev \
		--create-namespace \
		-f ./helm/order-platform/values-dev.yaml \
		--wait \
		--timeout 10m
	@echo "$(GREEN)✅ Helm chart installed!$(RESET)"
	@make k8s-status

helm-upgrade: helm-deps
	@echo "$(CYAN)🔄 Upgrading Helm chart...$(RESET)"
	helm upgrade order-platform ./helm/order-platform \
		-n order-platform-dev \
		-f ./helm/order-platform/values-dev.yaml \
		--wait \
		--timeout 10m
	@echo "$(GREEN)✅ Helm chart upgraded!$(RESET)"

helm-uninstall:
	@echo "$(YELLOW)🗑️ Uninstalling Helm chart...$(RESET)"
	helm uninstall order-platform -n order-platform-dev
	@echo "$(GREEN)✅ Helm chart uninstalled$(RESET)"

helm-template:
	@echo "$(CYAN)📄 Rendering Helm templates...$(RESET)"
	helm template order-platform ./helm/order-platform \
		-f ./helm/order-platform/values-dev.yaml

k8s-status:
	@echo "$(CYAN)📊 Cluster Status:$(RESET)"
	@echo ""
	@echo "$(GREEN)Pods:$(RESET)"
	@kubectl get pods -n order-platform-dev -o wide
	@echo ""
	@echo "$(GREEN)Services:$(RESET)"
	@kubectl get svc -n order-platform-dev
	@echo ""
	@echo "$(GREEN)Ingresses:$(RESET)"
	@kubectl get ingress -n order-platform-dev

port-forward:
	@echo "$(CYAN)🔌 Port forwarding API Gateway to localhost:8080...$(RESET)"
	kubectl port-forward -n order-platform-dev svc/order-platform-api-gateway 8080:8080

logs:
	@echo "$(CYAN)📋 Streaming logs...$(RESET)"
	kubectl logs -f -n order-platform-dev -l app.kubernetes.io/instance=order-platform --all-containers=true --max-log-requests=10

# =============================================
# Docker
# =============================================

docker-build: build
	@echo "$(CYAN)🐳 Building Docker images...$(RESET)"
	docker-compose build
	@echo "$(GREEN)✅ Docker images built$(RESET)"

docker-push:
	@echo "$(CYAN)🚀 Pushing Docker images...$(RESET)"
	docker-compose push
	@echo "$(GREEN)✅ Images pushed$(RESET)"

# =============================================
# Utilities
# =============================================

clean:
	@echo "$(YELLOW)🧹 Cleaning build artifacts...$(RESET)"
	./gradlew clean
	docker system prune -f
	@echo "$(GREEN)✅ Clean complete$(RESET)"

# Inject secrets from .secrets file
inject-secrets:
	@echo "$(CYAN)🔐 Injecting secrets...$(RESET)"
	kubectl create secret generic app-secrets \
		--from-env-file=.env \
		-n order-platform-dev \
		--dry-run=client -o yaml | kubectl apply -f -
	@echo "$(GREEN)✅ Secrets injected$(RESET)"

# Quick demo
demo: dev-up
	@echo ""
	@echo "$(GREEN)🎉 Demo environment is ready!$(RESET)"
	@echo ""
	@echo "$(CYAN)Available endpoints:$(RESET)"
	@echo "  📡 API Gateway:  http://localhost:8080"
	@echo "  📚 Swagger UI:   http://localhost:8080/swagger-ui.html"
	@echo "  📊 Kafka UI:     http://localhost:8088"
	@echo ""
	@echo "$(CYAN)Quick test:$(RESET)"
	@echo "  curl http://localhost:8080/actuator/health"

