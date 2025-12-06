package fpl.manhph61584.duan1_nhom3_app.network.dto;

public class OrderItemRequest {
    private String productId;
    private int quantity;
    private double price;
    private String color;
    private String size;

    public OrderItemRequest(String productId, int quantity, double price, String color, String size) {
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
        this.color = color;
        this.size = size;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }
}










