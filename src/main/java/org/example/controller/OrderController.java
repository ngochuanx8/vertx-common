package org.example.controller;

import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.example.model.Order;
import org.example.model.Order.OrderItem;
import org.example.model.Order.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class OrderController extends AbstractHttpController {
    
    private final ConcurrentHashMap<String, Order> orderStore = new ConcurrentHashMap<>();
    
    public OrderController(Vertx vertx, WorkerExecutor workerExecutor) {
        super(vertx, workerExecutor);
        // Initialize with some sample data
        initSampleData();
    }
    
    private void initSampleData() {
        // Sample order 1
        List<OrderItem> items1 = Arrays.asList(
            new OrderItem("prod-1", "Laptop", 1, new BigDecimal("999.99")),
            new OrderItem("prod-2", "Mouse", 2, new BigDecimal("29.99"))
        );
        BigDecimal total1 = items1.stream()
            .map(OrderItem::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        Order order1 = new Order("order-1", "customer-1", items1, total1, OrderStatus.CONFIRMED);
        orderStore.put("order-1", order1);
        
        // Sample order 2
        List<OrderItem> items2 = Arrays.asList(
            new OrderItem("prod-3", "Keyboard", 1, new BigDecimal("79.99")),
            new OrderItem("prod-4", "Monitor", 1, new BigDecimal("299.99"))
        );
        BigDecimal total2 = items2.stream()
            .map(OrderItem::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        Order order2 = new Order("order-2", "customer-2", items2, total2, OrderStatus.PROCESSING);
        orderStore.put("order-2", order2);
    }
    
    @Override
    public void setupRoutes(Router router) {
        router.get("/api/orders").handler(this::getAllOrders);
        router.get("/api/orders/:id").handler(this::getOrderById);
        router.post("/api/orders").handler(this::createOrder);
        router.put("/api/orders/:id").handler(this::updateOrder);
        router.delete("/api/orders/:id").handler(this::deleteOrder);
        router.put("/api/orders/:id/status").handler(this::updateOrderStatus);
        router.get("/api/orders/:id/calculate-total").handler(this::calculateOrderTotal);
    }
    
    private void getAllOrders(RoutingContext context) {
        handleAsyncWithWorker(context, promise -> {
            try {
                logger.info("Fetching all orders");
                // Simulate database operation
                Thread.sleep(150);
                
                List<Order> orders = new ArrayList<>(orderStore.values());
                sendJsonResponse(context, orders);
                promise.complete();
                
            } catch (Exception e) {
                logger.error("Error fetching orders", e);
                promise.fail(e);
            }
        });
    }
    
    private void getOrderById(RoutingContext context) {
        String orderId = context.pathParam("id");
        
        handleAsyncWithWorker(context, promise -> {
            try {
                logger.info("Fetching order with ID: {}", orderId);
                // Simulate database lookup
                Thread.sleep(75);
                
                Order order = orderStore.get(orderId);
                if (order != null) {
                    sendJsonResponse(context, order);
                } else {
                    sendErrorResponse(context, "Order not found", 404);
                }
                promise.complete();
                
            } catch (Exception e) {
                logger.error("Error fetching order {}", orderId, e);
                promise.fail(e);
            }
        });
    }
    
    private void createOrder(RoutingContext context) {
        Order newOrder = parseRequestBody(context, Order.class);
        
        if (newOrder == null || newOrder.getCustomerId() == null || newOrder.getItems() == null || newOrder.getItems().isEmpty()) {
            sendErrorResponse(context, "Invalid order data", 400);
            return;
        }
        
        handleAsyncWithWorker(context, promise -> {
            try {
                logger.info("Creating new order for customer: {}", newOrder.getCustomerId());
                // Simulate order processing
                Thread.sleep(300);
                
                // Generate order ID
                String orderId = "order-" + System.currentTimeMillis();
                newOrder.setId(orderId);
                
                // Calculate total amount
                BigDecimal totalAmount = newOrder.getItems().stream()
                    .map(OrderItem::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                newOrder.setTotalAmount(totalAmount);
                
                // Set initial status
                newOrder.setStatus(OrderStatus.PENDING);
                newOrder.setCreatedAt(LocalDateTime.now());
                newOrder.setUpdatedAt(LocalDateTime.now());
                
                orderStore.put(orderId, newOrder);
                
                sendJsonResponse(context, newOrder, 201);
                promise.complete();
                
            } catch (Exception e) {
                logger.error("Error creating order", e);
                promise.fail(e);
            }
        });
    }
    
    private void updateOrder(RoutingContext context) {
        String orderId = context.pathParam("id");
        Order updatedOrder = parseRequestBody(context, Order.class);
        
        if (updatedOrder == null) {
            sendErrorResponse(context, "Invalid order data", 400);
            return;
        }
        
        handleAsyncWithWorker(context, promise -> {
            try {
                logger.info("Updating order with ID: {}", orderId);
                // Simulate database update operation
                Thread.sleep(200);
                
                Order existingOrder = orderStore.get(orderId);
                if (existingOrder == null) {
                    sendErrorResponse(context, "Order not found", 404);
                } else {
                    updatedOrder.setId(orderId);
                    updatedOrder.setCreatedAt(existingOrder.getCreatedAt());
                    updatedOrder.setUpdatedAt(LocalDateTime.now());
                    
                    // Recalculate total if items changed
                    if (updatedOrder.getItems() != null && !updatedOrder.getItems().isEmpty()) {
                        BigDecimal totalAmount = updatedOrder.getItems().stream()
                            .map(OrderItem::getTotalPrice)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                        updatedOrder.setTotalAmount(totalAmount);
                    }
                    
                    orderStore.put(orderId, updatedOrder);
                    sendJsonResponse(context, updatedOrder);
                }
                promise.complete();
                
            } catch (Exception e) {
                logger.error("Error updating order {}", orderId, e);
                promise.fail(e);
            }
        });
    }
    
    private void deleteOrder(RoutingContext context) {
        String orderId = context.pathParam("id");
        
        handleAsyncWithWorker(context, promise -> {
            try {
                logger.info("Deleting order with ID: {}", orderId);
                // Simulate database delete operation
                Thread.sleep(120);
                
                Order deletedOrder = orderStore.remove(orderId);
                if (deletedOrder != null) {
                    sendJsonResponse(context, new JsonObject().put("message", "Order deleted successfully"));
                } else {
                    sendErrorResponse(context, "Order not found", 404);
                }
                promise.complete();
                
            } catch (Exception e) {
                logger.error("Error deleting order {}", orderId, e);
                promise.fail(e);
            }
        });
    }
    
    private void updateOrderStatus(RoutingContext context) {
        String orderId = context.pathParam("id");
        JsonObject statusUpdate = getRequestBody(context);
        
        if (statusUpdate == null || !statusUpdate.containsKey("status")) {
            sendErrorResponse(context, "Status is required", 400);
            return;
        }
        
        handleAsyncWithWorker(context, promise -> {
            try {
                logger.info("Updating status for order: {}", orderId);
                // Simulate status update operation
                Thread.sleep(100);
                
                Order order = orderStore.get(orderId);
                if (order == null) {
                    sendErrorResponse(context, "Order not found", 404);
                } else {
                    try {
                        OrderStatus newStatus = OrderStatus.valueOf(statusUpdate.getString("status").toUpperCase());
                        order.setStatus(newStatus);
                        order.setUpdatedAt(LocalDateTime.now());
                        
                        orderStore.put(orderId, order);
                        sendJsonResponse(context, order);
                    } catch (IllegalArgumentException e) {
                        sendErrorResponse(context, "Invalid status value", 400);
                    }
                }
                promise.complete();
                
            } catch (Exception e) {
                logger.error("Error updating order status {}", orderId, e);
                promise.fail(e);
            }
        });
    }
    
    private void calculateOrderTotal(RoutingContext context) {
        String orderId = context.pathParam("id");
        
        // Use worker executor for calculation-heavy operations
        handleAsyncWithWorker(context, promise -> {
            try {
                logger.info("Calculating total for order: {}", orderId);
                
                Order order = orderStore.get(orderId);
                if (order == null) {
                    sendErrorResponse(context, "Order not found", 404);
                    promise.complete();
                    return;
                }
                
                // Simulate complex calculation with tax, shipping, discounts
                Thread.sleep(500); // Simulate heavy calculation
                
                BigDecimal subtotal = order.getItems().stream()
                    .map(OrderItem::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                BigDecimal taxRate = new BigDecimal("0.08"); // 8% tax
                BigDecimal tax = subtotal.multiply(taxRate);
                
                BigDecimal shipping = subtotal.compareTo(new BigDecimal("100")) >= 0 
                    ? BigDecimal.ZERO 
                    : new BigDecimal("9.99");
                
                BigDecimal discount = subtotal.compareTo(new BigDecimal("500")) >= 0 
                    ? subtotal.multiply(new BigDecimal("0.05")) 
                    : BigDecimal.ZERO;
                
                BigDecimal finalTotal = subtotal.add(tax).add(shipping).subtract(discount);
                
                JsonObject calculation = new JsonObject()
                    .put("orderId", orderId)
                    .put("subtotal", subtotal)
                    .put("tax", tax)
                    .put("shipping", shipping)
                    .put("discount", discount)
                    .put("finalTotal", finalTotal)
                    .put("calculatedAt", System.currentTimeMillis());
                
                sendJsonResponse(context, calculation);
                promise.complete();
                
            } catch (Exception e) {
                logger.error("Error calculating order total for {}", orderId, e);
                promise.fail(e);
            }
        });
    }
}