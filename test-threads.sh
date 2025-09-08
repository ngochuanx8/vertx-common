#!/bin/bash

echo "Testing Vert.x Dynamic Thread Configuration and Utilization..."
echo ""

PORT=8080

# Test 1: Check if server is running
echo "1. Checking server health..."
if curl -f -s "http://localhost:$PORT/health" > /dev/null; then
    echo "   ✅ Server is running on port $PORT"
else
    echo "   ❌ Server is NOT running on port $PORT"
    echo "   Please start with: ./run-with-monitoring.sh"
    exit 1
fi
echo ""

# Test 2: Get system thread configuration
echo "2. Analyzing system thread configuration..."
THREAD_STATS=$(curl -s "http://localhost:$PORT/thread-stats" 2>/dev/null)
if [ -n "$THREAD_STATS" ]; then
    echo "   System Thread Analysis:"
    echo "$THREAD_STATS" | jq -r '
    "   CPU Cores: " + (.runtime.availableProcessors | tostring) +
    "\n   Event Loop Threads: " + (.systemThreads.eventLoopThreads | tostring) +
    "\n   Worker Threads: " + (.systemThreads.workerThreads | tostring) +
    "\n   Total Active Threads: " + (.systemThreads.total | tostring) +
    "\n   Memory Usage: " + ((.runtime.totalMemory - .runtime.freeMemory) / 1024 / 1024 | floor | tostring) + "MB / " + (.runtime.totalMemory / 1024 / 1024 | floor | tostring) + "MB"
    ' 2>/dev/null || echo "   ⚠️  Could not parse thread statistics"
else
    echo "   ⚠️  Could not retrieve thread statistics"
fi
echo ""

# Test 3: Test verticle distribution
echo "3. Testing verticle and thread distribution..."
echo "   Making requests to see different verticles and event loop threads..."

TEMP_FILE=$(mktemp)

# Make concurrent requests to see thread/verticle distribution
for i in {1..50}; do
  (curl -s --no-keepalive "http://localhost:$PORT/verticle-info" 2>/dev/null | jq -r '"\(.eventLoopThread)|\(.verticleId)"' 2>/dev/null) >> "$TEMP_FILE" &
done

wait

echo ""
echo "   Results from 50 concurrent requests:"
if [ -s "$TEMP_FILE" ]; then
  # Show thread distribution
  echo "   Event Loop Thread Distribution:"
  cut -d'|' -f1 "$TEMP_FILE" | sort | uniq -c | while read count thread; do
    echo "   $count requests -> $thread"
  done
  
  echo ""
  echo "   Verticle Distribution:"
  cut -d'|' -f2 "$TEMP_FILE" | sort | uniq -c | while read count verticle; do
    echo "   $count requests -> $verticle"
  done
  
  unique_threads=$(cut -d'|' -f1 "$TEMP_FILE" | sort | uniq | wc -l)
  unique_verticles=$(cut -d'|' -f2 "$TEMP_FILE" | sort | uniq | wc -l)
  
  echo ""
  echo "   Summary:"
  echo "   - $unique_threads unique event loop threads used"
  echo "   - $unique_verticles unique verticles handled requests"
  
  if [ "$unique_threads" -gt 1 ]; then
    echo "   ✅ SUCCESS: Multiple event loop threads are working!"
    if [ "$unique_verticles" -gt 1 ]; then
      echo "   ✅ EXCELLENT: Multiple verticles are distributing load!"
      echo "   🚀 Vert.x round-robin request sharing is working perfectly!"
    fi
  else
    echo "   ⚠️  Only using 1 event loop thread (this may be normal under light load)"
  fi
else
  echo "   ❌ No successful responses received"
fi

# Clean up
rm -f "$TEMP_FILE"

echo ""
echo "4. Thread utilization verification..."
echo "   Checking if all configured event loop threads are being utilized..."

# Get expected thread count from configuration
EXPECTED_THREADS=$(curl -s "http://localhost:$PORT/thread-stats" 2>/dev/null | jq -r '.runtime.availableProcessors * 2' 2>/dev/null)
if [ -z "$EXPECTED_THREADS" ] || [ "$EXPECTED_THREADS" = "null" ]; then
    EXPECTED_THREADS=8  # Default fallback
fi

echo "   Expected event loop threads (2x CPU cores): $EXPECTED_THREADS"

# Make many concurrent requests to activate all threads
TEMP_FILE3=$(mktemp)
for i in $(seq 1 $((EXPECTED_THREADS * 10))); do
  (curl -s --no-keepalive "http://localhost:$PORT/thread-info" 2>/dev/null | jq -r '.eventLoopThread' 2>/dev/null) >> "$TEMP_FILE3" &
  
  # Batch requests to avoid overwhelming
  if (( i % 20 == 0 )); then
    wait
  fi
done
wait

if [ -s "$TEMP_FILE3" ]; then
  echo "   Thread utilization results:"
  sort "$TEMP_FILE3" | uniq -c | while read count thread; do
    echo "   $count requests -> $thread"
  done
  
  actual_threads=$(sort "$TEMP_FILE3" | uniq | wc -l)
  utilization_percent=$(( (actual_threads * 100) / EXPECTED_THREADS ))
  
  echo ""
  echo "   Thread Utilization Analysis:"
  echo "   - Expected threads: $EXPECTED_THREADS"
  echo "   - Active threads: $actual_threads"
  echo "   - Utilization: $utilization_percent%"
  
  if [ "$actual_threads" -ge "$EXPECTED_THREADS" ]; then
    echo "   ✅ EXCELLENT: All configured event loop threads are active!"
    echo "   🚀 100% thread utilization achieved!"
  elif [ "$actual_threads" -ge $((EXPECTED_THREADS * 3 / 4)) ]; then
    echo "   ✅ GOOD: Most event loop threads are active ($utilization_percent%)"
    echo "   💡 Some threads may activate under higher load"
  else
    echo "   ⚠️  LOW: Only $utilization_percent% of threads utilized"
    echo "   💡 Try running the K6 load test for higher concurrency"
  fi
fi

rm -f "$TEMP_FILE3"

echo ""
echo "5. High-load simulation test..."
echo "   Making 200 requests with high concurrency to trigger multiple threads..."

TEMP_FILE2=$(mktemp)

# High concurrency test
for i in {1..200}; do
  (curl -s --connect-timeout 1 --max-time 2 "http://localhost:$PORT/thread-info" 2>/dev/null | jq -r '.eventLoopThread' 2>/dev/null) >> "$TEMP_FILE2" &
  
  # Limit concurrent connections to avoid overwhelming the system
  if (( i % 50 == 0 )); then
    wait
  fi
done

wait

echo ""
echo "   Results from 200 high-concurrency requests:"
if [ -s "$TEMP_FILE2" ]; then
  sort "$TEMP_FILE2" | uniq -c | while read count thread; do
    echo "   $count requests -> $thread"
  done
  
  unique_threads_hc=$(sort "$TEMP_FILE2" | uniq | wc -l)
  total_requests=$(wc -l < "$TEMP_FILE2")
  echo ""
  echo "   High-concurrency summary: $unique_threads_hc threads handled $total_requests requests"
  
  if [ "$unique_threads_hc" -gt 1 ]; then
    echo "   ✅ OUTSTANDING: Multiple event loop threads activated under load!"
    echo "   🚀 System is ready for high-performance load testing!"
  else
    echo "   ℹ️  Single thread handling all requests"
    echo "   💡 This may be normal - try the K6 load test for true high load"
  fi
fi

# Clean up
rm -f "$TEMP_FILE2"

echo ""
echo "6. Configuration Summary:"
echo "   📋 Check application startup logs for:"
echo "   - Thread Configuration Analysis (CPU cores, event loops, workers)"
echo "   - Verticle deployment details"
echo "   - Each verticle assigned to different event loop threads"
echo ""
echo "7. Verticle Deployment Information:"
echo "   Check application logs for verticle deployment details"
echo "   Each verticle should be deployed on different event loop threads"
echo ""

echo "Next steps:"
echo "   🧪 Run comprehensive load test: ./loadtest/run-loadtest.sh"
echo "   📊 Monitor in VisualVM during load test"
echo "   👀 Watch for multiple active event loop threads under real load"
echo "   📈 Observe worker thread utilization for API processing"