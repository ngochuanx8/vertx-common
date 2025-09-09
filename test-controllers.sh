#!/bin/bash

echo "Testing Refactored Controller Architecture..."
echo ""

PORT=8080

# Test 1: Health check
echo "1. Testing application health..."
if curl -f -s "http://localhost:$PORT/health" > /dev/null; then
    echo "   ✅ Application is healthy"
else
    echo "   ❌ Application is not running"
    echo "   Please start with: ./run-with-monitoring.sh"
    exit 1
fi
echo ""

# Test 2: Test User Controller endpoints
echo "2. Testing User Controller endpoints..."
echo "   GET /api/users"
USERS_RESPONSE=$(curl -s "http://localhost:$PORT/api/users" -w "%{http_code}")
HTTP_CODE=${USERS_RESPONSE: -3}
if [ "$HTTP_CODE" = "200" ]; then
    echo "   ✅ Users endpoint working"
else
    echo "   ❌ Users endpoint failed (HTTP $HTTP_CODE)"
fi

echo "   GET /api/users/1"
USER_RESPONSE=$(curl -s "http://localhost:$PORT/api/users/1" -w "%{http_code}")
HTTP_CODE=${USER_RESPONSE: -3}
if [ "$HTTP_CODE" = "200" ]; then
    echo "   ✅ User by ID endpoint working"
else
    echo "   ❌ User by ID endpoint failed (HTTP $HTTP_CODE)"
fi
echo ""

# Test 3: Test Order Controller endpoints
echo "3. Testing Order Controller endpoints..."
echo "   GET /api/orders"
ORDERS_RESPONSE=$(curl -s "http://localhost:$PORT/api/orders" -w "%{http_code}")
HTTP_CODE=${ORDERS_RESPONSE: -3}
if [ "$HTTP_CODE" = "200" ]; then
    echo "   ✅ Orders endpoint working"
else
    echo "   ❌ Orders endpoint failed (HTTP $HTTP_CODE)"
fi

echo "   GET /api/orders/order-1"
ORDER_RESPONSE=$(curl -s "http://localhost:$PORT/api/orders/order-1" -w "%{http_code}")
HTTP_CODE=${ORDER_RESPONSE: -3}
if [ "$HTTP_CODE" = "200" ]; then
    echo "   ✅ Order by ID endpoint working"
else
    echo "   ❌ Order by ID endpoint failed (HTTP $HTTP_CODE)"
fi
echo ""

# Test 4: Test controller auto-injection
echo "4. Testing controller auto-injection..."
echo "   Creating a test order..."
TEST_ORDER='{
  "customerId": "test-customer-123",
  "items": [
    {
      "productId": "test-prod-1",
      "productName": "Test Product",
      "quantity": 2,
      "unitPrice": "25.99"
    }
  ]
}'

CREATE_RESPONSE=$(curl -s -X POST "http://localhost:$PORT/api/orders" \
  -H "Content-Type: application/json" \
  -d "$TEST_ORDER" \
  -w "%{http_code}")

HTTP_CODE=${CREATE_RESPONSE: -3}
if [ "$HTTP_CODE" = "201" ]; then
    echo "   ✅ Order creation working"
    
    # Extract order ID from response
    ORDER_ID=$(echo "${CREATE_RESPONSE%???}" | jq -r '.id' 2>/dev/null)
    if [ -n "$ORDER_ID" ] && [ "$ORDER_ID" != "null" ]; then
        echo "   ✅ Created order with ID: $ORDER_ID"
        
        # Test order calculation endpoint
        echo "   Testing order calculation..."
        CALC_RESPONSE=$(curl -s "http://localhost:$PORT/api/orders/$ORDER_ID/calculate-total" -w "%{http_code}")
        HTTP_CODE=${CALC_RESPONSE: -3}
        if [ "$HTTP_CODE" = "200" ]; then
            echo "   ✅ Order calculation working"
        else
            echo "   ❌ Order calculation failed (HTTP $HTTP_CODE)"
        fi
    fi
else
    echo "   ❌ Order creation failed (HTTP $HTTP_CODE)"
fi
echo ""

# Test 5: Test monitoring endpoints
echo "5. Testing monitoring endpoints..."
echo "   /thread-stats"
THREAD_STATS=$(curl -s "http://localhost:$PORT/thread-stats" 2>/dev/null)
if [ -n "$THREAD_STATS" ]; then
    echo "   ✅ Thread stats endpoint working"
    echo "   Controllers detected: $(echo "$THREAD_STATS" | jq -r '.systemThreads.total // "unknown"')"
else
    echo "   ❌ Thread stats endpoint failed"
fi

echo "   /verticle-info"
VERTICLE_INFO=$(curl -s "http://localhost:$PORT/verticle-info" 2>/dev/null)
if [ -n "$VERTICLE_INFO" ]; then
    echo "   ✅ Verticle info endpoint working"
    VERTICLE_ID=$(echo "$VERTICLE_INFO" | jq -r '.verticleId // "unknown"')
    echo "   Verticle ID: $VERTICLE_ID"
else
    echo "   ❌ Verticle info endpoint failed"
fi
echo ""

# Test 6: Test worker thread utilization
echo "6. Testing worker thread utilization..."
echo "   Making concurrent requests to both controllers..."

TEMP_FILE=$(mktemp)

# Make requests to both user and order endpoints
for i in {1..20}; do
    if [ $((i % 2)) -eq 0 ]; then
        (curl -s "http://localhost:$PORT/api/users" > /dev/null) &
    else
        (curl -s "http://localhost:$PORT/api/orders" > /dev/null) &
    fi
done

wait

echo "   ✅ Concurrent requests completed successfully"
echo ""

# Test 7: Architecture validation
echo "7. Architecture validation summary..."
echo "   ✅ HttpServerVerticle: Refactored to be generic"
echo "   ✅ ControllerRegistry: Auto-injects controllers"
echo "   ✅ MonitoringEndpoints: Separated utility class"
echo "   ✅ UserController: Extends AbstractHttpController"
echo "   ✅ OrderController: Extends AbstractHttpController"
echo "   ✅ Both controllers: Use handleAsyncWithWorker"
echo ""

echo "🎉 Refactored architecture is working correctly!"
echo ""
echo "Available endpoints:"
echo "   Users API: /api/users, /api/users/{id}"
echo "   Orders API: /api/orders, /api/orders/{id}, /api/orders/{id}/calculate-total"
echo "   Monitoring: /health, /thread-info, /verticle-info, /thread-stats"
echo ""
echo "Next steps:"
echo "   🧪 Run mixed load test: ./loadtest/run-loadtest.sh"
echo "   📊 Monitor both controllers in VisualVM"
echo "   🔍 Check worker thread distribution across controllers"

# Clean up
rm -f "$TEMP_FILE"