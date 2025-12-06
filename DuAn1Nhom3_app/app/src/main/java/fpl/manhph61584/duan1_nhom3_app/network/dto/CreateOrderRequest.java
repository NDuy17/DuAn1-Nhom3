package fpl.manhph61584.duan1_nhom3_app.network.dto;

import java.util.List;

public class CreateOrderRequest {
    private String receiverName;
    private String phone;
    private String address;
    private String note;
    private String voucherId;
    private List<OrderItemRequest> items;

    public CreateOrderRequest(String receiverName, String phone, String address, String note, String voucherId) {
        this.receiverName = receiverName;
        this.phone = phone;
        this.address = address;
        this.note = note;
        this.voucherId = voucherId;
        this.items = null;
    }

    public CreateOrderRequest(String receiverName, String phone, String address, String note, String voucherId, List<OrderItemRequest> items) {
        this.receiverName = receiverName;
        this.phone = phone;
        this.address = address;
        this.note = note;
        this.voucherId = voucherId;
        this.items = items;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(String voucherId) {
        this.voucherId = voucherId;
    }

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }
}

