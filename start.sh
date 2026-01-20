#!/bin/bash
# =============================================================================
# Order Processing Platform - Startup Script (Linux/macOS)
# =============================================================================

set -e

echo "============================================"
echo "Order Processing Platform - Startup"
echo "============================================"
echo ""

# Check if .env exists
if [ ! -f .env ]; then
    echo "[ERROR] .env file not found!"
    echo ""
    echo "Please create .env file:"
    echo "  1. Copy .env.example to .env"
    echo "  2. Edit .env and replace all CHANGE_ME values"
    echo ""
    echo "Run: cp .env.example .env"
    exit 1
fi

# Check Docker
if ! command -v docker &> /dev/null; then
    echo "[ERROR] Docker is not installed!"
    echo ""
    echo "Please install Docker: https://docs.docker.com/get-docker/"
    exit 1
fi

# Check Docker Compose
if ! docker compose version &> /dev/null; then
    echo "[ERROR] Docker Compose is not installed!"
    echo ""
    echo "Please install Docker Compose or update Docker"
    exit 1
fi

echo "[INFO] Prerequisites check: OK"
echo ""

# Menu
echo "Choose startup mode:"
echo "  1. Full stack (Infrastructure + All Microservices)"
echo "  2. Infrastructure only (for local development)"
echo "  3. With Vault (Infrastructure + Microservices + Vault)"
echo "  4. Stop all services"
echo "  5. Rebuild and restart all"
echo ""
read -p "Enter choice (1-5): " choice

case $choice in
    1)
        echo ""
        echo "[INFO] Starting full stack..."
        docker compose up -d
        ;;
    2)
        echo ""
        echo "[INFO] Starting infrastructure only..."
        docker compose -f docker-compose-infra.yml up -d
        echo ""
        echo "[INFO] Infrastructure started. Run microservices locally:"
        echo "  ./gradlew :auth-service:bootRun"
        echo "  ./gradlew :api-gateway:bootRun"
        echo "  ./gradlew :user-service:bootRun"
        echo "  etc..."
        ;;
    3)
        echo ""
        echo "[INFO] Starting with Vault..."
        docker compose --profile with-vault up -d
        ;;
    4)
        echo ""
        echo "[INFO] Stopping all services..."
        docker compose down
        docker compose -f docker-compose-infra.yml down
        echo "[INFO] All services stopped."
        exit 0
        ;;
    5)
        echo ""
        echo "[WARN] This will rebuild all Docker images and restart services."
        read -p "Continue? (y/n): " confirm
        if [ "$confirm" != "y" ]; then
            exit 0
        fi

        echo "[INFO] Building Java artifacts..."
        ./gradlew clean build -x test

        echo "[INFO] Rebuilding Docker images..."
        docker compose down
        docker compose build --no-cache

        echo "[INFO] Starting services..."
        docker compose up -d
        ;;
    *)
        echo "[ERROR] Invalid choice!"
        exit 1
        ;;
esac

# Show status
echo ""
echo "============================================"
echo "Services Status"
echo "============================================"
sleep 5
docker compose ps

echo ""
echo "============================================"
echo "Health Checks"
echo "============================================"
echo ""
echo "API Gateway:         http://localhost:8080/actuator/health"
echo "Auth Service:        http://localhost:8087/actuator/health"
echo "User Service:        http://localhost:8081/actuator/health"
echo "Product Service:     http://localhost:8084/actuator/health"
echo "Order Service:       http://localhost:8083/actuator/health"
echo "Inventory Service:   http://localhost:8085/actuator/health"
echo "Notification Service:http://localhost:8086/actuator/health"
echo ""
echo "Kafka UI:            http://localhost:8090"
echo "Schema Registry:     http://localhost:8082"
echo "Vault (if enabled):  http://localhost:8200"
echo ""
echo "============================================"
echo "View Logs"
echo "============================================"
echo "To view logs:"
echo "  docker compose logs -f [service-name]"
echo ""
echo "Example:"
echo "  docker compose logs -f auth-service"
echo ""

