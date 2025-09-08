# Load Testing and Performance Monitoring Guide

## Overview

This guide explains how to perform load testing on the Vert.x application using K6 and monitor performance using VisualVM.

## Prerequisites

### Install K6
```bash
# macOS
brew install k6

# Ubuntu/Debian
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6

# Windows
choco install k6
```

### Install VisualVM
Download from: https://visualvm.github.io/download.html

## Running the Application with Monitoring

1. **Start the application with monitoring enabled:**
   ```bash
   ./run-with-monitoring.sh
   ```

   This script starts the application with:
   - JMX remote monitoring on port 9999
   - Flight Recorder enabled
   - G1 garbage collector optimizations
   - 2GB max heap size

2. **Connect VisualVM:**
   - Open VisualVM
   - Go to "Applications" → "Add JMX Connection"
   - Enter: `localhost:9999`
   - Click "OK"

## Load Testing with K6

### Quick Start
```bash
# Run the complete load test suite
./loadtest/run-loadtest.sh
```

### Manual K6 Execution
```bash
# Run the load test directly
k6 run loadtest/users-api-test.js
```

## Load Test Scenarios

The K6 test includes multiple scenarios with different weights:

| Scenario | Weight | Description |
|----------|---------|-------------|
| GET /api/users | 40% | Fetch all users |
| GET /api/users/:id | 30% | Fetch user by ID |
| POST /api/users | 15% | Create new user |
| PUT /api/users/:id | 10% | Update existing user |
| DELETE /api/users/:id | 5% | Delete user |

### Load Pattern
- **Ramp up:** 30s to 10 users
- **Sustain:** 1m at 50 users  
- **Peak:** 30s ramp to 100 users
- **Stress:** 2m at 100 users
- **Ramp down:** 30s to 0 users

## Performance Thresholds

The test enforces these performance requirements:
- 95% of requests must complete under 500ms
- Error rate must be below 5%
- Custom error rate must be below 10%

## Monitoring with VisualVM

### Key Metrics to Watch

1. **Threads:**
   - Event Loop threads (should be low, typically 2x CPU cores)
   - Worker pool threads (configured as 10 in the app)
   - GC threads

2. **Memory:**
   - Heap usage and GC frequency
   - Non-heap memory (metaspace, code cache)
   - Memory leaks over time

3. **CPU:**
   - Overall CPU utilization
   - GC CPU overhead
   - Thread CPU usage

4. **GC Performance:**
   - GC frequency and duration
   - Heap allocation rate
   - Memory pool usage

### Expected Thread Behavior

With Vert.x and WorkerExecutor:
- **Event Loop threads:** 2-8 threads (CPU cores × 2)
- **Worker threads:** 10 threads (as configured)
- **Netty threads:** Additional threads for I/O operations

## Performance Tuning Tips

### JVM Tuning
```bash
# Current JVM options in run-with-monitoring.sh
-XX:+UseG1GC                    # Use G1 garbage collector
-XX:MaxGCPauseMillis=200       # Target max GC pause time
-Xmx2g -Xms1g                  # Heap size: 1-2GB
-XX:+UseStringDeduplication    # Reduce memory for duplicate strings
```

### Application Tuning
- Monitor worker thread pool usage
- Adjust worker pool size based on blocking operations
- Consider using `vertx.executeBlocking()` for I/O operations
- Use `executeBlockingWithWorker()` for CPU-intensive tasks

## Interpreting Results

### K6 Output Metrics
- **http_req_duration:** Response times (p50, p95, p99)
- **http_req_failed:** Error rate percentage
- **http_reqs:** Total requests per second
- **vus:** Virtual users (concurrent connections)

### VisualVM Analysis
- **High CPU + Low thread count:** Good (event loop efficiency)
- **Many blocked threads:** Consider increasing worker pool
- **Frequent GC:** Tune heap size or GC algorithm
- **Memory growth:** Check for memory leaks

## Troubleshooting

### Common Issues

1. **High Response Times:**
   - Check worker thread pool utilization
   - Monitor database connection pool
   - Verify GC pause times

2. **Memory Issues:**
   - Increase heap size: `-Xmx4g`
   - Enable heap dumps: `-XX:+HeapDumpOnOutOfMemoryError`
   - Use memory profiler in VisualVM

3. **Connection Issues:**
   - Verify JMX port is not blocked: `netstat -an | grep 9999`
   - Check firewall settings
   - Ensure hostname resolution works

### Performance Baseline

Expected performance for this application:
- **Throughput:** 1000+ req/sec for GET operations
- **Response time:** p95 < 100ms for simple operations
- **Memory:** Stable heap usage under load
- **Threads:** Minimal worker thread blocking

## Advanced Monitoring

### Flight Recorder Analysis
```bash
# Analyze the generated JFR file
jcmd <pid> JFR.dump filename=snapshot.jfr
```

### Custom Metrics
Add application metrics using Micrometer or Vert.x metrics:
```java
// In your controller
Timer.Sample sample = Timer.start(meterRegistry);
// ... operation ...
sample.stop(Timer.builder("api.users.get").register(meterRegistry));
```

## Load Test Variations

### Stress Test
```javascript
// Modify options in users-api-test.js
export const options = {
  stages: [
    { duration: '2m', target: 200 },  // Stress test
    { duration: '5m', target: 200 },
    { duration: '2m', target: 0 },
  ],
};
```

### Spike Test
```javascript
export const options = {
  stages: [
    { duration: '10s', target: 100 }, // Fast ramp
    { duration: '30s', target: 100 },
    { duration: '10s', target: 0 },
  ],
};
```