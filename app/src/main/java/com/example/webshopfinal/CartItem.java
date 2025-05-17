package com.example.webshopfinal;

public class CartItem {
    private String carpetDocId;
    private int quantity;

    public CartItem() {}

    public CartItem(String carpetDocId, int quantity) {
        this.carpetDocId = carpetDocId;
        this.quantity = quantity;
    }

    public String getCarpetDocId() { return carpetDocId; }
    public void setCarpetDocId(String carpetDocId) { this.carpetDocId = carpetDocId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
} 