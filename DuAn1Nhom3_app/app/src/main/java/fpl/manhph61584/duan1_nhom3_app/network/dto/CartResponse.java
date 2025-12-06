package fpl.manhph61584.duan1_nhom3_app.network.dto;

import java.util.List;

public class CartResponse {
    private String user;
    private List<CartItemDto> items;
    private String message;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public List<CartItemDto> getItems() {
        return items;
    }

    public void setItems(List<CartItemDto> items) {
        this.items = items;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}



