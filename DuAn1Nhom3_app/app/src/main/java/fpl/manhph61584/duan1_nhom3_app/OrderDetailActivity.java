package fpl.manhph61584.duan1_nhom3_app;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import fpl.manhph61584.duan1_nhom3_app.network.ApiClient;
import fpl.manhph61584.duan1_nhom3_app.network.dto.OrderDto;
import fpl.manhph61584.duan1_nhom3_app.network.dto.OrderItemDto;
import fpl.manhph61584.duan1_nhom3_app.UserManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderDetailActivity extends AppCompatActivity {

    private TextView txtOrderId, txtOrderDate, txtOrderStatus;
    private TextView txtReceiverName, txtPhone, txtShippingAddress;
    private TextView txtTotalAmount, txtDiscountAmount, txtFinalAmount;
    private RecyclerView recyclerViewOrderItems;
    private ImageView btnBack;
    private OrderDetailAdapter adapter;
    private String orderId;
    private OrderDto currentOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        orderId = getIntent().getStringExtra("orderId");
        if (orderId == null) {
            Toast.makeText(this, "Không tìm thấy đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        loadOrderDetail();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        txtOrderId = findViewById(R.id.txtOrderId);
        txtOrderDate = findViewById(R.id.txtOrderDate);
        txtOrderStatus = findViewById(R.id.txtOrderStatus);
        txtReceiverName = findViewById(R.id.txtReceiverName);
        txtPhone = findViewById(R.id.txtPhone);
        txtShippingAddress = findViewById(R.id.txtShippingAddress);
        txtTotalAmount = findViewById(R.id.txtTotalAmount);
        txtDiscountAmount = findViewById(R.id.txtDiscountAmount);
        txtFinalAmount = findViewById(R.id.txtFinalAmount);
        recyclerViewOrderItems = findViewById(R.id.recyclerViewOrderItems);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new OrderDetailAdapter(this, new ArrayList<>());
        recyclerViewOrderItems.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewOrderItems.setAdapter(adapter);
    }

    private void loadOrderDetail() {
        String token = UserManager.getAuthToken();
        if (token == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String authHeader = "Bearer " + token;
        ApiClient.getApiService().getOrderDetail(authHeader, orderId).enqueue(new Callback<OrderDto>() {
            @Override
            public void onResponse(Call<OrderDto> call, Response<OrderDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    OrderDto order = response.body();
                    currentOrder = order;
                    displayOrderDetail(order);
                } else {
                    Toast.makeText(OrderDetailActivity.this, "Không thể tải chi tiết đơn hàng", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<OrderDto> call, Throwable t) {
                Toast.makeText(OrderDetailActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayOrderDetail(OrderDto order) {
        // Hiển thị mã đơn hàng
        String orderIdStr = order.getId();
        if (orderIdStr != null && orderIdStr.length() > 8) {
            orderIdStr = "#" + orderIdStr.substring(orderIdStr.length() - 8);
        } else if (orderIdStr != null) {
            orderIdStr = "#" + orderIdStr;
        } else {
            orderIdStr = "#N/A";
        }
        txtOrderId.setText("Mã đơn: " + orderIdStr);

        // Hiển thị ngày đặt hàng
        String createdAt = order.getCreatedAt();
        if (createdAt != null && !createdAt.isEmpty()) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                Date date = inputFormat.parse(createdAt);
                if (date != null) {
                    txtOrderDate.setText("Ngày đặt: " + outputFormat.format(date));
                } else {
                    txtOrderDate.setText("Ngày đặt: " + createdAt);
                }
            } catch (Exception e) {
                txtOrderDate.setText("Ngày đặt: " + createdAt);
            }
        } else {
            txtOrderDate.setText("Ngày đặt: N/A");
        }

        // Hiển thị trạng thái
        String status = order.getStatus();
        if (status != null) {
            switch (status) {
                case "pending":
                    txtOrderStatus.setText("Trạng thái: Chờ xác nhận");
                    txtOrderStatus.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                    break;
                case "processing":
                    txtOrderStatus.setText("Trạng thái: Đang xử lý");
                    txtOrderStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                    break;
                case "shipped":
                    txtOrderStatus.setText("Trạng thái: Đang vận chuyển");
                    txtOrderStatus.setTextColor(getResources().getColor(android.R.color.holo_purple));
                    break;
                case "delivered":
                    txtOrderStatus.setText("Trạng thái: Đã giao");
                    txtOrderStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    break;
                case "cancelled":
                    txtOrderStatus.setText("Trạng thái: Đã hủy");
                    txtOrderStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
                    break;
                default:
                    txtOrderStatus.setText("Trạng thái: " + status);
            }
        }

        // Hiển thị thông tin giao hàng
        if (UserManager.getCurrentUser() != null) {
            txtReceiverName.setText("Người nhận: " + UserManager.getCurrentUser().getName());
        } else {
            txtReceiverName.setText("Người nhận: N/A");
        }

        String phone = order.getPhone();
        txtPhone.setText("Số điện thoại: " + (phone != null ? phone : "N/A"));

        String address = order.getShippingAddress();
        txtShippingAddress.setText("Địa chỉ: " + (address != null ? address : "N/A"));

        // Hiển thị tổng tiền
        txtTotalAmount.setText(String.format("%,.0f₫", order.getTotalAmount()));
        txtDiscountAmount.setText(String.format("%,.0f₫", order.getDiscountAmount()));
        txtFinalAmount.setText(String.format("%,.0f₫", order.getFinalAmount()));

        // Hiển thị danh sách sản phẩm
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            adapter.updateItems(order.getItems());
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}





