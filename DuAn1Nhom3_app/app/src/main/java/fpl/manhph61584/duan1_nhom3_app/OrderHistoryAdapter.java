package fpl.manhph61584.duan1_nhom3_app;

import android.app.AlertDialog;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import fpl.manhph61584.duan1_nhom3_app.network.dto.OrderDto;
import fpl.manhph61584.duan1_nhom3_app.network.dto.ReviewRequest;
import fpl.manhph61584.duan1_nhom3_app.network.dto.ReviewResponse;
import fpl.manhph61584.duan1_nhom3_app.network.ApiClient;
import fpl.manhph61584.duan1_nhom3_app.UserManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.OrderViewHolder> {

    private final android.content.Context context;
    private List<OrderDto> orders;

    public OrderHistoryAdapter(android.content.Context context, List<OrderDto> orders) {
        this.context = context;
        this.orders = new ArrayList<>(orders);
    }

    public void updateOrders(List<OrderDto> newOrders) {
        this.orders = new ArrayList<>(newOrders);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_history, parent, false);
        return new OrderViewHolder(view, context, this);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        OrderDto order = orders.get(position);
        holder.bind(order);
    }

    @Override
    public int getItemCount() {
        return orders != null ? orders.size() : 0;
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        private final android.content.Context context;
        private final OrderHistoryAdapter adapter;
        private TextView txtOrderId;
        private TextView txtOrderDate;
        private TextView txtOrderTotal;
        private TextView tvStatus;
        private Button btnCancelOrder;
        private Button btnViewDetail;

        public OrderViewHolder(@NonNull View itemView, android.content.Context context, OrderHistoryAdapter adapter) {
            super(itemView);
            this.context = context;
            this.adapter = adapter;
            txtOrderId = itemView.findViewById(R.id.txtOrderId);
            txtOrderDate = itemView.findViewById(R.id.txtOrderDate);
            txtOrderTotal = itemView.findViewById(R.id.txtOrderTotal);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnCancelOrder = itemView.findViewById(R.id.btnCancelOrder);
            btnViewDetail = itemView.findViewById(R.id.btnViewDetail);
        }

        public void bind(OrderDto order) {
            // Hiển thị mã đơn hàng (lấy 8 ký tự cuối của ID)
            String orderId = order.getId();
            if (orderId != null && orderId.length() > 8) {
                orderId = "#" + orderId.substring(orderId.length() - 8);
            } else if (orderId != null) {
                orderId = "#" + orderId;
            } else {
                orderId = "#N/A";
            }
            txtOrderId.setText("Mã đơn: " + orderId);

            // Hiển thị ngày đặt hàng
            String createdAt = order.getCreatedAt();
            if (createdAt != null && !createdAt.isEmpty()) {
                try {
                    // Parse ISO date string và format lại
                    java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault());
                    java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
                    java.util.Date date = inputFormat.parse(createdAt);
                    if (date != null) {
                        txtOrderDate.setText("Ngày: " + outputFormat.format(date));
                    } else {
                        txtOrderDate.setText("Ngày: " + createdAt);
                    }
                } catch (Exception e) {
                    // Nếu parse lỗi, hiển thị nguyên chuỗi
                    txtOrderDate.setText("Ngày: " + createdAt);
                }
            } else {
                txtOrderDate.setText("Ngày: N/A");
            }

            // Hiển thị tổng tiền
            txtOrderTotal.setText("Tổng tiền: " + String.format("%,.0f₫", order.getFinalAmount()));

            // Hiển thị trạng thái
            String status = order.getStatus();
            if (status != null) {
                switch (status) {
                    case "pending":
                        tvStatus.setText("Chờ xác nhận");
                        tvStatus.setTextColor(context.getResources().getColor(android.R.color.holo_blue_dark));
                        tvStatus.setBackgroundColor(context.getResources().getColor(android.R.color.holo_blue_light));
                        break;
                    case "processing":
                        tvStatus.setText("Đang xử lý");
                        tvStatus.setTextColor(context.getResources().getColor(android.R.color.holo_orange_dark));
                        tvStatus.setBackgroundColor(context.getResources().getColor(android.R.color.holo_orange_light));
                        break;
                    case "shipped":
                        tvStatus.setText("Đang vận chuyển");
                        tvStatus.setTextColor(context.getResources().getColor(android.R.color.holo_purple));
                        tvStatus.setBackgroundColor(context.getResources().getColor(android.R.color.holo_blue_light));
                        break;
                    case "delivered":
                        tvStatus.setText("Đã giao");
                        tvStatus.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
                        tvStatus.setBackgroundColor(context.getResources().getColor(android.R.color.holo_green_light));
                        break;
                    case "cancelled":
                        tvStatus.setText("Đã hủy");
                        tvStatus.setTextColor(context.getResources().getColor(android.R.color.white));
                        tvStatus.setBackgroundColor(context.getResources().getColor(android.R.color.darker_gray));
                        break;
                    default:
                        tvStatus.setText(status);
                }
            }

            // Hiển thị nút hủy đơn chỉ khi đơn ở trạng thái chờ xác nhận
            boolean canCancel = status != null && status.equals("pending");
            btnCancelOrder.setVisibility(canCancel ? View.VISIBLE : View.GONE);

            // Click vào item để xem chi tiết
            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, OrderDetailActivity.class);
                intent.putExtra("orderId", order.getId());
                context.startActivity(intent);
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).overridePendingTransition(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                    );
                }
            });

            // Nút xem chi tiết
            btnViewDetail.setOnClickListener(v -> {
                Intent intent = new Intent(context, OrderDetailActivity.class);
                intent.putExtra("orderId", order.getId());
                context.startActivity(intent);
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).overridePendingTransition(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                    );
                }
            });

            // Nút hủy đơn
            btnCancelOrder.setOnClickListener(v -> {
                if (context instanceof android.app.Activity) {
                    android.app.Activity activity = (android.app.Activity) context;
                    new android.app.AlertDialog.Builder(activity)
                        .setTitle("Xác nhận hủy đơn")
                        .setMessage("Bạn có chắc chắn muốn hủy đơn hàng này?")
                        .setPositiveButton("Hủy đơn", (dialog, which) -> {
                            adapter.cancelOrder(order.getId(), activity);
                        })
                        .setNegativeButton("Không", null)
                        .show();
                }
            });
        }
    }

    private void cancelOrder(String orderId, android.app.Activity activity) {
        String token = UserManager.getAuthToken();
        if (token == null) {
            Toast.makeText(activity, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        String authHeader = "Bearer " + token;
        ApiClient.getApiService().cancelOrder(authHeader, orderId).enqueue(new Callback<OrderDto>() {
            @Override
            public void onResponse(Call<OrderDto> call, Response<OrderDto> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(activity, "Đã hủy đơn hàng thành công", Toast.LENGTH_SHORT).show();
                    // Reload orders
                    if (activity instanceof OrderHistoryActivity) {
                        ((OrderHistoryActivity) activity).reloadOrders();
                    }
                } else {
                    String errorMsg = "Lỗi hủy đơn hàng";
                    if (response.errorBody() != null) {
                        try {
                            String errorStr = response.errorBody().string();
                            com.google.gson.JsonObject jsonObject = new com.google.gson.Gson().fromJson(errorStr, com.google.gson.JsonObject.class);
                            if (jsonObject.has("message")) {
                                errorMsg = jsonObject.get("message").getAsString();
                            }
                        } catch (Exception e) {
                            errorMsg += ": " + response.message();
                        }
                    }
                    Toast.makeText(activity, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<OrderDto> call, Throwable t) {
                Toast.makeText(activity, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showReviewDialog(String productId) {
        if (!(context instanceof android.app.Activity)) {
            return;
        }
        
        android.app.Activity activity = (android.app.Activity) context;
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_review, null);
        builder.setView(dialogView);

        ImageView star1 = dialogView.findViewById(R.id.star1);
        ImageView star2 = dialogView.findViewById(R.id.star2);
        ImageView star3 = dialogView.findViewById(R.id.star3);
        ImageView star4 = dialogView.findViewById(R.id.star4);
        ImageView star5 = dialogView.findViewById(R.id.star5);
        TextView txtRatingValue = dialogView.findViewById(R.id.txtRatingValue);
        EditText edtComment = dialogView.findViewById(R.id.edtReviewComment);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelReview);
        Button btnSubmit = dialogView.findViewById(R.id.btnSubmitReview);

        final int[] selectedRating = {0};
        ImageView[] stars = {star1, star2, star3, star4, star5};

        View.OnClickListener starClickListener = v -> {
            int rating = 0;
            for (int i = 0; i < stars.length; i++) {
                if (v == stars[i]) {
                    rating = i + 1;
                    break;
                }
            }
            selectedRating[0] = rating;
            updateStars(stars, rating);
            txtRatingValue.setText(rating + " sao");
        };

        for (ImageView star : stars) {
            star.setOnClickListener(starClickListener);
        }

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            if (selectedRating[0] == 0) {
                Toast.makeText(activity, "Vui lòng chọn số sao", Toast.LENGTH_SHORT).show();
                return;
            }

            String token = UserManager.getAuthToken();
            if (token == null) {
                Toast.makeText(activity, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
                return;
            }

            String comment = edtComment.getText().toString().trim();
            ReviewRequest request = new ReviewRequest(productId, selectedRating[0], comment);

            String authHeader = "Bearer " + token;
            ApiClient.getApiService().createReview(authHeader, request).enqueue(new Callback<ReviewResponse>() {
                @Override
                public void onResponse(Call<ReviewResponse> call, Response<ReviewResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Toast.makeText(activity, "Đánh giá thành công!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        String errorMsg = "Lỗi đánh giá";
                        if (response.errorBody() != null) {
                            try {
                                String errorStr = response.errorBody().string();
                                com.google.gson.JsonObject jsonObject = new com.google.gson.Gson().fromJson(errorStr, com.google.gson.JsonObject.class);
                                if (jsonObject.has("message")) {
                                    errorMsg = jsonObject.get("message").getAsString();
                                }
                            } catch (Exception e) {
                                errorMsg += ": " + response.message();
                            }
                        }
                        Toast.makeText(activity, errorMsg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<ReviewResponse> call, Throwable t) {
                    Toast.makeText(activity, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void updateStars(ImageView[] stars, int rating) {
        for (int i = 0; i < stars.length; i++) {
            if (i < rating) {
                stars[i].setImageResource(R.drawable.ic_star);
            } else {
                stars[i].setImageResource(R.drawable.ic_star_empty);
            }
        }
    }
}


