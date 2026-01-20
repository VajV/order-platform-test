# =============================================================================
# Order Processing Platform - Startup Script (PowerShell)
# =============================================================================

param(
    [Parameter(Mandatory=$false)]
    [ValidateSet('full', 'infra', 'vault', 'stop', 'rebuild')]
    [string]$Mode = 'menu'
)

$ErrorActionPreference = "Stop"

function Write-Header {
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host "Order Processing Platform - Startup" -ForegroundColor Cyan
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host ""
}

function Check-Prerequisites {
    Write-Host "[INFO] Checking prerequisites..." -ForegroundColor Yellow
    
    # Check .env file
    if (-not (Test-Path ".env")) {
        Write-Host "[ERROR] .env file not found!" -ForegroundColor Red
        Write-Host ""
        Write-Host "Please create .env file:" -ForegroundColor Yellow
        Write-Host "  1. Copy .env.example to .env" -ForegroundColor White
        Write-Host "  2. Edit .env and replace all CHANGE_ME values" -ForegroundColor White
        Write-Host ""
        Write-Host "Run: Copy-Item .env.example .env" -ForegroundColor Green
        exit 1
    }
    
    # Check Docker
    try {
        docker --version | Out-Null
    } catch {
        Write-Host "[ERROR] Docker is not installed or not running!" -ForegroundColor Red
        Write-Host ""
        Write-Host "Please install Docker Desktop: https://www.docker.com/products/docker-desktop" -ForegroundColor Yellow
        exit 1
    }
    
    # Check Docker Compose
    try {
        docker compose version | Out-Null
    } catch {
        Write-Host "[ERROR] Docker Compose is not installed!" -ForegroundColor Red
        Write-Host ""
        Write-Host "Please install Docker Compose or update Docker Desktop" -ForegroundColor Yellow
        exit 1
    }
    
    Write-Host "[INFO] Prerequisites check: OK" -ForegroundColor Green
    Write-Host ""
}

function Show-Status {
    Write-Host ""
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host "Services Status" -ForegroundColor Cyan
    Write-Host "============================================" -ForegroundColor Cyan
    Start-Sleep -Seconds 5
    docker compose ps
    
    Write-Host ""
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host "Health Checks" -ForegroundColor Cyan
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "API Gateway:         http://localhost:8080/actuator/health" -ForegroundColor White
    Write-Host "Auth Service:        http://localhost:8087/actuator/health" -ForegroundColor White
    Write-Host "User Service:        http://localhost:8081/actuator/health" -ForegroundColor White
    Write-Host "Product Service:     http://localhost:8084/actuator/health" -ForegroundColor White
    Write-Host "Order Service:       http://localhost:8083/actuator/health" -ForegroundColor White
    Write-Host "Inventory Service:   http://localhost:8085/actuator/health" -ForegroundColor White
    Write-Host "Notification Service:http://localhost:8086/actuator/health" -ForegroundColor White
    Write-Host ""
    Write-Host "Kafka UI:            http://localhost:8090" -ForegroundColor Yellow
    Write-Host "Schema Registry:     http://localhost:8082" -ForegroundColor Yellow
    Write-Host "Vault (if enabled):  http://localhost:8200" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host "View Logs" -ForegroundColor Cyan
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host "To view logs:" -ForegroundColor White
    Write-Host "  docker compose logs -f [service-name]" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Example:" -ForegroundColor White
    Write-Host "  docker compose logs -f auth-service" -ForegroundColor Green
    Write-Host ""
}

function Start-FullStack {
    Write-Host ""
    Write-Host "[INFO] Starting full stack..." -ForegroundColor Yellow
    docker compose up -d
    Show-Status
}

function Start-Infrastructure {
    Write-Host ""
    Write-Host "[INFO] Starting infrastructure only..." -ForegroundColor Yellow
    docker compose -f docker-compose-infra.yml up -d
    Write-Host ""
    Write-Host "[INFO] Infrastructure started. Run microservices locally:" -ForegroundColor Green
    Write-Host "  .\gradlew.bat :auth-service:bootRun" -ForegroundColor Gray
    Write-Host "  .\gradlew.bat :api-gateway:bootRun" -ForegroundColor Gray
    Write-Host "  .\gradlew.bat :user-service:bootRun" -ForegroundColor Gray
    Write-Host "  etc..." -ForegroundColor Gray
    Show-Status
}

function Start-WithVault {
    Write-Host ""
    Write-Host "[INFO] Starting with Vault..." -ForegroundColor Yellow
    docker compose --profile with-vault up -d
    Show-Status
}

function Stop-AllServices {
    Write-Host ""
    Write-Host "[INFO] Stopping all services..." -ForegroundColor Yellow
    docker compose down
    docker compose -f docker-compose-infra.yml down
    Write-Host "[INFO] All services stopped." -ForegroundColor Green
}

function Rebuild-AndRestart {
    Write-Host ""
    Write-Host "[WARN] This will rebuild all Docker images and restart services." -ForegroundColor Yellow
    $confirm = Read-Host "Continue? (y/n)"
    if ($confirm -ne 'y') {
        exit 0
    }

    Write-Host "[INFO] Building Java artifacts..." -ForegroundColor Yellow
    .\gradlew.bat clean build -x test

    Write-Host "[INFO] Rebuilding Docker images..." -ForegroundColor Yellow
    docker compose down
    docker compose build --no-cache

    Write-Host "[INFO] Starting services..." -ForegroundColor Yellow
    docker compose up -d
    Show-Status
}

# Main
Write-Header
Check-Prerequisites

if ($Mode -eq 'menu') {
    Write-Host "Choose startup mode:" -ForegroundColor Cyan
    Write-Host "  1. Full stack (Infrastructure + All Microservices)" -ForegroundColor White
    Write-Host "  2. Infrastructure only (for local development)" -ForegroundColor White
    Write-Host "  3. With Vault (Infrastructure + Microservices + Vault)" -ForegroundColor White
    Write-Host "  4. Stop all services" -ForegroundColor White
    Write-Host "  5. Rebuild and restart all" -ForegroundColor White
    Write-Host ""
    $choice = Read-Host "Enter choice (1-5)"
    
    switch ($choice) {
        '1' { Start-FullStack }
        '2' { Start-Infrastructure }
        '3' { Start-WithVault }
        '4' { Stop-AllServices }
        '5' { Rebuild-AndRestart }
        default {
            Write-Host "[ERROR] Invalid choice!" -ForegroundColor Red
            exit 1
        }
    }
} else {
    switch ($Mode) {
        'full' { Start-FullStack }
        'infra' { Start-Infrastructure }
        'vault' { Start-WithVault }
        'stop' { Stop-AllServices }
        'rebuild' { Rebuild-AndRestart }
    }
}

