package org.example;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.WorkerExecutor;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.example.controller.UserController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
    
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private static final int HTTP_PORT = 8080;
    private static final String WORKER_POOL_NAME = "worker-pool";
    private static final int WORKER_POOL_SIZE = 10;
    private static final long WORKER_MAX_EXECUTE_TIME = 60000; // 60 seconds
    
    public static void main(String[] args) {
        VertxOptions options = new VertxOptions();
        Vertx vertx = Vertx.vertx(options);
        
        // Create worker executor
        WorkerExecutor workerExecutor = vertx.createSharedWorkerExecutor(
            WORKER_POOL_NAME, 
            WORKER_POOL_SIZE, 
            WORKER_MAX_EXECUTE_TIME
        );
        
        // Setup HTTP server
        setupHttpServer(vertx, workerExecutor);
        
        // Graceful shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down application...");
            workerExecutor.close();
            vertx.close();
        }));
    }
    
    private static void setupHttpServer(Vertx vertx, WorkerExecutor workerExecutor) {
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
        
        // Create HTTP server
        vertx.createHttpServer()
             .requestHandler(router)
             .listen(HTTP_PORT, result -> {
                 if (result.succeeded()) {
                     logger.info("HTTP server started on port {}", HTTP_PORT);
                     logger.info("Worker pool '{}' created with {} threads", WORKER_POOL_NAME, WORKER_POOL_SIZE);
                 } else {
                     logger.error("Failed to start HTTP server", result.cause());
                     System.exit(1);
                 }
             });
    }
}
