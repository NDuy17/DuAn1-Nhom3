package fpl.manhph61584.duan1_nhom3_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import fpl.manhph61584.duan1_nhom3_app.network.ApiClient;
import fpl.manhph61584.duan1_nhom3_app.network.dto.OrderDto;
import fpl.manhph61584.duan1_nhom3_app.UserManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerViewOrders;
    private TextView txtEmptyOrders, tabAll, tabPending, tabProcessing, tabShipped, tabCancelled;
    private ImageView btnBack;
    private OrderHistoryAdapter orderAdapter;
    private DeliveredItemsAdapter itemsAdapter;
    private List<OrderDto> allOrders = new ArrayList<>();
    private String currentFilter = "all"; // "all", "pending", "processing", "shipped", "cancelled"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);

        recyclerViewOrders = findViewById(R.id.recyclerViewOrders);
        txtEmptyOrders = findViewById(R.id.txtEmptyOrders);
        tabAll = findViewById(R.id.tabAll);
        tabPending = findViewById(R.id.tabPending);
        tabProcessing = findViewById(R.id.tabProcessing);
        tabShipped = findViewById(R.id.tabShipped);
        tabCancelled = findViewById(R.id.tabHuy);
        btnBack = findViewById(R.id.btnBack);

        orderAdapter = new OrderHistoryAdapter(this, new ArrayList<>());
        itemsAdapter = new DeliveredItemsAdapter(this, new ArrayList<>());
        recyclerViewOrders.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewOrders.setAdapter(orderAdapter);

        setupTabs();
        loadOrders();

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupTabs() {
        tabAll.setOnClickListener(v -> {
            currentFilter = "all";
            updateTabSelection();
            filterOrders();
        });

        tabPending.setOnClickListener(v -> {
            currentFilter = "pending";
            updateTabSelection();
            filterOrders();
        });

        tabProcessing.setOnClickListener(v -> {
            currentFilter = "processing";
            updateTabSelection();
            filterOrders();
        });

        tabShipped.setOnClickListener(v -> {
            currentFilter = "shipped";
            updateTabSelection();
            filterOrders();
        });

        if (tabCancelled != null) {
            tabCancelled.setOnClickListener(v -> {
                currentFilter = "cancelled";
                updateTabSelection();
                filterOrders();
            });
        }
    }

    private void updateTabSelection() {
        tabAll.setTextColor(getResources().getColor(currentFilter.equals("all") ? R.color.primary : android.R.color.darker_gray));
        tabAll.setTypeface(null, currentFilter.equals("all") ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        
        tabPending.setTextColor(getResources().getColor(currentFilter.equals("pending") ? R.color.primary : android.R.color.darker_gray));
        tabPending.setTypeface(null, currentFilter.equals("pending") ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        
        tabProcessing.setTextColor(getResources().getColor(currentFilter.equals("processing") ? R.color.primary : android.R.color.darker_gray));
        tabProcessing.setTypeface(null, currentFilter.equals("processing") ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        
        tabShipped.setTextColor(getResources().getColor(currentFilter.equals("shipped") ? R.color.primary : android.R.color.darker_gray));
        tabShipped.setTypeface(null, currentFilter.equals("shipped") ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        
        if (tabCancelled != null) {
            tabCancelled.setTextColor(getResources().getColor(currentFilter.equals("cancelled") ? R.color.primary : android.R.color.darker_gray));
            tabCancelled.setTypeface(null, currentFilter.equals("cancelled") ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
    }

    private void loadOrders() {
        String token = UserManager.getAuthToken();
        if (token == null) {
            txtEmptyOrders.setVisibility(View.VISIBLE);
            recyclerViewOrders.setVisibility(View.GONE);
            return;
        }

        String authHeader = "Bearer " + token;
        ApiClient.getApiService().getOrders(authHeader, null).enqueue(new Callback<List<OrderDto>>() {
            @Override
            public void onResponse(Call<List<OrderDto>> call, Response<List<OrderDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allOrders.clear();
                    allOrders.addAll(response.body());
                    filterOrders();
                } else {
                    txtEmptyOrders.setVisibility(View.VISIBLE);
                    recyclerViewOrders.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<List<OrderDto>> call, Throwable t) {
                txtEmptyOrders.setVisibility(View.VISIBLE);
                recyclerViewOrders.setVisibility(View.GONE);
            }
        });
    }

    private void filterOrders() {
        // Nếu là tab "Hoàn thành" (shipped), hiển thị items thay vì orders
        if (currentFilter.equals("shipped")) {
            List<fpl.manhph61584.duan1_nhom3_app.network.dto.OrderItemDto> allItems = new ArrayList<>();
            
            // Lấy tất cả items từ các đơn hàng đã giao
            for (OrderDto order : allOrders) {
                if ("delivered".equals(order.getStatus()) && order.getItems() != null) {
                    allItems.addAll(order.getItems());
                }
            }
            
            itemsAdapter.updateItems(allItems);
            recyclerViewOrders.setAdapter(itemsAdapter);
            
            if (allItems.isEmpty()) {
                txtEmptyOrders.setVisibility(View.VISIBLE);
                recyclerViewOrders.setVisibility(View.GONE);
            } else {
                txtEmptyOrders.setVisibility(View.GONE);
                recyclerViewOrders.setVisibility(View.VISIBLE);
            }
        } else {
            // Các tab khác hiển thị orders như bình thường
            List<OrderDto> filteredOrders = new ArrayList<>();
            
            for (OrderDto order : allOrders) {
                if (currentFilter.equals("all")) {
                    filteredOrders.add(order);
                } else if (currentFilter.equals("pending") && "pending".equals(order.getStatus())) {
                    filteredOrders.add(order);
                } else if (currentFilter.equals("processing") && "processing".equals(order.getStatus())) {
                    filteredOrders.add(order);
                } else if (currentFilter.equals("cancelled") && "cancelled".equals(order.getStatus())) {
                    filteredOrders.add(order);
                }
            }

            orderAdapter.updateOrders(filteredOrders);
            recyclerViewOrders.setAdapter(orderAdapter);
            
            if (filteredOrders.isEmpty()) {
                txtEmptyOrders.setVisibility(View.VISIBLE);
                recyclerViewOrders.setVisibility(View.GONE);
            } else {
                txtEmptyOrders.setVisibility(View.GONE);
                recyclerViewOrders.setVisibility(View.VISIBLE);
            }
        }
    }

    public void reloadOrders() {
        loadOrders();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}

