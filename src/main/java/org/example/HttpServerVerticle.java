package org.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.WorkerExecutor;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.example.controller.UserController;
import org.example.controller.OrderController;
import org.example.util.ControllerRegistry;
import org.example.util.MonitoringEndpoints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServerVerticle extends AbstractVerticle {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpServerVerticle.class);
    private static final int HTTP_PORT = 8080;
    private static final String WORKER_POOL_NAME = "worker-pool-verticle";
    private static final int WORKER_POOL_SIZE = 15;
    private static final long WORKER_MAX_EXECUTE_TIME = 60000; // 60 seconds
    
    private WorkerExecutor workerExecutor;
    private ControllerRegistry controllerRegistry;
    
    @Override
    public void start(Promise<Void> startPromise) {
        String threadName = Thread.currentThread().getName();
        String verticleId = deploymentID();
        
        logger.info("Starting HttpServerVerticle {} on thread: {}", verticleId, threadName);
        
        try {
            // Create worker executor for this verticle
            workerExecutor = vertx.createSharedWorkerExecutor(
                WORKER_POOL_NAME + "-" + verticleId, 
                WORKER_POOL_SIZE, 
                WORKER_MAX_EXECUTE_TIME
            );
            
            // Setup router
            Router router = Router.router(vertx);
            
            // Global middleware
            setupGlobalHandlers(router);
            
            // Auto-inject and setup controllers
            setupControllers(router);
            
            // Setup monitoring endpoints
            setupMonitoringEndpoints(router, verticleId);
            
            // Start HTTP server
            startHttpServer(router, verticleId, threadName, startPromise);
            
        } catch (Exception e) {
            logger.error("Failed to start HttpServerVerticle {}", verticleId, e);
            startPromise.fail(e);
        }
    }
    
    private void setupGlobalHandlers(Router router) {
        // Body handler for parsing request bodies
        router.route().handler(BodyHandler.create());
        
        // CORS handler
        router.route().handler(ctx -> {
            ctx.response()
               .putHeader("Access-Control-Allow-Origin", "*")
               .putHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
               .putHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
            ctx.next();
        });
        
        // Request logging middleware
        router.route().handler(ctx -> {
            String method = ctx.request().method().toString();
            String uri = ctx.request().uri();
            String thread = Thread.currentThread().getName();
            logger.debug("Request: {} {} (thread: {})", method, uri, thread);
            ctx.next();
        });
    }
    
    private void setupControllers(Router router) {
        // Initialize controller registry and auto-inject controllers
        controllerRegistry = new ControllerRegistry();
        controllerRegistry.registerControllers(vertx, workerExecutor);
        
        // Register controllers directly
        controllerRegistry.addController(new UserController(vertx, workerExecutor));
        controllerRegistry.addController(new OrderController(vertx, workerExecutor));
        
        controllerRegistry.setupRoutes(router);
        
        logger.info("Auto-injected {} controllers", controllerRegistry.getControllers().size());
    }
    
    private void setupMonitoringEndpoints(Router router, String verticleId) {
        MonitoringEndpoints monitoring = new MonitoringEndpoints(verticleId);
        monitoring.setupRoutes(router);
        logger.info("Monitoring endpoints configured");
    }
    
    private void startHttpServer(Router router, String verticleId, String threadName, Promise<Void> startPromise) {
        vertx.createHttpServer()
             .requestHandler(router)
             .listen(HTTP_PORT, result -> {
                 if (result.succeeded()) {
                     logger.info("HTTP server verticle {} started on port {} (thread: {})", 
                               verticleId, HTTP_PORT, threadName);
                     logger.info("Controllers: {}", 
                               controllerRegistry.getControllers().stream()
                                   .map(c -> c.getClass().getSimpleName())
                                   .toArray());
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
            logger.info("Worker executor closed for verticle {}", verticleId);
        }
        
        stopPromise.complete();
    }
    
    // Getter for accessing controller registry (useful for testing)
    public ControllerRegistry getControllerRegistry() {
        return controllerRegistry;
    }
}