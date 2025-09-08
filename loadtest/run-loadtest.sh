#!/bin/bash

# Script to run K6 load tests against the Vert.x application

# Check if k6 is installed
if ! command -v k6 &> /dev/null; then
    echo "k6 is not installed. Please install it first:"
    echo "  macOS: brew install k6"
    echo "  Ubuntu: sudo gpg -k && sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69"
    echo "  Or visit: https://k6.io/docs/get-started/installation/"
    exit 1
fi

# Check if application is running
echo "Checking if Vert.x application is running..."
if ! curl -f -s http://localhost:8080/health > /dev/null; then
    echo "❌ Application is not running on http://localhost:8080"
    echo "Please start the application first:"
    echo "  ./run-with-monitoring.sh"
    exit 1
fi

echo "✅ Application is running"
echo ""

# Run the load test
echo "Starting K6 load test..."
echo "Test will run for approximately 4 minutes with varying load"
echo ""

k6 run loadtest/users-api-test.js

echo ""
echo "Load test completed!"
echo "Check the results above for performance metrics and any failures."