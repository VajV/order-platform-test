#!/usr/bin/env pwsh
# =============================================================================
# Order Lifecycle Test Script
# Демонстрирует полный жизненный цикл заказа: NEW → RESERVED → PAID
# =============================================================================

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$AuthUrl = "http://localhost:8087",
    [string]$Username = "testuser",
    [string]$Password = "Test123!",
    [string]$AdminUsername = "admin",
    [string]$AdminPassword = "Admin123!"
)

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Order Lifecycle Demo" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# 1. Login as user
Write-Host "[1/4] Logging in as $Username..." -ForegroundColor Yellow
try {
    $loginResult = Invoke-RestMethod -Uri "$AuthUrl/api/auth/login" `
        -Method POST -ContentType "application/json" `
        -Body (@{username=$Username; password=$Password} | ConvertTo-Json)
    $userToken = $loginResult.accessToken
    Write-Host "      OK - User logged in" -ForegroundColor Green
} catch {
    Write-Host "      FAIL - $_" -ForegroundColor Red
    exit 1
}

# 2. Login as admin (for demo-lifecycle endpoint)
Write-Host "[2/4] Logging in as admin..." -ForegroundColor Yellow
try {
    $adminLogin = Invoke-RestMethod -Uri "$AuthUrl/api/auth/login" `
        -Method POST -ContentType "application/json" `
        -Body (@{username=$AdminUsername; password=$AdminPassword} | ConvertTo-Json)
    $adminToken = $adminLogin.accessToken
    Write-Host "      OK - Admin logged in" -ForegroundColor Green
} catch {
    Write-Host "      FAIL - $_" -ForegroundColor Red
    exit 1
}

# 3. Create Order
Write-Host "[3/4] Creating order..." -ForegroundColor Yellow
$orderBody = @{
    items = @(@{
        productId = "DEMO-PROD-001"
        productName = "Demo Product"
        quantity = 2
        unitPrice = 99.99
    })
} | ConvertTo-Json -Depth 3

try {
    $order = Invoke-RestMethod -Uri "$BaseUrl/api/orders" `
        -Method POST -ContentType "application/json" `
        -Headers @{Authorization = "Bearer $userToken"} `
        -Body $orderBody
    $orderId = $order.id
    Write-Host "      OK - Order #$orderId created (Status: $($order.status))" -ForegroundColor Green
} catch {
    Write-Host "      FAIL - $_" -ForegroundColor Red
    exit 1
}

# 4. Trigger demo lifecycle (NEW → RESERVED → PAID)
Write-Host "[4/4] Running demo lifecycle (NEW -> RESERVED -> PAID)..." -ForegroundColor Yellow
try {
    $result = Invoke-RestMethod -Uri "$BaseUrl/api/orders/$orderId/demo-lifecycle" `
        -Method POST `
        -Headers @{Authorization = "Bearer $adminToken"}
    Write-Host "      OK - Lifecycle completed" -ForegroundColor Green
} catch {
    Write-Host "      FAIL - $_" -ForegroundColor Red
    exit 1
}

# Final result
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Order #$orderId - RESULT" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Status:     $($result.status)" -ForegroundColor Green
Write-Host "  Total:      $($result.totalPrice)" -ForegroundColor White
Write-Host "  PaymentId:  $($result.paymentId)" -ForegroundColor White
Write-Host "========================================`n" -ForegroundColor Cyan

if ($result.status -eq "PAID") {
    Write-Host "SUCCESS: Full lifecycle completed (NEW -> RESERVED -> PAID)" -ForegroundColor Green
} else {
    Write-Host "UNEXPECTED: Status is $($result.status)" -ForegroundColor Yellow
}
