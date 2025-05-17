package com.example.webshopfinal;

import com.google.firebase.Timestamp;
import java.util.List;
import java.util.Map;

public class Order {
    private String userId;
    private String userEmail;
    private Map<String, Object> address;
    private List<CartItem> items;
    private double total;
    private Timestamp timestamp;

    public Order() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public Map<String, Object> getAddress() { return address; }
    public void setAddress(Map<String, Object> address) { this.address = address; }

    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
} 