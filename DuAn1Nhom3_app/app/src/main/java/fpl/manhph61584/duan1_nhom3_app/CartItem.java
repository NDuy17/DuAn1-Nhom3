package fpl.manhph61584.duan1_nhom3_app;

public class CartItem {
    private final Product product;
    private int quantity;
    private final String color;
    private final String size;
    private final double unitPrice;

    public CartItem(Product product, int quantity, String color, String size) {
        this.product = product;
        this.quantity = quantity;
        this.color = color;
        this.size = size;
        this.unitPrice = product != null ? product.getPrice() : 0;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void increase(int delta) {
        this.quantity += delta;
    }

    public String getColor() {
        return color;
    }

    public String getSize() {
        return size;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public double getSubtotal() {
        return unitPrice * quantity;
    }
}

