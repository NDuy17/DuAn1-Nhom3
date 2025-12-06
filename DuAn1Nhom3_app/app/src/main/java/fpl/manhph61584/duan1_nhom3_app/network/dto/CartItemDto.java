package fpl.manhph61584.duan1_nhom3_app.network.dto;

import com.google.gson.annotations.SerializedName;
import fpl.manhph61584.duan1_nhom3_app.Product;

public class CartItemDto {
    @SerializedName("product")
    private Product product;
    
    private int quantity;
    private String color;
    private String size;

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
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



















