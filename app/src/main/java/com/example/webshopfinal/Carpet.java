package com.example.webshopfinal;

public class Carpet {
    private String name;
    private String description;
    private double price;
    private String material;
    private double width;
    private double length;
    private String color;
    private String imageUrl;
    private String docId;
    private double area;

    // Default constructor
    public Carpet() {}

    // Constructor with all fields except id
    public Carpet(String name, String description, double price, String material,
                 double width, double length, String color, String imageUrl) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.material = material;
        this.width = width;
        this.length = length;
        this.color = color;
        this.imageUrl = imageUrl;
        this.area = width * length;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }
    public double getWidth() { return width; }
    public void setWidth(double width) { 
        this.width = width;
        this.area = width * length;
    }
    public double getLength() { return length; }
    public void setLength(double length) { 
        this.length = length;
        this.area = width * length;
    }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public double getArea() { return area; }
    public void setArea(double area) { this.area = area; }
    public String getDocId() { return docId; }
    public void setDocId(String docId) { this.docId = docId; }

    @Override
    public String toString() {
        return "Carpet{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", material='" + material + '\'' +
                ", width=" + width +
                ", length=" + length +
                ", color='" + color + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", area=" + area +
                '}';
    }
} 