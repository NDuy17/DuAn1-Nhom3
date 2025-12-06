package fpl.manhph61584.duan1_nhom3_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fpl.manhph61584.duan1_nhom3_app.network.ApiClient;
import fpl.manhph61584.duan1_nhom3_app.UserManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderStatusActivity extends AppCompatActivity implements OrderStatusAdapter.OnStatusChangedListener {

    private RecyclerView recyclerView;
    private TextView txtEmptyOrders, txtCartTitle, btnEditCart;
    private LinearLayout layoutEditBottomBar;
    private AppCompatCheckBox checkboxSelectAll;
    private Button btnDelete, btnMoveToWishlist;
    private OrderStatusAdapter adapter;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_status);

        recyclerView = findViewById(R.id.recyclerOrderStatus);
        txtEmptyOrders = findViewById(R.id.txtEmptyOrders);
        txtCartTitle = findViewById(R.id.txtCartTitle);
        btnEditCart = findViewById(R.id.btnEditCart);
        layoutEditBottomBar = findViewById(R.id.layoutEditBottomBar);
        checkboxSelectAll = findViewById(R.id.checkboxSelectAll);
        btnDelete = findViewById(R.id.btnDelete);
        btnMoveToWishlist = findViewById(R.id.btnMoveToWishlist);

        adapter = new OrderStatusAdapter(
                this,
                new ArrayList<>(),
                this
        );
        adapter.setSelectionListener((selectedPositions, total) -> {
            // Cập nhật checkbox "Tất cả"
            boolean allSelected = selectedPositions.size() == adapter.getItemCount() && adapter.getItemCount() > 0;
            checkboxSelectAll.setOnCheckedChangeListener(null);
            checkboxSelectAll.setChecked(allSelected);
            checkboxSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
                adapter.selectAll(isChecked);
            });
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        setupEditButton();
        setupDeleteButton();
        setupBuyNowButton();
        setupSelectAllCheckbox();
        updateCartTitle();
        setupBottomNavigation();
        loadCartFromServer();
    }

    private void setupEditButton() {
        btnEditCart.setOnClickListener(v -> {
            isEditMode = !isEditMode;
            if (isEditMode) {
                btnEditCart.setText("Xong");
                layoutEditBottomBar.setVisibility(View.VISIBLE);
            } else {
                btnEditCart.setText("Chỉnh sửa");
                layoutEditBottomBar.setVisibility(View.GONE);
            }
            adapter.setEditMode(isEditMode);
        });
    }

    private void setupBuyNowButton() {
        btnMoveToWishlist.setOnClickListener(v -> {
            Set<Integer> selectedPositions = adapter.getSelectedPositions();
            if (selectedPositions.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn sản phẩm để mua ngay", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Lấy tất cả sản phẩm được chọn để mua ngay
            List<Integer> positions = new ArrayList<>(selectedPositions);
            ArrayList<String> productIds = new ArrayList<>();
            ArrayList<Integer> quantities = new ArrayList<>();
            ArrayList<String> colors = new ArrayList<>();
            ArrayList<String> sizes = new ArrayList<>();
            
            for (Integer pos : positions) {
                if (pos >= 0 && pos < adapter.orderItems.size()) {
                    CartItem item = adapter.orderItems.get(pos);
                    if (item.getProduct() != null && item.getProduct().getId() != null) {
                        productIds.add(item.getProduct().getId());
                        quantities.add(item.getQuantity());
                        colors.add(item.getColor() != null ? item.getColor() : "Mặc định");
                        sizes.add(item.getSize() != null ? item.getSize() : "Free size");
                    }
                }
            }
            
            if (!productIds.isEmpty()) {
                // Chuyển đến CartActivity với chế độ "Mua ngay" và nhiều sản phẩm
                Intent intent = new Intent(OrderStatusActivity.this, CartActivity.class);
                intent.putExtra("buy_now", true);
                intent.putStringArrayListExtra("product_ids", productIds);
                intent.putIntegerArrayListExtra("quantities", quantities);
                intent.putStringArrayListExtra("colors", colors);
                intent.putStringArrayListExtra("sizes", sizes);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });
    }

    private void setupDeleteButton() {
        btnDelete.setOnClickListener(v -> {
            Set<Integer> selectedPositions = adapter.getSelectedPositions();
            if (selectedPositions.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn sản phẩm để xóa", Toast.LENGTH_SHORT).show();
                return;
            }

            // Xóa các sản phẩm đã chọn
            String token = UserManager.getAuthToken();
            List<CartItem> items = new ArrayList<>(adapter.orderItems);
            List<Integer> positionsToDelete = new ArrayList<>(selectedPositions);
            
            // Sort descending to avoid index issues when removing
            positionsToDelete.sort((a, b) -> b.compareTo(a));
            
            int[] deleteCount = {0};
            int totalToDelete = positionsToDelete.size();
            
            for (Integer pos : positionsToDelete) {
                if (pos >= 0 && pos < items.size()) {
                    CartItem item = items.get(pos);
                    if (item.getProduct() != null && item.getProduct().getId() != null) {
                        if (token != null) {
                            // Xóa từ server
                            String authHeader = "Bearer " + token;
                            ApiClient.getApiService().removeFromCart(authHeader, item.getProduct().getId()).enqueue(new Callback<fpl.manhph61584.duan1_nhom3_app.network.dto.CartResponse>() {
                                @Override
                                public void onResponse(Call<fpl.manhph61584.duan1_nhom3_app.network.dto.CartResponse> call, Response<fpl.manhph61584.duan1_nhom3_app.network.dto.CartResponse> response) {
                                    deleteCount[0]++;
                                    if (deleteCount[0] >= totalToDelete) {
                                        // Reload cart sau khi xóa xong tất cả
                                        loadCartFromServer();
                                        Toast.makeText(OrderStatusActivity.this, "Đã xóa sản phẩm", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<fpl.manhph61584.duan1_nhom3_app.network.dto.CartResponse> call, Throwable t) {
                                    deleteCount[0]++;
                                    if (deleteCount[0] >= totalToDelete) {
                                        loadCartFromServer();
                                        Toast.makeText(OrderStatusActivity.this, "Đã xóa sản phẩm", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        } else {
                            // Chưa đăng nhập, chỉ xóa local
                            CartManager.removeFromCart(item);
                            deleteCount[0]++;
                            if (deleteCount[0] >= totalToDelete) {
                                loadCartFromServer();
                                Toast.makeText(OrderStatusActivity.this, "Đã xóa sản phẩm", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        deleteCount[0]++;
                    }
                } else {
                    deleteCount[0]++;
                }
            }
        });
    }

    private void setupSelectAllCheckbox() {
        checkboxSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            adapter.selectAll(isChecked);
        });
    }

    private void updateCartTitle() {
        int count = adapter.getItemCount();
        txtCartTitle.setText("Giỏ hàng (" + count + ")");
    }

    private void setupBottomNavigation() {
        LinearLayout btnHome = findViewById(R.id.btnHome);
        LinearLayout btnBottomCart = findViewById(R.id.btnBottomCart);
        LinearLayout btnProfile = findViewById(R.id.btnProfile);

        if (btnHome != null) {
            btnHome.setOnClickListener(v -> {
                Intent intent = new Intent(OrderStatusActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
            });
        }

        if (btnBottomCart != null) {
            btnBottomCart.setOnClickListener(v -> {
                // Đã ở trang OrderStatus rồi, không cần làm gì
            });
        }

        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                if (UserManager.getCurrentUser() == null) {
                    Intent intent = new Intent(OrderStatusActivity.this, LoginActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                } else {
                    Intent intent = new Intent(OrderStatusActivity.this, ProfileActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCartFromServer();
    }

    private void loadCartFromServer() {
        String token = UserManager.getAuthToken();
        if (token == null) {
            // Chưa đăng nhập, load từ local
            List<CartItem> items = new ArrayList<>(CartManager.getCartItems());
            adapter.updateData(items);
            adapter.setEditMode(isEditMode);
            updateEmptyState();
            updateCartTitle();
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
                    adapter.updateData(cartItems);
                    adapter.setEditMode(isEditMode);
                    updateEmptyState();
                    updateCartTitle();
                    Log.d("OrderStatusActivity", "Cart loaded from server: " + cartItems.size() + " items");
                } else {
                    Log.e("OrderStatusActivity", "Failed to load cart from server: " + response.code());
                    // Load from local as fallback
                    List<CartItem> items = new ArrayList<>(CartManager.getCartItems());
                    adapter.updateData(items);
                    adapter.setEditMode(isEditMode);
                    updateEmptyState();
                    updateCartTitle();
                }
            }

            @Override
            public void onFailure(Call<fpl.manhph61584.duan1_nhom3_app.network.dto.CartResponse> call, Throwable t) {
                Log.e("OrderStatusActivity", "Error loading cart from server", t);
                // Load from local as fallback
                List<CartItem> items = new ArrayList<>(CartManager.getCartItems());
                adapter.updateData(items);
                updateEmptyState();
            }
        });
    }

    private void updateEmptyState() {
        boolean isEmpty = adapter.getItemCount() == 0;
        txtEmptyOrders.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onStatusChanged() {
        // Khi trạng thái thay đổi chỉ cần cập nhật lại empty state (nếu cần)
        updateEmptyState();
    }
}



