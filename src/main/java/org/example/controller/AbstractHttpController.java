package org.example.controller;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractHttpController {
    
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final Vertx vertx;
    protected final WorkerExecutor workerExecutor;
    protected final ObjectMapper objectMapper;
    
    public AbstractHttpController(Vertx vertx, WorkerExecutor workerExecutor) {
        this.vertx = vertx;
        this.workerExecutor = workerExecutor;
        this.objectMapper = new ObjectMapper();
    }
    
    protected <T> Future<T> executeBlocking(Handler<Promise<T>> blockingCodeHandler) {
        return vertx.executeBlocking(blockingCodeHandler, false);
    }
    
    protected <T> Future<T> executeBlockingWithWorker(Handler<Promise<T>> blockingCodeHandler) {
        return workerExecutor.executeBlocking(blockingCodeHandler, false);
    }
    
    protected void sendJsonResponse(RoutingContext context, Object data) {
        sendJsonResponse(context, data, 200);
    }
    
    protected void sendJsonResponse(RoutingContext context, Object data, int statusCode) {
        try {
            HttpServerResponse response = context.response();
            response.setStatusCode(statusCode)
                   .putHeader("Content-Type", "application/json");
            
            if (data != null) {
                String json = objectMapper.writeValueAsString(data);
                response.end(json);
            } else {
                response.end();
            }
        } catch (Exception e) {
            logger.error("Error sending JSON response", e);
            sendErrorResponse(context, "Internal server error", 500);
        }
    }
    
    protected void sendErrorResponse(RoutingContext context, String message, int statusCode) {
        try {
            JsonObject error = new JsonObject()
                .put("error", true)
                .put("message", message)
                .put("statusCode", statusCode);
            
            context.response()
                   .setStatusCode(statusCode)
                   .putHeader("Content-Type", "application/json")
                   .end(error.encode());
        } catch (Exception e) {
            logger.error("Error sending error response", e);
            context.response().setStatusCode(500).end("Internal server error");
        }
    }
    
    protected JsonObject getRequestBody(RoutingContext context) {
        try {
            return context.getBodyAsJson();
        } catch (Exception e) {
            logger.warn("Invalid JSON in request body", e);
            return null;
        }
    }
    
    protected <T> T parseRequestBody(RoutingContext context, Class<T> clazz) {
        try {
            String body = context.getBodyAsString();
            return objectMapper.readValue(body, clazz);
        } catch (Exception e) {
            logger.warn("Error parsing request body to {}", clazz.getSimpleName(), e);
            return null;
        }
    }
    
    protected void handleAsync(RoutingContext context, Handler<Promise<Object>> asyncHandler) {
        executeBlocking(promise -> {
            try {
                asyncHandler.handle(promise);
            } catch (Exception e) {
                logger.error("Error in async handler", e);
                promise.fail(e);
            }
        }).onSuccess(result -> {
            // Success handled by the async handler itself
        }).onFailure(throwable -> {
            logger.error("Async operation failed", throwable);
            if (!context.response().ended()) {
                sendErrorResponse(context, "Operation failed", 500);
            }
        });
    }
    
    protected void handleAsyncWithWorker(RoutingContext context, Handler<Promise<Object>> asyncHandler) {
        executeBlockingWithWorker(promise -> {
            try {
                asyncHandler.handle(promise);
            } catch (Exception e) {
                logger.error("Error in worker async handler", e);
                promise.fail(e);
            }
        }).onSuccess(result -> {
            // Success handled by the async handler itself
        }).onFailure(throwable -> {
            logger.error("Worker async operation failed", throwable);
            if (!context.response().ended()) {
                sendErrorResponse(context, "Operation failed", 500);
            }
        });
    }
    
    public abstract void setupRoutes(io.vertx.ext.web.Router router);
}