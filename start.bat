@echo off
REM =============================================================================
REM Order Processing Platform - Startup Script (Windows)
REM =============================================================================

echo ============================================
echo Order Processing Platform - Startup
echo ============================================
echo.

REM Check if .env exists
if not exist .env (
    echo [ERROR] .env file not found!
    echo.
    echo Please create .env file:
    echo   1. Copy .env.example to .env
    echo   2. Edit .env and replace all CHANGE_ME values
    echo.
    echo Run: copy .env.example .env
    pause
    exit /b 1
)

REM Check Docker
docker --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker is not installed or not running!
    echo.
    echo Please install Docker Desktop: https://www.docker.com/products/docker-desktop
    pause
    exit /b 1
)

REM Check Docker Compose
docker compose version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker Compose is not installed!
    echo.
    echo Please install Docker Compose or update Docker Desktop
    pause
    exit /b 1
)

echo [INFO] Prerequisites check: OK
echo.

REM Menu
echo Choose startup mode:
echo   1. Full stack (Infrastructure + All Microservices)
echo   2. Infrastructure only (for local development)
echo   3. With Vault (Infrastructure + Microservices + Vault)
echo   4. Stop all services
echo   5. Rebuild and restart all
echo.
set /p choice="Enter choice (1-5): "

if "%choice%"=="1" goto full
if "%choice%"=="2" goto infra
if "%choice%"=="3" goto with_vault
if "%choice%"=="4" goto stop
if "%choice%"=="5" goto rebuild
echo [ERROR] Invalid choice!
pause
exit /b 1

:full
echo.
echo [INFO] Starting full stack...
docker compose up -d
goto show_status

:infra
echo.
echo [INFO] Starting infrastructure only...
docker compose -f docker-compose-infra.yml up -d
echo.
echo [INFO] Infrastructure started. Run microservices locally:
echo   gradlew.bat :auth-service:bootRun
echo   gradlew.bat :api-gateway:bootRun
echo   gradlew.bat :user-service:bootRun
echo   etc...
goto show_status

:with_vault
echo.
echo [INFO] Starting with Vault...
docker compose --profile with-vault up -d
goto show_status

:stop
echo.
echo [INFO] Stopping all services...
docker compose down
docker compose -f docker-compose-infra.yml down
echo [INFO] All services stopped.
pause
exit /b 0

:rebuild
echo.
echo [WARN] This will rebuild all Docker images and restart services.
set /p confirm="Continue? (y/n): "
if /i not "%confirm%"=="y" exit /b 0

echo [INFO] Building Java artifacts...
call gradlew.bat clean build -x test

echo [INFO] Rebuilding Docker images...
docker compose down
docker compose build --no-cache

echo [INFO] Starting services...
docker compose up -d
goto show_status

:show_status
echo.
echo ============================================
echo Services Status
echo ============================================
timeout /t 5 /nobreak >nul
docker compose ps
echo.
echo ============================================
echo Health Checks
echo ============================================
echo.
echo API Gateway:         http://localhost:8080/actuator/health
echo Auth Service:        http://localhost:8087/actuator/health
echo User Service:        http://localhost:8081/actuator/health
echo Product Service:     http://localhost:8084/actuator/health
echo Order Service:       http://localhost:8083/actuator/health
echo Inventory Service:   http://localhost:8085/actuator/health
echo Notification Service:http://localhost:8086/actuator/health
echo.
echo Kafka UI:            http://localhost:8090
echo Schema Registry:     http://localhost:8082
echo Vault (if enabled):  http://localhost:8200
echo.
echo ============================================
echo View Logs
echo ============================================
echo To view logs:
echo   docker compose logs -f [service-name]
echo.
echo Example:
echo   docker compose logs -f auth-service
echo.
pause

