package org.example.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Order {
    
    private String id;
    private String customerId;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public Order() {}
    
    @JsonCreator
    public Order(@JsonProperty("id") String id,
                 @JsonProperty("customerId") String customerId,
                 @JsonProperty("items") List<OrderItem> items,
                 @JsonProperty("totalAmount") BigDecimal totalAmount,
                 @JsonProperty("status") OrderStatus status) {
        this.id = id;
        this.customerId = customerId;
        this.items = items;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { 
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @Override
    public String toString() {
        return "Order{" +
               "id='" + id + '\'' +
               ", customerId='" + customerId + '\'' +
               ", items=" + items +
               ", totalAmount=" + totalAmount +
               ", status=" + status +
               ", createdAt=" + createdAt +
               ", updatedAt=" + updatedAt +
               '}';
    }
    
    // Inner classes
    public static class OrderItem {
        private String productId;
        private String productName;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        
        public OrderItem() {}
        
        @JsonCreator
        public OrderItem(@JsonProperty("productId") String productId,
                        @JsonProperty("productName") String productName,
                        @JsonProperty("quantity") int quantity,
                        @JsonProperty("unitPrice") BigDecimal unitPrice) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
        
        // Getters and Setters
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { 
            this.quantity = quantity;
            this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
        
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { 
            this.unitPrice = unitPrice;
            this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
        
        public BigDecimal getTotalPrice() { return totalPrice; }
        
        @Override
        public String toString() {
            return "OrderItem{" +
                   "productId='" + productId + '\'' +
                   ", productName='" + productName + '\'' +
                   ", quantity=" + quantity +
                   ", unitPrice=" + unitPrice +
                   ", totalPrice=" + totalPrice +
                   '}';
        }
    }
    
    public enum OrderStatus {
        PENDING,
        CONFIRMED,
        PROCESSING,
        SHIPPED,
        DELIVERED,
        CANCELLED,
        REFUNDED
    }
}