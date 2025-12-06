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

import fpl.manhph61584.duan1_nhom3_app.network.ApiClient;
import fpl.manhph61584.duan1_nhom3_app.network.dto.OrderItemDto;
import fpl.manhph61584.duan1_nhom3_app.network.dto.ReviewRequest;
import fpl.manhph61584.duan1_nhom3_app.network.dto.ReviewResponse;
import fpl.manhph61584.duan1_nhom3_app.UserManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeliveredItemsAdapter extends RecyclerView.Adapter<DeliveredItemsAdapter.ItemViewHolder> {

    private final android.content.Context context;
    private List<OrderItemDto> items;

    public DeliveredItemsAdapter(android.content.Context context, List<OrderItemDto> items) {
        this.context = context;
        this.items = new ArrayList<>(items);
    }

    public void updateItems(List<OrderItemDto> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_order_status, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        OrderItemDto item = items.get(position);
        
        if (item.getProduct() != null) {
            Product product = item.getProduct();
            holder.txtName.setText(product.getName());
            holder.txtVariant.setText("Màu: " + (item.getColor() != null ? item.getColor() : "Mặc định") + 
                                     " | Size: " + (item.getSize() != null ? item.getSize() : "Free size"));
            holder.txtQuantity.setText("Số lượng: " + item.getQuantity());
            
            double itemTotal = item.getPrice() * item.getQuantity();
            holder.txtPrice.setText(String.format("%,.0f₫", itemTotal));

            // Load ảnh
            String imageUrl = product.getImage();
            if (imageUrl != null && imageUrl.startsWith("/uploads/")) {
                imageUrl = "http://10.0.2.2:3000" + imageUrl;
            }
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(holder.imgProduct);

            // Setup button "Đánh giá"
            holder.btnReview.setOnClickListener(v -> {
                if (product.getId() != null) {
                    showReviewDialog(product.getId());
                } else {
                    Toast.makeText(context, "Không tìm thấy thông tin sản phẩm", Toast.LENGTH_SHORT).show();
                }
            });

            // Setup button "Mua lại"
            holder.btnBuyAgain.setOnClickListener(v -> {
                String token = UserManager.getAuthToken();
                if (token == null) {
                    Toast.makeText(context, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (product.getId() == null) {
                    Toast.makeText(context, "Không tìm thấy thông tin sản phẩm", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Chuyển đến CartActivity với chế độ "Mua ngay"
                Intent intent = new Intent(context, CartActivity.class);
                intent.putExtra("buy_now", true);
                intent.putExtra("product_id", product.getId());
                intent.putExtra("quantity", item.getQuantity());
                intent.putExtra("color", item.getColor() != null ? item.getColor() : "Mặc định");
                intent.putExtra("size", item.getSize() != null ? item.getSize() : "Free size");
                context.startActivity(intent);
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).overridePendingTransition(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                    );
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView txtName, txtVariant, txtQuantity, txtPrice;
        Button btnReview, btnBuyAgain;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.orderImgProduct);
            txtName = itemView.findViewById(R.id.orderTxtName);
            txtVariant = itemView.findViewById(R.id.orderTxtVariant);
            txtQuantity = itemView.findViewById(R.id.orderTxtQuantity);
            txtPrice = itemView.findViewById(R.id.orderTxtPrice);
            btnReview = itemView.findViewById(R.id.btnReview);
            btnBuyAgain = itemView.findViewById(R.id.btnBuyAgain);
        }
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

