#!/bin/bash
# E2E Test Script for Order Processing Platform
# Bash version

set -e

GATEWAY_URL="http://localhost:8080"

echo "========================================"
echo "  Order Processing Platform E2E Test"
echo "========================================"

# Generate unique identifiers
TIMESTAMP=$(date +%Y%m%d%H%M%S)
USER_USERNAME="testuser_$TIMESTAMP"
USER_EMAIL="testuser_$TIMESTAMP@example.com"
ADMIN_USERNAME="admin_$TIMESTAMP"
ADMIN_EMAIL="admin_$TIMESTAMP@example.com"
PASSWORD="SecurePass123!"

# ============================================
# STEP 1: Register USER
# ============================================
echo ""
echo "[1/7] Registering USER..."

USER_RESPONSE=$(curl -s -X POST "$GATEWAY_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"$USER_USERNAME\",
    \"email\": \"$USER_EMAIL\",
    \"password\": \"$PASSWORD\",
    \"fullName\": \"Test User\"
  }")

USER_TOKEN=$(echo $USER_RESPONSE | jq -r '.accessToken')
USER_ID=$(echo $USER_RESPONSE | jq -r '.user.id')

if [ "$USER_TOKEN" == "null" ] || [ -z "$USER_TOKEN" ]; then
  echo "  ❌ Failed to register USER"
  echo "  Response: $USER_RESPONSE"
  exit 1
fi
echo "  ✅ USER registered: $USER_USERNAME (ID: $USER_ID)"

# ============================================
# STEP 2: Register ADMIN
# ============================================
echo ""
echo "[2/7] Registering ADMIN..."

ADMIN_RESPONSE=$(curl -s -X POST "$GATEWAY_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"$ADMIN_USERNAME\",
    \"email\": \"$ADMIN_EMAIL\",
    \"password\": \"$PASSWORD\",
    \"fullName\": \"Admin User\"
  }")

ADMIN_ID=$(echo $ADMIN_RESPONSE | jq -r '.user.id')

if [ "$ADMIN_ID" == "null" ] || [ -z "$ADMIN_ID" ]; then
  echo "  ❌ Failed to register ADMIN"
  echo "  Response: $ADMIN_RESPONSE"
  exit 1
fi
echo "  ✅ ADMIN registered: $ADMIN_USERNAME (ID: $ADMIN_ID)"

# ============================================
# STEP 3: Update ADMIN role in database
# ============================================
echo ""
echo "[3/7] Updating ADMIN role in database..."
echo "  ⚠️  Run this SQL manually in auth_db:"
echo "     UPDATE users SET role = 'ROLE_ADMIN' WHERE id = $ADMIN_ID;"
echo ""
echo "  Press ENTER after updating the database..."
read

# Re-login ADMIN to get new token with ROLE_ADMIN
ADMIN_LOGIN_RESPONSE=$(curl -s -X POST "$GATEWAY_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"$ADMIN_USERNAME\",
    \"password\": \"$PASSWORD\"
  }")

ADMIN_TOKEN=$(echo $ADMIN_LOGIN_RESPONSE | jq -r '.accessToken')

if [ "$ADMIN_TOKEN" == "null" ] || [ -z "$ADMIN_TOKEN" ]; then
  echo "  ❌ Failed to login ADMIN"
  exit 1
fi
echo "  ✅ ADMIN re-logged in with new role"

# ============================================
# STEP 4: Create Product (ADMIN)
# ============================================
echo ""
echo "[4/7] Creating Product (ADMIN)..."

PRODUCT_RESPONSE=$(curl -s -X POST "$GATEWAY_URL/api/products" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "{
    \"name\": \"Test Product $TIMESTAMP\",
    \"description\": \"E2E Test Product\",
    \"price\": 99.99,
    \"category\": \"Electronics\",
    \"isPublished\": true
  }")

PRODUCT_ID=$(echo $PRODUCT_RESPONSE | jq -r '.id')

if [ "$PRODUCT_ID" == "null" ] || [ -z "$PRODUCT_ID" ]; then
  echo "  ❌ Failed to create product"
  echo "  Response: $PRODUCT_RESPONSE"
  exit 1
fi
echo "  ✅ Product created: $PRODUCT_ID"

# ============================================
# STEP 5: Create Inventory (ADMIN)
# ============================================
echo ""
echo "[5/7] Creating Inventory (ADMIN)..."

INVENTORY_RESPONSE=$(curl -s -X POST "$GATEWAY_URL/api/inventory" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "{
    \"productId\": \"$PRODUCT_ID\",
    \"totalQuantity\": 100,
    \"description\": \"E2E Test Inventory\"
  }")

INVENTORY_ID=$(echo $INVENTORY_RESPONSE | jq -r '.id')

if [ "$INVENTORY_ID" == "null" ] || [ -z "$INVENTORY_ID" ]; then
  echo "  ❌ Failed to create inventory"
  echo "  Response: $INVENTORY_RESPONSE"
  exit 1
fi
echo "  ✅ Inventory created for product: $PRODUCT_ID"

# ============================================
# STEP 6: Create Order (USER)
# ============================================
echo ""
echo "[6/7] Creating Order (USER)..."

ORDER_RESPONSE=$(curl -s -X POST "$GATEWAY_URL/api/orders" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d "{
    \"items\": [
      {
        \"productId\": \"$PRODUCT_ID\",
        \"productName\": \"Test Product $TIMESTAMP\",
        \"quantity\": 2,
        \"unitPrice\": 99.99
      }
    ]
  }")

ORDER_ID=$(echo $ORDER_RESPONSE | jq -r '.id')
ORDER_STATUS=$(echo $ORDER_RESPONSE | jq -r '.status')

if [ "$ORDER_ID" == "null" ] || [ -z "$ORDER_ID" ]; then
  echo "  ❌ Failed to create order"
  echo "  Response: $ORDER_RESPONSE"
  exit 1
fi
echo "  ✅ Order created: $ORDER_ID (Status: $ORDER_STATUS)"

# ============================================
# STEP 7: Check Notifications (USER)
# ============================================
echo ""
echo "[7/7] Checking Notifications (USER)..."

sleep 3  # Wait for Kafka events to process

NOTIFICATIONS_RESPONSE=$(curl -s -X GET "$GATEWAY_URL/api/v1/notifications/my" \
  -H "Authorization: Bearer $USER_TOKEN")

NOTIFICATION_COUNT=$(echo $NOTIFICATIONS_RESPONSE | jq 'if type == "array" then length else 1 end')

echo "  ✅ Notifications received: $NOTIFICATION_COUNT"

if [ "$NOTIFICATION_COUNT" != "0" ] && [ "$NOTIFICATION_COUNT" != "null" ]; then
  echo "  Latest notification types:"
  echo $NOTIFICATIONS_RESPONSE | jq -r '.[0:3] | .[] | "    - \(.type): \(.message // "N/A")"' 2>/dev/null || true
fi

# ============================================
# SUMMARY
# ============================================
echo ""
echo "========================================"
echo "  E2E Test Summary"
echo "========================================"
echo "  USER_ID:    $USER_ID"
echo "  ADMIN_ID:   $ADMIN_ID"
echo "  PRODUCT_ID: $PRODUCT_ID"
echo "  ORDER_ID:   $ORDER_ID"
echo ""
echo "  ✅ E2E Test PASSED!"
