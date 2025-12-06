package fpl.manhph61584.duan1_nhom3_app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import fpl.manhph61584.duan1_nhom3_app.network.ApiClient;
import fpl.manhph61584.duan1_nhom3_app.network.ApiService;
import fpl.manhph61584.duan1_nhom3_app.network.dto.OrderItemRequest;
import fpl.manhph61584.duan1_nhom3_app.Voucher;
import fpl.manhph61584.duan1_nhom3_app.UserManager;
import fpl.manhph61584.duan1_nhom3_app.Product;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartActivity extends AppCompatActivity implements VoucherAdapter.OnVoucherSelectListener {

    private RecyclerView recyclerView;
    private TextView txtSubtotal, txtTotal, txtDiscount, txtSelectedVoucherCode, txtSelectedVoucherName;
    private LinearLayout layoutDiscount, layoutVoucherSelector;
    private EditText edtReceiverName, edtPhone, edtAddress, edtNote;
    private Button btnCheckout;
    private CartAdapter adapter;
    private VoucherAdapter voucherAdapter;
    private Voucher selectedVoucher;
    private List<Voucher> vouchers = new ArrayList<>();
    private boolean isBuyNowMode = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        recyclerView = findViewById(R.id.cartRecyclerView);
        txtSubtotal = findViewById(R.id.txtSubtotal);
        txtTotal = findViewById(R.id.txtTotal);
        txtDiscount = findViewById(R.id.txtDiscount);
        txtSelectedVoucherCode = findViewById(R.id.txtSelectedVoucherCode);
        txtSelectedVoucherName = findViewById(R.id.txtSelectedVoucherName);
        layoutDiscount = findViewById(R.id.layoutDiscount);
        layoutVoucherSelector = findViewById(R.id.layoutVoucherSelector);
        edtReceiverName = findViewById(R.id.edtReceiverName);
        edtPhone = findViewById(R.id.edtPhone);
        edtAddress = findViewById(R.id.edtAddress);
        edtNote = findViewById(R.id.edtNote);
        btnCheckout = findViewById(R.id.cartCheckout);

        adapter = new CartAdapter(this, new ArrayList<>());
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        
        // Kiểm tra nếu là "Mua ngay" từ DetailProductActivity hoặc OrderStatusActivity
        isBuyNowMode = getIntent().getBooleanExtra("buy_now", false);
        if (isBuyNowMode) {
            // Kiểm tra xem có nhiều sản phẩm không (từ OrderStatusActivity)
            ArrayList<String> productIds = getIntent().getStringArrayListExtra("product_ids");
            if (productIds != null && !productIds.isEmpty()) {
                // Mua ngay nhiều sản phẩm
                ArrayList<Integer> quantities = getIntent().getIntegerArrayListExtra("quantities");
                ArrayList<String> colors = getIntent().getStringArrayListExtra("colors");
                ArrayList<String> sizes = getIntent().getStringArrayListExtra("sizes");
                loadMultipleProductsForBuyNow(productIds, quantities, colors, sizes);
            } else {
                // Mua ngay 1 sản phẩm (từ DetailProductActivity)
                String productId = getIntent().getStringExtra("product_id");
                int quantity = getIntent().getIntExtra("quantity", 1);
                String color = getIntent().getStringExtra("color");
                String size = getIntent().getStringExtra("size");
                
                if (productId != null && !productId.isEmpty()) {
                    loadProductForBuyNow(productId, quantity, color, size);
                } else {
                    loadCartFromServer();
                }
            }
        } else {
            loadCartFromServer();
        }

        voucherAdapter = new VoucherAdapter(vouchers, this);

        // Setup voucher selector click
        if (layoutVoucherSelector != null) {
            layoutVoucherSelector.setOnClickListener(v -> showVoucherDialog());
        }

        loadVouchers();
        updateTotal();

        btnCheckout.setOnClickListener(v -> handleCheckout());
    }



    @Override
    protected void onResume() {
        super.onResume();
        // Không reload nếu đang ở chế độ "Mua ngay" (để giữ sản phẩm "Mua ngay")
        if (!isBuyNowMode) {
            loadCartFromServer();
        }
    }

    private void loadProductForBuyNow(String productId, int quantity, String color, String size) {
        // Load product từ API
        ApiClient.getApiService().getProductDetail(productId).enqueue(new Callback<Product>() {
            @Override
            public void onResponse(Call<Product> call, Response<Product> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Product product = response.body();
                    CartItem buyNowItem = new CartItem(product, quantity, color, size);
                    
                    // Chỉ hiển thị sản phẩm "Mua ngay", không load thêm sản phẩm khác từ giỏ hàng
                    List<CartItem> cartItems = new ArrayList<>();
                    cartItems.add(buyNowItem);
                    
                    // Hiển thị danh sách chỉ với sản phẩm "Mua ngay"
                    adapter = new CartAdapter(CartActivity.this, cartItems);
                    recyclerView.setAdapter(adapter);
                    updateTotal();
                } else {
                    // Nếu không load được sản phẩm, load giỏ hàng bình thường
                    loadCartFromServer();
                }
            }

            @Override
            public void onFailure(Call<Product> call, Throwable t) {
                // Nếu không load được sản phẩm, load giỏ hàng bình thường
                loadCartFromServer();
            }
        });
    }

    private void loadMultipleProductsForBuyNow(ArrayList<String> productIds, ArrayList<Integer> quantities, ArrayList<String> colors, ArrayList<String> sizes) {
        // Load nhiều sản phẩm từ API
        List<CartItem> cartItems = new ArrayList<>();
        final int[] loadedCount = {0};
        final int totalProducts = productIds.size();
        
        for (int i = 0; i < productIds.size(); i++) {
            String productId = productIds.get(i);
            int quantity = (quantities != null && i < quantities.size()) ? quantities.get(i) : 1;
            String color = (colors != null && i < colors.size()) ? colors.get(i) : "Mặc định";
            String size = (sizes != null && i < sizes.size()) ? sizes.get(i) : "Free size";
            
            ApiClient.getApiService().getProductDetail(productId).enqueue(new Callback<Product>() {
                @Override
                public void onResponse(Call<Product> call, Response<Product> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Product product = response.body();
                        CartItem buyNowItem = new CartItem(product, quantity, color, size);
                        cartItems.add(buyNowItem);
                    }
                    
                    loadedCount[0]++;
                    if (loadedCount[0] >= totalProducts) {
                        // Đã load xong tất cả sản phẩm
                        if (!cartItems.isEmpty()) {
                            adapter = new CartAdapter(CartActivity.this, cartItems);
                            recyclerView.setAdapter(adapter);
                            updateTotal();
                        } else {
                            loadCartFromServer();
                        }
                    }
                }

                @Override
                public void onFailure(Call<Product> call, Throwable t) {
                    loadedCount[0]++;
                    if (loadedCount[0] >= totalProducts) {
                        // Đã load xong tất cả (kể cả lỗi)
                        if (!cartItems.isEmpty()) {
                            adapter = new CartAdapter(CartActivity.this, cartItems);
                            recyclerView.setAdapter(adapter);
                            updateTotal();
                        } else {
                            loadCartFromServer();
                        }
                    }
                }
            });
        }
    }

    private void loadCartFromServer() {
        String token = UserManager.getAuthToken();
        if (token == null) {
            // Chưa đăng nhập, load từ local
            adapter = new CartAdapter(this, new ArrayList<>(CartManager.getCartItems()));
            recyclerView.setAdapter(adapter);
            updateTotal();
            return;
        }

        String authHeader = "Bearer " + token;
        ApiClient.getApiService().getCart(authHeader).enqueue(new Callback<fpl.manhph61584.duan1_nhom3_app.network.dto.CartResponse>() {
            @Override
            public void onResponse(Call<fpl.manhph61584.duan1_nhom3_app.network.dto.CartResponse> call, Response<fpl.manhph61584.duan1_nhom3_app.network.dto.CartResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    fpl.manhph61584.duan1_nhom3_app.network.dto.CartResponse cartResponse = response.body();
                    List<fpl.manhph61584.duan1_nhom3_app.network.dto.CartItemDto> items = cartResponse.getItems();
                    
                    // Convert CartItemDto to CartItem
                    List<CartItem> cartItems = new ArrayList<>();
                    if (items != null) {
                        for (fpl.manhph61584.duan1_nhom3_app.network.dto.CartItemDto dto : items) {
                            if (dto.getProduct() != null) {
                                CartItem item = new CartItem(
                                    dto.getProduct(),
                                    dto.getQuantity(),
                                    dto.getColor() != null ? dto.getColor() : "Mặc định",
                                    dto.getSize() != null ? dto.getSize() : "Free size"
                                );
                                cartItems.add(item);
                            }
                        }
                    }
                    
                    // Update CartManager
                    CartManager.clear();
                    for (CartItem item : cartItems) {
                        CartManager.addToCart(item.getProduct(), item.getQuantity(), item.getColor(), item.getSize());
                    }
                    
                    // Update adapter
                    adapter = new CartAdapter(CartActivity.this, new ArrayList<>(cartItems));
                    recyclerView.setAdapter(adapter);
                    updateTotal();
                } else {
                    // Load từ local nếu server lỗi
                    adapter = new CartAdapter(CartActivity.this, new ArrayList<>(CartManager.getCartItems()));
                    recyclerView.setAdapter(adapter);
                    updateTotal();
                }
            }

            @Override
            public void onFailure(Call<fpl.manhph61584.duan1_nhom3_app.network.dto.CartResponse> call, Throwable t) {
                // Load từ local nếu server lỗi
                adapter = new CartAdapter(CartActivity.this, new ArrayList<>(CartManager.getCartItems()));
                recyclerView.setAdapter(adapter);
                updateTotal();
            }
        });
    }

    private void loadVouchers() {
        String token = UserManager.getAuthToken();
        
        if (token != null) {
            // Đã đăng nhập: lấy cả vouchers của user và vouchers public
            String authHeader = "Bearer " + token;
            ApiClient.getApiService().getVouchers(authHeader).enqueue(new Callback<List<Voucher>>() {
                @Override
                public void onResponse(Call<List<Voucher>> call, Response<List<Voucher>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        vouchers.clear();
                        vouchers.addAll(response.body());
                        voucherAdapter.notifyDataSetChanged();
                    } else {
                        // Nếu lỗi, thử load vouchers public
                        loadPublicVouchers();
                    }
                }

                @Override
                public void onFailure(Call<List<Voucher>> call, Throwable t) {
                    // Nếu lỗi, thử load vouchers public
                    loadPublicVouchers();
                }
            });
        } else {
            // Chưa đăng nhập: chỉ lấy vouchers public
            loadPublicVouchers();
        }
    }

    private void loadPublicVouchers() {
        ApiClient.getApiService().getPublicVouchers().enqueue(new Callback<List<Voucher>>() {
            @Override
            public void onResponse(Call<List<Voucher>> call, Response<List<Voucher>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    vouchers.clear();
                    vouchers.addAll(response.body());
                    voucherAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<Voucher>> call, Throwable t) {
                // Không hiển thị lỗi nếu không load được vouchers
            }
        });
    }

    @Override
    public void onVoucherSelected(Voucher voucher) {
        selectedVoucher = voucher;
        voucherAdapter.setSelectedVoucher(voucher);
        updateVoucherDisplay();
        updateTotal();
    }
    
    private void updateVoucherDisplay() {
        if (selectedVoucher != null) {
            txtSelectedVoucherCode.setText(selectedVoucher.getCode());
            txtSelectedVoucherCode.setTextColor(getResources().getColor(android.R.color.black));
            txtSelectedVoucherName.setText(selectedVoucher.getName());
            txtSelectedVoucherName.setVisibility(TextView.VISIBLE);
        } else {
            txtSelectedVoucherCode.setText("Chọn mã giảm giá");
            txtSelectedVoucherCode.setTextColor(getResources().getColor(android.R.color.darker_gray));
            txtSelectedVoucherName.setText("");
            txtSelectedVoucherName.setVisibility(TextView.GONE);
        }
    }
    
    private void showVoucherDialog() {
        // Tính tổng tiền hiện tại
        double subtotal = 0;
        if (adapter != null && adapter.items != null) {
            for (CartItem item : adapter.items) {
                subtotal += item.getSubtotal();
            }
        }
        
        // Lọc vouchers có thể áp dụng
        final List<Voucher> applicableVouchers = new ArrayList<>();
        for (Voucher voucher : vouchers) {
            if (voucher.canApply(subtotal)) {
                applicableVouchers.add(voucher);
            }
        }
        
        if (applicableVouchers.isEmpty()) {
            Toast.makeText(this, "Không có mã giảm giá nào khả dụng", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Tạo dialog với RecyclerView
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Chọn mã giảm giá");
        
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_vouchers, null);
        RecyclerView recyclerViewVouchers = dialogView.findViewById(R.id.recyclerViewVouchers);
        TextView txtNoVoucher = dialogView.findViewById(R.id.txtNoVoucher);
        
        recyclerViewVouchers.setVisibility(View.VISIBLE);
        txtNoVoucher.setVisibility(View.GONE);
        
        builder.setView(dialogView);
        builder.setNegativeButton("Hủy", null);
        builder.setNeutralButton("Bỏ chọn", (dialogInterface, which) -> {
            selectedVoucher = null;
            voucherAdapter.setSelectedVoucher(null);
            updateVoucherDisplay();
            updateTotal();
        });
        
        // Tạo dialog trước khi tạo adapter để có thể dùng trong inner class
        final android.app.AlertDialog dialog = builder.create();
        
        // Tạo adapter với listener để đóng dialog sau khi chọn
        VoucherAdapter dialogAdapter = new VoucherAdapter(applicableVouchers, new VoucherAdapter.OnVoucherSelectListener() {
            @Override
            public void onVoucherSelected(Voucher voucher) {
                // Gọi method của CartActivity
                CartActivity.this.onVoucherSelected(voucher);
                dialog.dismiss();
            }
        });
        dialogAdapter.setSelectedVoucher(selectedVoucher);
        recyclerViewVouchers.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewVouchers.setAdapter(dialogAdapter);
        
        dialog.show();
    }

    private void updateTotal() {
        // Tính tổng từ adapter (bao gồm cả sản phẩm "Mua ngay")
        double subtotal = 0;
        if (adapter != null && adapter.items != null) {
            for (CartItem item : adapter.items) {
                subtotal += item.getSubtotal();
            }
        }
        txtSubtotal.setText(String.format("%,.0f₫", subtotal));

        double discount = 0;
        if (selectedVoucher != null) {
            // Kiểm tra voucher có thể áp dụng không
            if (!selectedVoucher.canApply(subtotal)) {
                // Tự động bỏ chọn voucher nếu không thể áp dụng
                selectedVoucher = null;
                voucherAdapter.setSelectedVoucher(null);
                updateVoucherDisplay();
            } else {
                discount = selectedVoucher.calculateDiscount(subtotal);
            }
        }
        
        if (discount > 0) {
            txtDiscount.setText(String.format("-%,.0f₫", discount));
            layoutDiscount.setVisibility(LinearLayout.VISIBLE);
        } else {
            layoutDiscount.setVisibility(LinearLayout.GONE);
        }

        double total = subtotal - discount;
        if (total < 0) total = 0;
        txtTotal.setText(String.format("%,.0f₫", total));
    }

    private void handleCheckout() {
        String receiverName = edtReceiverName.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String address = edtAddress.getText().toString().trim();
        String note = edtNote.getText().toString().trim();

        // Kiểm tra từ adapter (bao gồm cả sản phẩm "Mua ngay")
        if (adapter == null || adapter.items == null || adapter.items.isEmpty()) {
            Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(receiverName)) {
            Toast.makeText(this, "Vui lòng nhập tên người nhận", Toast.LENGTH_SHORT).show();
            edtReceiverName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show();
            edtPhone.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(address)) {
            Toast.makeText(this, "Vui lòng nhập địa chỉ", Toast.LENGTH_SHORT).show();
            edtAddress.requestFocus();
            return;
        }

        // Tạo đơn hàng và gửi lên server
        String token = UserManager.getAuthToken();
        if (token == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để thanh toán", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCheckout.setEnabled(false);
        btnCheckout.setText("Đang xử lý...");

        // Tính tổng tiền
        double subtotal = 0;
        for (CartItem item : adapter.items) {
            subtotal += item.getSubtotal();
        }
        double discount = selectedVoucher != null ? selectedVoucher.calculateDiscount(subtotal) : 0;
        double finalAmount = subtotal - discount;

        // Lấy voucher ID nếu có
        String voucherId = selectedVoucher != null ? selectedVoucher.getId() : null;

        // Luôn gửi items trực tiếp trong request (không phụ thuộc vào giỏ hàng trên server)
        List<OrderItemRequest> orderItems = new ArrayList<>();
        if (adapter != null && adapter.items != null && !adapter.items.isEmpty()) {
            for (CartItem item : adapter.items) {
                if (item.getProduct() != null && item.getProduct().getId() != null) {
                    OrderItemRequest orderItem = 
                        new OrderItemRequest(
                            item.getProduct().getId(),
                            item.getQuantity(),
                            item.getUnitPrice(),
                            item.getColor() != null ? item.getColor() : "Mặc định",
                            item.getSize() != null ? item.getSize() : "Free size"
                        );
                    orderItems.add(orderItem);
                }
            }
        }
        
        // Kiểm tra nếu không có items
        if (orderItems.isEmpty()) {
            btnCheckout.setEnabled(true);
            btnCheckout.setText("Thanh toán");
            Toast.makeText(this, "Không có sản phẩm để thanh toán", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Tạo request với items
        fpl.manhph61584.duan1_nhom3_app.network.dto.CreateOrderRequest request = 
            new fpl.manhph61584.duan1_nhom3_app.network.dto.CreateOrderRequest(receiverName, phone, address, note, voucherId, orderItems);

        String authHeader = "Bearer " + token;
        
        // Log request để debug
        android.util.Log.d("Payment", "Sending order request with " + orderItems.size() + " items");
        
        ApiClient.getApiService().createOrder(authHeader, request).enqueue(new Callback<fpl.manhph61584.duan1_nhom3_app.network.dto.CreateOrderResponse>() {
            @Override
            public void onResponse(Call<fpl.manhph61584.duan1_nhom3_app.network.dto.CreateOrderResponse> call, Response<fpl.manhph61584.duan1_nhom3_app.network.dto.CreateOrderResponse> response) {
                btnCheckout.setEnabled(true);
                btnCheckout.setText("Thanh toán");
                
                android.util.Log.d("Payment", "Response code: " + response.code());
                
                if (response.isSuccessful() && response.body() != null) {
                    android.util.Log.d("Payment", "✅ Payment successful! Order saved to MongoDB");
                    Toast.makeText(CartActivity.this, "Thanh toán thành công!", Toast.LENGTH_SHORT).show();
                    
                    // Clear form và giỏ hàng
                    edtPhone.setText("");
                    edtAddress.setText("");
                    edtNote.setText("");
                    selectedVoucher = null;
                    updateVoucherDisplay();
                    
                    // Reload cart (sẽ trống sau khi tạo order)
                    loadCartFromServer();
                    
                    // Quay về MainActivity
                    Intent intent = new Intent(CartActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    String errorMsg = "Lỗi thanh toán";
                    try {
                        if (response.errorBody() != null) {
                            String errorStr = response.errorBody().string();
                            android.util.Log.e("PaymentError", "Error response: " + errorStr);
                            
                            // Thử parse JSON error message
                            try {
                                com.google.gson.JsonObject jsonObject = new com.google.gson.Gson().fromJson(errorStr, com.google.gson.JsonObject.class);
                                if (jsonObject.has("message")) {
                                    errorMsg = jsonObject.get("message").getAsString();
                                } else if (jsonObject.has("error")) {
                                    errorMsg = jsonObject.get("error").getAsString();
                                } else {
                                    errorMsg += ": " + errorStr;
                                }
                            } catch (Exception e) {
                                errorMsg += ": " + errorStr;
                            }
                        } else {
                            errorMsg += " (Code: " + response.code() + ")";
                        }
                    } catch (Exception e) {
                        android.util.Log.e("PaymentError", "Error reading error body", e);
                        errorMsg += ": " + response.message() + " (Code: " + response.code() + ")";
                    }
                    Toast.makeText(CartActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<fpl.manhph61584.duan1_nhom3_app.network.dto.CreateOrderResponse> call, Throwable t) {
                btnCheckout.setEnabled(true);
                btnCheckout.setText("Thanh toán");
                Toast.makeText(CartActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}


