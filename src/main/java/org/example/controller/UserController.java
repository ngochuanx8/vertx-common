package org.example.controller;

import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.example.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class UserController extends AbstractHttpController {
    
    private final ConcurrentHashMap<String, User> userStore = new ConcurrentHashMap<>();
    
    public UserController(Vertx vertx, WorkerExecutor workerExecutor) {
        super(vertx, workerExecutor);
        // Initialize with some sample data
        initSampleData();
    }
    
    private void initSampleData() {
        userStore.put("1", new User("1", "John Doe", "john@example.com"));
        userStore.put("2", new User("2", "Jane Smith", "jane@example.com"));
    }
    
    @Override
    public void setupRoutes(Router router) {
        router.get("/api/users").handler(this::getAllUsers);
        router.get("/api/users/:id").handler(this::getUserById);
        router.post("/api/users").handler(this::createUser);
        router.put("/api/users/:id").handler(this::updateUser);
        router.delete("/api/users/:id").handler(this::deleteUser);
        router.get("/api/users/:id/heavy-operation").handler(this::performHeavyOperation);
    }
    
    private void getAllUsers(RoutingContext context) {
        handleAsyncWithWorker(context, promise -> {
            try {
                logger.info("Fetching all users");
                // Simulate database operation
                Thread.sleep(100);
                
                List<User> users = new ArrayList<>(userStore.values());
                sendJsonResponse(context, users);
                promise.complete();
                
            } catch (Exception e) {
                logger.error("Error fetching users", e);
                promise.fail(e);
            }
        });
    }
    
    private void getUserById(RoutingContext context) {
        String userId = context.pathParam("id");
        
        handleAsyncWithWorker(context, promise -> {
            try {
                logger.info("Fetching user with ID: {}", userId);
                // Simulate database lookup
                Thread.sleep(50);
                
                User user = userStore.get(userId);
                if (user != null) {
                    sendJsonResponse(context, user);
                } else {
                    sendErrorResponse(context, "User not found", 404);
                }
                promise.complete();
                
            } catch (Exception e) {
                logger.error("Error fetching user {}", userId, e);
                promise.fail(e);
            }
        });
    }
    
    private void createUser(RoutingContext context) {
        User newUser = parseRequestBody(context, User.class);
        
        if (newUser == null || newUser.getName() == null || newUser.getEmail() == null) {
            sendErrorResponse(context, "Invalid user data", 400);
            return;
        }
        
        handleAsyncWithWorker(context, promise -> {
            try {
                logger.info("Creating new user: {}", newUser.getName());
                // Simulate database save operation
                Thread.sleep(200);
                
                String id = String.valueOf(System.currentTimeMillis());
                newUser.setId(id);
                userStore.put(id, newUser);
                
                sendJsonResponse(context, newUser, 201);
                promise.complete();
                
            } catch (Exception e) {
                logger.error("Error creating user", e);
                promise.fail(e);
            }
        });
    }
    
    private void updateUser(RoutingContext context) {
        String userId = context.pathParam("id");
        User updatedUser = parseRequestBody(context, User.class);
        
        if (updatedUser == null) {
            sendErrorResponse(context, "Invalid user data", 400);
            return;
        }
        
        handleAsyncWithWorker(context, promise -> {
            try {
                logger.info("Updating user with ID: {}", userId);
                // Simulate database update operation
                Thread.sleep(150);
                
                User existingUser = userStore.get(userId);
                if (existingUser == null) {
                    sendErrorResponse(context, "User not found", 404);
                } else {
                    updatedUser.setId(userId);
                    userStore.put(userId, updatedUser);
                    sendJsonResponse(context, updatedUser);
                }
                promise.complete();
                
            } catch (Exception e) {
                logger.error("Error updating user {}", userId, e);
                promise.fail(e);
            }
        });
    }
    
    private void deleteUser(RoutingContext context) {
        String userId = context.pathParam("id");
        
        handleAsyncWithWorker(context, promise -> {
            try {
                logger.info("Deleting user with ID: {}", userId);
                // Simulate database delete operation
                Thread.sleep(100);
                
                User deletedUser = userStore.remove(userId);
                if (deletedUser != null) {
                    sendJsonResponse(context, new JsonObject().put("message", "User deleted successfully"));
                } else {
                    sendErrorResponse(context, "User not found", 404);
                }
                promise.complete();
                
            } catch (Exception e) {
                logger.error("Error deleting user {}", userId, e);
                promise.fail(e);
            }
        });
    }
    
    private void performHeavyOperation(RoutingContext context) {
        String userId = context.pathParam("id");
        
        // Use worker executor for CPU-intensive operations
        handleAsyncWithWorker(context, promise -> {
            try {
                logger.info("Performing heavy operation for user: {}", userId);
                
                User user = userStore.get(userId);
                if (user == null) {
                    sendErrorResponse(context, "User not found", 404);
                    promise.complete();
                    return;
                }
                
                // Simulate heavy CPU-bound operation
                int result = performComplexCalculation();
                
                JsonObject response = new JsonObject()
                    .put("userId", userId)
                    .put("userName", user.getName())
                    .put("calculationResult", result)
                    .put("processingTime", "Heavy operation completed");
                
                sendJsonResponse(context, response);
                promise.complete();
                
            } catch (Exception e) {
                logger.error("Error in heavy operation for user {}", userId, e);
                promise.fail(e);
            }
        });
    }
    
    private int performComplexCalculation() {
        // Simulate CPU-intensive work
        int result = 0;
        for (int i = 0; i < 1000000; i++) {
            result += ThreadLocalRandom.current().nextInt(1000);
        }
        return result % 10000;
    }
}