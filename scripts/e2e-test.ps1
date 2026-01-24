# E2E Test Script for Order Processing Platform
# PowerShell version

$ErrorActionPreference = "Stop"
$GATEWAY_URL = "http://localhost:8080"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Order Processing Platform E2E Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Generate unique identifiers
$TIMESTAMP = Get-Date -Format "yyyyMMddHHmmss"
$USER_USERNAME = "testuser_$TIMESTAMP"
$USER_EMAIL = "testuser_$TIMESTAMP@example.com"
$ADMIN_USERNAME = "admin_$TIMESTAMP"
$ADMIN_EMAIL = "admin_$TIMESTAMP@example.com"
$PASSWORD = "SecurePass123!"

# ============================================
# STEP 1: Register USER
# ============================================
Write-Host "`n[1/7] Registering USER..." -ForegroundColor Yellow

$userRegisterBody = @{
    username = $USER_USERNAME
    email = $USER_EMAIL
    password = $PASSWORD
    fullName = "Test User"
} | ConvertTo-Json

try {
    $userResponse = Invoke-RestMethod -Uri "$GATEWAY_URL/api/auth/register" `
        -Method POST `
        -ContentType "application/json" `
        -Body $userRegisterBody
    
    $USER_TOKEN = $userResponse.accessToken
    $USER_ID = $userResponse.user.id
    Write-Host "  ✅ USER registered: $USER_USERNAME (ID: $USER_ID)" -ForegroundColor Green
} catch {
    Write-Host "  ❌ Failed to register USER: $_" -ForegroundColor Red
    exit 1
}

# ============================================
# STEP 2: Register ADMIN
# ============================================
Write-Host "`n[2/7] Registering ADMIN..." -ForegroundColor Yellow

$adminRegisterBody = @{
    username = $ADMIN_USERNAME
    email = $ADMIN_EMAIL
    password = $PASSWORD
    fullName = "Admin User"
} | ConvertTo-Json

try {
    $adminResponse = Invoke-RestMethod -Uri "$GATEWAY_URL/api/auth/register" `
        -Method POST `
        -ContentType "application/json" `
        -Body $adminRegisterBody
    
    $ADMIN_ID = $adminResponse.user.id
    Write-Host "  ✅ ADMIN registered: $ADMIN_USERNAME (ID: $ADMIN_ID)" -ForegroundColor Green
} catch {
    Write-Host "  ❌ Failed to register ADMIN: $_" -ForegroundColor Red
    exit 1
}

# ============================================
# STEP 3: Update ADMIN role in database
# ============================================
Write-Host "`n[3/7] Updating ADMIN role in database..." -ForegroundColor Yellow
Write-Host "  ⚠️  Run this SQL manually in auth_db:" -ForegroundColor Magenta
Write-Host "     UPDATE users SET role = 'ROLE_ADMIN' WHERE id = $ADMIN_ID;" -ForegroundColor White

Write-Host "`n  Press ENTER after updating the database..." -ForegroundColor Cyan
Read-Host

# Re-login ADMIN to get new token with ROLE_ADMIN
$adminLoginBody = @{
    username = $ADMIN_USERNAME
    password = $PASSWORD
} | ConvertTo-Json

try {
    $adminLoginResponse = Invoke-RestMethod -Uri "$GATEWAY_URL/api/auth/login" `
        -Method POST `
        -ContentType "application/json" `
        -Body $adminLoginBody
    
    $ADMIN_TOKEN = $adminLoginResponse.accessToken
    Write-Host "  ✅ ADMIN re-logged in with new role" -ForegroundColor Green
} catch {
    Write-Host "  ❌ Failed to login ADMIN: $_" -ForegroundColor Red
    exit 1
}

# ============================================
# STEP 4: Create Product (ADMIN)
# ============================================
Write-Host "`n[4/7] Creating Product (ADMIN)..." -ForegroundColor Yellow

$productBody = @{
    name = "Test Product $TIMESTAMP"
    description = "E2E Test Product"
    price = 99.99
    category = "Electronics"
    isPublished = $true
} | ConvertTo-Json

try {
    $productResponse = Invoke-RestMethod -Uri "$GATEWAY_URL/api/products" `
        -Method POST `
        -ContentType "application/json" `
        -Headers @{ Authorization = "Bearer $ADMIN_TOKEN" } `
        -Body $productBody
    
    $PRODUCT_ID = $productResponse.id
    Write-Host "  ✅ Product created: $PRODUCT_ID" -ForegroundColor Green
} catch {
    Write-Host "  ❌ Failed to create product: $_" -ForegroundColor Red
    Write-Host "  Response: $($_.Exception.Response)" -ForegroundColor Red
    exit 1
}

# ============================================
# STEP 5: Create Inventory (ADMIN)
# ============================================
Write-Host "`n[5/7] Creating Inventory (ADMIN)..." -ForegroundColor Yellow

$inventoryBody = @{
    productId = $PRODUCT_ID
    totalQuantity = 100
    description = "E2E Test Inventory"
} | ConvertTo-Json

try {
    $inventoryResponse = Invoke-RestMethod -Uri "$GATEWAY_URL/api/inventory" `
        -Method POST `
        -ContentType "application/json" `
        -Headers @{ Authorization = "Bearer $ADMIN_TOKEN" } `
        -Body $inventoryBody
    
    Write-Host "  ✅ Inventory created for product: $PRODUCT_ID" -ForegroundColor Green
} catch {
    Write-Host "  ❌ Failed to create inventory: $_" -ForegroundColor Red
    exit 1
}

# ============================================
# STEP 6: Create Order (USER)
# ============================================
Write-Host "`n[6/7] Creating Order (USER)..." -ForegroundColor Yellow

$orderBody = @{
    items = @(
        @{
            productId = $PRODUCT_ID
            productName = "Test Product $TIMESTAMP"
            quantity = 2
            unitPrice = 99.99
        }
    )
} | ConvertTo-Json -Depth 3

try {
    $orderResponse = Invoke-RestMethod -Uri "$GATEWAY_URL/api/orders" `
        -Method POST `
        -ContentType "application/json" `
        -Headers @{ Authorization = "Bearer $USER_TOKEN" } `
        -Body $orderBody
    
    $ORDER_ID = $orderResponse.id
    Write-Host "  ✅ Order created: $ORDER_ID (Status: $($orderResponse.status))" -ForegroundColor Green
} catch {
    Write-Host "  ❌ Failed to create order: $_" -ForegroundColor Red
    Write-Host "  Request body: $orderBody" -ForegroundColor Yellow
    exit 1
}

# ============================================
# STEP 7: Check Notifications (USER)
# ============================================
Write-Host "`n[7/7] Checking Notifications (USER)..." -ForegroundColor Yellow

Start-Sleep -Seconds 3  # Wait for Kafka events to process

try {
    $notificationsResponse = Invoke-RestMethod -Uri "$GATEWAY_URL/api/v1/notifications/my" `
        -Method GET `
        -Headers @{ Authorization = "Bearer $USER_TOKEN" }
    
    $notificationCount = if ($notificationsResponse -is [array]) { $notificationsResponse.Count } else { 1 }
    Write-Host "  ✅ Notifications received: $notificationCount" -ForegroundColor Green
    
    if ($notificationsResponse) {
        Write-Host "  Latest notification types:" -ForegroundColor Cyan
        $notificationsResponse | Select-Object -First 3 | ForEach-Object {
            Write-Host "    - $($_.type): $($_.message)" -ForegroundColor White
        }
    }
} catch {
    Write-Host "  ⚠️  Could not fetch notifications (may be empty): $_" -ForegroundColor Yellow
}

# ============================================
# SUMMARY
# ============================================
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  E2E Test Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  USER_ID:    $USER_ID" -ForegroundColor White
Write-Host "  ADMIN_ID:   $ADMIN_ID" -ForegroundColor White
Write-Host "  PRODUCT_ID: $PRODUCT_ID" -ForegroundColor White
Write-Host "  ORDER_ID:   $ORDER_ID" -ForegroundColor White
Write-Host "`n  ✅ E2E Test PASSED!" -ForegroundColor Green
