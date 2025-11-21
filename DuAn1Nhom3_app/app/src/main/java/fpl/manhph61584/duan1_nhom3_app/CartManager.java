package fpl.manhph61584.duan1_nhom3_app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CartManager {

    private static final List<CartItem> CART_ITEMS = new ArrayList<>();

    private CartManager() {
    }

    public static void addToCart(Product product, int quantity, String color, String size) {
        if (product == null || quantity <= 0) {
            return;
        }

        String safeColor = color == null || color.isEmpty() ? "Mặc định" : color;
        String safeSize = size == null || size.isEmpty() ? "Free size" : size;

        for (CartItem item : CART_ITEMS) {
            if (item.getProduct().getId().equals(product.getId())
                    && Objects.equals(item.getColor(), safeColor)
                    && Objects.equals(item.getSize(), safeSize)) {
                item.increase(quantity);
                return;
            }
        }

        CART_ITEMS.add(new CartItem(product, quantity, safeColor, safeSize));
    }

    public static List<CartItem> getCartItems() {
        return Collections.unmodifiableList(CART_ITEMS);
    }

    public static double getTotal() {
        double total = 0;
        for (CartItem item : CART_ITEMS) {
            total += item.getSubtotal();
        }
        return total;
    }

    public static void clear() {
        CART_ITEMS.clear();
    }
}

