package org.example.util;

import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.ext.web.Router;
import org.example.controller.AbstractHttpController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ControllerRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(ControllerRegistry.class);
    private final List<AbstractHttpController> controllers = new ArrayList<>();
    
    public void registerControllers(Vertx vertx, WorkerExecutor workerExecutor) {
        logger.info("Registering HTTP controllers...");
        
        // Controllers are now registered externally
        
        logger.info("Registered {} controllers", controllers.size());
    }
    
    public void addController(AbstractHttpController controller) {
        controllers.add(controller);
    }
    
    public void setupRoutes(Router router) {
        logger.info("Setting up routes for {} controllers", controllers.size());
        
        for (AbstractHttpController controller : controllers) {
            try {
                controller.setupRoutes(router);
                logger.info("Routes configured for: {}", controller.getClass().getSimpleName());
            } catch (Exception e) {
                logger.error("Failed to setup routes for controller: {}", 
                           controller.getClass().getSimpleName(), e);
            }
        }
        
        logger.info("All controller routes configured successfully");
    }
    
    public List<AbstractHttpController> getControllers() {
        return new ArrayList<>(controllers);
    }
    
    public <T extends AbstractHttpController> T getController(Class<T> controllerClass) {
        for (AbstractHttpController controller : controllers) {
            if (controllerClass.isInstance(controller)) {
                return controllerClass.cast(controller);
            }
        }
        return null;
    }
}