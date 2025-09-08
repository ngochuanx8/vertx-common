package org.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.WorkerExecutor;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.example.controller.UserController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServerVerticle extends AbstractVerticle {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpServerVerticle.class);
    private static final int HTTP_PORT = 8080;
    private static final String WORKER_POOL_NAME = "worker-pool-hung-verticle";
    private static final int WORKER_POOL_SIZE = 15;
    private static final long WORKER_MAX_EXECUTE_TIME = 60000; // 60 seconds
    
    private WorkerExecutor workerExecutor;
    
    @Override
    public void start(Promise<Void> startPromise) {
        String threadName = Thread.currentThread().getName();
        String verticleId = deploymentID();
        
        logger.info("Starting HttpServerVerticle {} on thread: {}", verticleId, threadName);
        
        // Create worker executor for this verticle
        workerExecutor = vertx.createSharedWorkerExecutor(
            WORKER_POOL_NAME + "-" + verticleId, 
            WORKER_POOL_SIZE, 
            WORKER_MAX_EXECUTE_TIME
        );
        
        // Setup router
        Router router = Router.router(vertx);
        
        // Global handlers
        router.route().handler(BodyHandler.create());
        
        // CORS handler (optional)
        router.route().handler(ctx -> {
            ctx.response()
               .putHeader("Access-Control-Allow-Origin", "*")
               .putHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
               .putHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
            ctx.next();
        });
        
        // Setup controllers
        UserController userController = new UserController(vertx, workerExecutor);
        userController.setupRoutes(router);
        
        // Health check endpoint
        router.get("/health").handler(ctx -> {
            ctx.response()
               .putHeader("Content-Type", "application/json")
               .end("{\"status\":\"UP\",\"timestamp\":\"" + System.currentTimeMillis() + "\"}");
        });
        
        // Thread info endpoint to see which event loop thread is handling requests
        router.get("/thread-info").handler(ctx -> {
            String currentThread = Thread.currentThread().getName();
            String response = String.format(
                "{\"eventLoopThread\":\"%s\",\"verticleId\":\"%s\",\"timestamp\":%d}", 
                currentThread,
                verticleId,
                System.currentTimeMillis()
            );
            ctx.response()
               .putHeader("Content-Type", "application/json")
               .end(response);
        });
        
        // Verticle info endpoint
        router.get("/verticle-info").handler(ctx -> {
            String response = String.format(
                "{\"verticleId\":\"%s\",\"eventLoopThread\":\"%s\",\"workerPool\":\"%s\",\"timestamp\":%d}",
                verticleId,
                threadName,
                WORKER_POOL_NAME + "-" + verticleId,
                System.currentTimeMillis()
            );
            ctx.response()
               .putHeader("Content-Type", "application/json")
               .end(response);
        });
        
        // Thread monitoring endpoint - shows system-wide thread information
        router.get("/thread-stats").handler(ctx -> {
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
            ctx.response()
               .putHeader("Content-Type", "application/json")
               .end(response);
        });
        
        // Create HTTP server (Vert.x will handle the sharing automatically)
        vertx.createHttpServer()
             .requestHandler(router)
             .listen(HTTP_PORT, result -> {
                 if (result.succeeded()) {
                     logger.info("HTTP server verticle {} started on port {} (thread: {})", 
                               verticleId, HTTP_PORT, threadName);
                     startPromise.complete();
                 } else {
                     logger.error("Failed to start HTTP server verticle {} on thread {}", 
                               verticleId, threadName, result.cause());
                     startPromise.fail(result.cause());
                 }
             });
    }
    
    @Override
    public void stop(Promise<Void> stopPromise) {
        String verticleId = deploymentID();
        logger.info("Stopping HttpServerVerticle {}", verticleId);
        
        if (workerExecutor != null) {
            workerExecutor.close();
        }
        
        stopPromise.complete();
    }
}