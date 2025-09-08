#!/bin/bash

echo "=== Diagnosing Event Loop Thread Issues ==="
echo ""

# Check 1: OS Support for SO_REUSEPORT
echo "1. Checking OS support for SO_REUSEPORT..."
if [[ "$OSTYPE" == "darwin"* ]]; then
    echo "   ✅ macOS: SO_REUSEPORT supported (since macOS 10.10)"
    sysctl -a | grep -i reuseport 2>/dev/null || echo "   ℹ️  No specific sysctl for SO_REUSEPORT (normal on macOS)"
elif [[ "$OSTYPE" == "linux"* ]]; then
    echo "   ✅ Linux: SO_REUSEPORT supported (kernel 3.9+)"
    echo "   Kernel version: $(uname -r)"
    if [ -f /proc/sys/net/core/reuseport ]; then
        echo "   SO_REUSEPORT kernel support: $(cat /proc/sys/net/core/reuseport)"
    fi
else
    echo "   ⚠️  Unknown OS: $OSTYPE - SO_REUSEPORT support uncertain"
fi
echo ""

# Check 2: Current application status
echo "2. Checking if application is running..."
if curl -f -s http://localhost:8080/health > /dev/null; then
    echo "   ✅ Application is running on port 8080"
    
    # Check how many processes are listening on port 8080
    echo ""
    echo "3. Checking listening processes on port 8080..."
    if command -v lsof > /dev/null; then
        echo "   Processes listening on port 8080:"
        lsof -i :8080 | head -10
    elif command -v netstat > /dev/null; then
        echo "   Processes listening on port 8080:"
        netstat -tlnp | grep :8080
    else
        echo "   ⚠️  Neither lsof nor netstat available"
    fi
    
    echo ""
    echo "4. Testing actual thread distribution with concurrent requests..."
    
    # Create temporary results file
    TEMP_FILE=$(mktemp)
    
    # Launch 20 concurrent requests
    for i in {1..20}; do
        (curl -s --connect-timeout 2 --max-time 5 --no-keepalive http://localhost:8080/thread-info 2>/dev/null | jq -r '.eventLoopThread' 2>/dev/null) >> "$TEMP_FILE" &
    done
    
    # Wait for all requests to complete
    wait
    
    echo "   Results from 20 concurrent requests:"
    if [ -s "$TEMP_FILE" ]; then
        sort "$TEMP_FILE" | uniq -c | while read count thread; do
            echo "   $count requests -> $thread"
        done
        
        unique_threads=$(sort "$TEMP_FILE" | uniq | wc -l)
        echo ""
        echo "   Summary: $unique_threads unique event loop thread(s) used"
        
        if [ "$unique_threads" -eq 1 ]; then
            echo "   ❌ Only using 1 event loop thread"
        else
            echo "   ✅ Using $unique_threads event loop threads"
        fi
    else
        echo "   ❌ No successful responses received"
    fi
    
    # Clean up
    rm -f "$TEMP_FILE"
    
else
    echo "   ❌ Application is not running on port 8080"
    echo "   Please start with: ./run-with-monitoring.sh"
    exit 1
fi

echo ""
echo "5. Checking Java/JVM version..."
if command -v java > /dev/null; then
    java -version 2>&1 | head -3
else
    echo "   ❌ Java not found"
fi

echo ""
echo "6. Recommended next steps..."
if [ "$unique_threads" -eq 1 ]; then
    echo "   Since only 1 thread is being used:"
    echo "   a) This might be normal for light load (curl requests)"
    echo "   b) Run the K6 load test to see if multiple threads activate under load:"
    echo "      ./loadtest/run-loadtest.sh"
    echo "   c) Check VisualVM during load test for multiple active event loop threads"
    echo "   d) Consider using Netty's native transport (epoll on Linux, kqueue on macOS)"
else
    echo "   ✅ Multiple threads are working correctly!"
    echo "   Run load test to see full utilization: ./loadtest/run-loadtest.sh"
fi

echo ""
echo "7. Additional debugging commands:"
echo "   # Watch threads in real-time during load test:"
echo "   watch -n 1 'curl -s http://localhost:8080/thread-info | jq -r .eventLoopThread'"
echo ""
echo "   # Monitor with jstack during load:"
echo "   jstack \$(jps | grep App | cut -d' ' -f1)"