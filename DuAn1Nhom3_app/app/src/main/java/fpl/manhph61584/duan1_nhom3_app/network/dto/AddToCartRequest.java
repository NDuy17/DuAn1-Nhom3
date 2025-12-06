package fpl.manhph61584.duan1_nhom3_app.network.dto;

public class AddToCartRequest {
    private String productId;
    private int quantity;
    private String color;
    private String size;

    public AddToCartRequest(String productId, int quantity, String color, String size) {
        this.productId = productId;
        this.quantity = quantity;
        this.color = color != null ? color : "Mặc định";
        this.size = size != null ? size : "Free size";
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






















