package org.example;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
    
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    
    public static void main(String[] args) {
        // Dynamic CPU detection and thread calculation
        int availableCpus = Runtime.getRuntime().availableProcessors();
        int eventLoopThreads = availableCpus * 2; // Vert.x best practice: 2x CPU cores
        int verticleInstances = eventLoopThreads;  // Match verticles to event loops for full utilization
        int workerPoolSize = verticleInstances * 15; // 15 workers per verticle
        int internalBlockingPool = Math.max(20, availableCpus * 2);
        
        logger.info("=== Thread Configuration Analysis ===");
        logger.info("Available CPU cores: {}", availableCpus);
        logger.info("Event loop threads: {} (2x CPU cores)", eventLoopThreads);
        logger.info("Verticle instances: {} (1 per event loop thread)", verticleInstances);
        logger.info("Worker pool size: {} ({} workers per verticle)", workerPoolSize, 15);
        logger.info("Internal blocking pool: {}", internalBlockingPool);
        logger.info("=========================================");
        
        VertxOptions options = new VertxOptions()
            .setEventLoopPoolSize(eventLoopThreads)
            .setWorkerPoolSize(workerPoolSize)
            .setInternalBlockingPoolSize(internalBlockingPool);
        
        Vertx vertx = Vertx.vertx(options);
        
        // Deploy multiple instances of the HTTP server verticle
        // Vert.x will automatically distribute them across event loop threads
        // and handle request sharing using round-robin
        io.vertx.core.DeploymentOptions deploymentOptions = new io.vertx.core.DeploymentOptions()
            .setInstances(verticleInstances);
        
        vertx.deployVerticle("org.example.HttpServerVerticle", deploymentOptions, result -> {
            if (result.succeeded()) {
                logger.info("Successfully deployed {} HttpServerVerticle instances", verticleInstances);
                logger.info("Deployment ID: {}", result.result());
                logger.info("Application ready! Each verticle runs on a different event loop thread.");
                logger.info("Vert.x handles request sharing automatically using round-robin strategy.");
                logger.info("Access the application at: http://localhost:8080");
            } else {
                logger.error("Failed to deploy HttpServerVerticle instances", result.cause());
                System.exit(1);
            }
        });
        
        // Graceful shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down application...");
            vertx.close();
        }));
    }
}
