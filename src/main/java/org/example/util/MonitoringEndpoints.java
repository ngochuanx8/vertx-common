package org.example.util;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MonitoringEndpoints {
    
    private static final Logger logger = LoggerFactory.getLogger(MonitoringEndpoints.class);
    private final String verticleId;
    
    public MonitoringEndpoints(String verticleId) {
        this.verticleId = verticleId;
    }
    
    public void setupRoutes(Router router) {
        router.get("/health").handler(this::healthCheck);
        router.get("/thread-info").handler(this::threadInfo);
        router.get("/verticle-info").handler(this::verticleInfo);
        router.get("/thread-stats").handler(this::threadStats);
    }
    
    private void healthCheck(RoutingContext context) {
        context.response()
               .putHeader("Content-Type", "application/json")
               .end("{\"status\":\"UP\",\"timestamp\":\"" + System.currentTimeMillis() + "\"}");
    }
    
    private void threadInfo(RoutingContext context) {
        String threadName = Thread.currentThread().getName();
        String response = String.format(
            "{\"eventLoopThread\":\"%s\",\"timestamp\":%d}", 
            threadName, 
            System.currentTimeMillis()
        );
        context.response()
               .putHeader("Content-Type", "application/json")
               .end(response);
    }
    
    private void verticleInfo(RoutingContext context) {
        String threadName = Thread.currentThread().getName();
        String response = String.format(
            "{\"verticleId\":\"%s\",\"eventLoopThread\":\"%s\",\"timestamp\":%d}",
            verticleId,
            threadName,
            System.currentTimeMillis()
        );
        context.response()
               .putHeader("Content-Type", "application/json")
               .end(response);
    }
    
    private void threadStats(RoutingContext context) {
        // Get current runtime information
        Runtime runtime = Runtime.getRuntime();
        ThreadGroup rootThreadGroup = Thread.currentThread().getThreadGroup();
        while (rootThreadGroup.getParent() != null) {
            rootThreadGroup = rootThreadGroup.getParent();
        }
        
        int activeThreads = rootThreadGroup.activeCount();
        Thread[] threads = new Thread[activeThreads * 2]; // Buffer for safety
        int threadCount = rootThreadGroup.enumerate(threads, true);
        
        // Count different types of threads
        int eventLoopThreadCount = 0;
        int workerThreadCount = 0;
        int otherThreadCount = 0;
        
        for (int i = 0; i < threadCount; i++) {
            if (threads[i] != null) {
                String threadNameStr = threads[i].getName();
                if (threadNameStr.startsWith("vert.x-eventloop-thread-")) {
                    eventLoopThreadCount++;
                } else if (threadNameStr.contains("worker")) {
                    workerThreadCount++;
                } else {
                    otherThreadCount++;
                }
            }
        }
        
        String response = String.format(
            "{\"systemThreads\":{\"total\":%d,\"eventLoopThreads\":%d,\"workerThreads\":%d,\"otherThreads\":%d},\"runtime\":{\"availableProcessors\":%d,\"maxMemory\":%d,\"totalMemory\":%d,\"freeMemory\":%d},\"currentVerticle\":{\"verticleId\":\"%s\",\"eventLoopThread\":\"%s\"},\"timestamp\":%d}",
            threadCount,
            eventLoopThreadCount,
            workerThreadCount,
            otherThreadCount,
            runtime.availableProcessors(),
            runtime.maxMemory(),
            runtime.totalMemory(),
            runtime.freeMemory(),
            verticleId,
            Thread.currentThread().getName(),
            System.currentTimeMillis()
        );
        context.response()
               .putHeader("Content-Type", "application/json")
               .end(response);
    }
}