package fpl.manhph61584.duan1_nhom3_app;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.content.Intent;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import fpl.manhph61584.duan1_nhom3_app.network.ApiClient;
import fpl.manhph61584.duan1_nhom3_app.network.ApiService;
import fpl.manhph61584.duan1_nhom3_app.network.dto.AddToCartRequest;
import fpl.manhph61584.duan1_nhom3_app.network.dto.CartResponse;
import fpl.manhph61584.duan1_nhom3_app.network.dto.ProductRatingResponse;
import fpl.manhph61584.duan1_nhom3_app.network.dto.ReviewRequest;
import fpl.manhph61584.duan1_nhom3_app.network.dto.ReviewResponse;
import fpl.manhph61584.duan1_nhom3_app.network.dto.UserDto;
import fpl.manhph61584.duan1_nhom3_app.Product;
import fpl.manhph61584.duan1_nhom3_app.Review;
import fpl.manhph61584.duan1_nhom3_app.UserManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetailProductActivity extends AppCompatActivity {

    private ImageView imgProduct, btnPlus, btnMinus;
    private TextView txtName, txtPrice, txtDesc, txtQuantity, txtTotalPrice, txtStock;
    private TextView txtAverageRating, txtTotalReviews;
    private Button btnAddToCart, btnGoToCart, btnAddReview;
    private LinearLayout layoutColors, layoutSizes;
    private LinearLayout layoutReviews, layoutAverageRating;

    private int quantity = 1;
    private String selectedColor = "";
    private String selectedSize = "";
    private Product currentProduct;
    private double unitPrice = 0;
    private String productId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_detail_product);

            imgProduct = findViewById(R.id.imgProduct);
            txtName = findViewById(R.id.txtName);
            txtPrice = findViewById(R.id.txtPrice);
            txtTotalPrice = findViewById(R.id.txtTotalPrice);
            txtDesc = findViewById(R.id.txtDesc);
            txtQuantity = findViewById(R.id.txtQuantity);
            txtStock = findViewById(R.id.txtStock);
            btnPlus = findViewById(R.id.btnPlus);
            btnMinus = findViewById(R.id.btnMinus);
            btnAddToCart = findViewById(R.id.btnAddToCart);
            btnGoToCart = findViewById(R.id.btnGoToCart);
            layoutColors = findViewById(R.id.layoutColors);
            layoutSizes = findViewById(R.id.layoutSizes);
            layoutReviews = findViewById(R.id.layoutReviews);
            layoutAverageRating = findViewById(R.id.layoutAverageRating);
            txtAverageRating = findViewById(R.id.txtAverageRating);
            txtTotalReviews = findViewById(R.id.txtTotalReviews);

            productId = getIntent().getStringExtra("id");
            if (productId == null || productId.isEmpty()) {
                Toast.makeText(this, "Lỗi: Không có ID sản phẩm", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            loadProduct(productId);
            // Load reviews từ MongoDB ngay khi mở màn hình
            loadReviews(productId);
            loadProductRating(productId);
            handleQuantity();
            handleActions();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload reviews khi quay lại màn hình để đảm bảo hiển thị đầy đủ reviews từ MongoDB
        if (productId != null && !productId.isEmpty()) {
            android.util.Log.d("ReviewLoad", "onResume: Reloading reviews");
            loadReviews(productId);
            loadProductRating(productId);
        }
    }

    private void loadProduct(String id) {
        ApiClient.getApiService().getProductDetail(id).enqueue(new Callback<Product>() {
            @Override
            public void onResponse(Call<Product> call, Response<Product> res) {
                if (!res.isSuccessful() || res.body() == null) return;

                Product p = res.body();
                currentProduct = p;
                unitPrice = p.getPrice();

                txtName.setText(p.getName());
                txtPrice.setText(unitPrice + "₫");
                txtDesc.setText(p.getDescription());
                
                // Hiển thị số lượng còn (tổng stock ban đầu)
                int stock = p.getStock();
                txtStock.setText(String.valueOf(stock));
                if (stock <= 0) {
                    txtStock.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    txtStock.setText("Hết hàng");
                } else {
                    txtStock.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                }
                
                // Kiểm tra và cập nhật trạng thái nút mua
                updateBuyButtonState();
                updateTotalPrice();
                
                // Cập nhật stock theo variant nếu đã chọn màu/size
                if ((selectedColor != null && !selectedColor.isEmpty()) || (selectedSize != null && !selectedSize.isEmpty())) {
                    updateStockForVariant();
                }

                String imageUrl = p.getImage();
                if (imageUrl != null && imageUrl.startsWith("/uploads/")) {
                    imageUrl = "http://10.0.2.2:3000" + imageUrl;
                }

                Glide.with(DetailProductActivity.this)
                        .load(imageUrl)
                        .into(imgProduct);

                layoutColors.removeAllViews();
                layoutSizes.removeAllViews();

                // Hiển thị màu sắc từ server
                String[] colors = p.getColors();
                if (colors != null && colors.length > 0) {
                    for (String color : colors) {
                        if (color != null && !color.trim().isEmpty()) {
                            addColor(color.trim());
                        }
                    }
                } else {
                    // Nếu không có màu, thêm màu mặc định
                    addColor("Mặc định");
                }

                // Hiển thị size từ server
                String[] sizes = p.getSizes();
                if (sizes != null && sizes.length > 0) {
                    for (String size : sizes) {
                        if (size != null && !size.trim().isEmpty()) {
                            addSize(size.trim());
                        }
                    }
                } else {
                    // Nếu không có size, thêm size mặc định
                    addSize("Free size");
                }
            }

            @Override
            public void onFailure(Call<Product> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void addColor(String color) {
        TextView tv = buildOptionTextView(color);
        tv.setOnClickListener(v -> selectColorOption(tv, color));
        layoutColors.addView(tv);

        if (TextUtils.isEmpty(selectedColor)) {
            selectColorOption(tv, color);
        }
    }

    private void addSize(String size) {
        TextView tv = buildOptionTextView(size);
        tv.setOnClickListener(v -> selectSizeOption(tv, size));
        layoutSizes.addView(tv);

        if (TextUtils.isEmpty(selectedSize)) {
            selectSizeOption(tv, size);
        }
    }

    private TextView buildOptionTextView(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(14);
        tv.setPadding(30, 10, 30, 10);
        tv.setBackgroundResource(R.drawable.bg_unselect);
        tv.setTextColor(Color.parseColor("#666666"));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(16, 8, 16, 8);
        tv.setLayoutParams(params);
        return tv;
    }

    private void selectColorOption(TextView tv, String color) {
        selectedColor = color;
        resetSelection(layoutColors);
        styleSelectedOption(tv, true);
        updateStockForVariant();
    }

    private void selectSizeOption(TextView tv, String size) {
        selectedSize = size;
        resetSelection(layoutSizes);
        styleSelectedOption(tv, true);
        updateStockForVariant();
    }

    private void resetSelection(LinearLayout layout) {
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof TextView) {
                styleSelectedOption((TextView) child, false);
            }
        }
    }

    private void styleSelectedOption(TextView tv, boolean selected) {
        tv.setBackgroundResource(selected ? R.drawable.bg_selected : R.drawable.bg_unselect);
        tv.setTextColor(selected ? Color.parseColor("#000000") : Color.parseColor("#666666"));
    }
    
    private void updateStockForVariant() {
        if (currentProduct == null || currentProduct.getId() == null) return;
        
        String color = selectedColor != null && !selectedColor.isEmpty() ? selectedColor : null;
        String size = selectedSize != null && !selectedSize.isEmpty() ? selectedSize : null;
        
        ApiClient.getApiService().getProductStock(currentProduct.getId(), color, size).enqueue(new Callback<fpl.manhph61584.duan1_nhom3_app.network.dto.StockResponse>() {
            @Override
            public void onResponse(Call<fpl.manhph61584.duan1_nhom3_app.network.dto.StockResponse> call, Response<fpl.manhph61584.duan1_nhom3_app.network.dto.StockResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int stock = response.body().getStock();
                    txtStock.setText(String.valueOf(stock));
                    if (stock <= 0) {
                        txtStock.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        txtStock.setText("Hết hàng");
                    } else {
                        txtStock.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    }
                    updateBuyButtonState();
                }
            }

            @Override
            public void onFailure(Call<fpl.manhph61584.duan1_nhom3_app.network.dto.StockResponse> call, Throwable t) {
                // Nếu lỗi, giữ nguyên stock tổng
            }
        });
    }

    private void handleQuantity() {
        btnPlus.setOnClickListener(v -> {
            if (currentProduct != null) {
                // Lấy stock từ text view (đã được cập nhật theo variant)
                String stockText = txtStock.getText().toString();
                int stock;
                if (stockText.equals("Hết hàng")) {
                    stock = 0;
                } else {
                    try {
                        stock = Integer.parseInt(stockText);
                    } catch (NumberFormatException e) {
                        stock = currentProduct.getStock(); // Fallback về stock tổng
                    }
                }
                
                if (quantity < stock) {
                    quantity++;
                    txtQuantity.setText(String.valueOf(quantity));
                    updateTotalPrice();
                    updateBuyButtonState();
                } else {
                    Toast.makeText(this, "Số lượng vượt quá số lượng còn lại (" + stock + ")", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnMinus.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                txtQuantity.setText(String.valueOf(quantity));
                updateTotalPrice();
                updateBuyButtonState();
            }
        });
    }
    
    private void updateBuyButtonState() {
        if (currentProduct == null) {
            btnAddToCart.setEnabled(false);
            btnGoToCart.setEnabled(false);
            return;
        }
        
        // Lấy stock từ text view (đã được cập nhật theo variant)
        String stockText = txtStock.getText().toString();
        int stock;
        if (stockText.equals("Hết hàng")) {
            stock = 0;
        } else {
            try {
                stock = Integer.parseInt(stockText);
            } catch (NumberFormatException e) {
                stock = currentProduct.getStock(); // Fallback về stock tổng
            }
        }
        
        boolean canBuy = stock > 0 && quantity <= stock;
        
        btnAddToCart.setEnabled(canBuy);
        btnGoToCart.setEnabled(canBuy);
        
        if (!canBuy) {
            if (stock <= 0) {
                btnAddToCart.setText("Hết hàng");
                btnGoToCart.setText("Hết hàng");
            } else {
                btnAddToCart.setText("Thêm vào giỏ");
                btnGoToCart.setText("Mua ngay");
            }
        } else {
            btnAddToCart.setText("Thêm vào giỏ");
            btnGoToCart.setText("Mua ngay");
        }
    }

    private void updateTotalPrice() {
        double total = unitPrice * quantity;
        txtTotalPrice.setText(String.format("%.0f₫", total));
    }

    private void handleActions() {
        btnAddToCart.setOnClickListener(v -> {
            if (currentProduct == null) {
                Toast.makeText(this, "Đang tải dữ liệu sản phẩm...", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Kiểm tra số lượng còn (theo variant nếu có)
            String stockText = txtStock.getText().toString();
            int stock;
            if (stockText.equals("Hết hàng")) {
                stock = 0;
            } else {
                try {
                    stock = Integer.parseInt(stockText);
                } catch (NumberFormatException e) {
                    stock = currentProduct.getStock(); // Fallback về stock tổng
                }
            }
            
            if (stock <= 0) {
                Toast.makeText(this, "Sản phẩm đã hết hàng", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (quantity > stock) {
                Toast.makeText(this, "Số lượng vượt quá số lượng còn lại (" + stock + ")", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Kiểm tra đăng nhập
            String token = UserManager.getAuthToken();
            if (token == null) {
                Toast.makeText(this, "Vui lòng đăng nhập để thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Gọi API để lưu vào server MongoDB trước
            String safeColor = selectedColor == null || selectedColor.isEmpty() ? "Mặc định" : selectedColor;
            String safeSize = selectedSize == null || selectedSize.isEmpty() ? "Free size" : selectedSize;
            AddToCartRequest request = new AddToCartRequest(currentProduct.getId(), quantity, safeColor, safeSize);
            String authHeader = "Bearer " + token;
            
            ApiClient.getApiService().addToCart(authHeader, request).enqueue(new Callback<CartResponse>() {
                @Override
                public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        // Thêm vào local cart sau khi server thành công
                        CartManager.addToCart(currentProduct, quantity, safeColor, safeSize);
                        Toast.makeText(DetailProductActivity.this, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
                    } else {
                        // Vẫn thêm vào local cart nếu server lỗi
                        CartManager.addToCart(currentProduct, quantity, safeColor, safeSize);
                        Toast.makeText(DetailProductActivity.this, "Đã thêm vào giỏ hàng (offline)", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<CartResponse> call, Throwable t) {
                    // Vẫn thêm vào local cart nếu server lỗi
                    CartManager.addToCart(currentProduct, quantity, safeColor, safeSize);
                    Toast.makeText(DetailProductActivity.this, "Đã thêm vào giỏ hàng (offline)", Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnGoToCart.setOnClickListener(v -> {
            if (currentProduct == null) {
                Toast.makeText(this, "Đang tải dữ liệu sản phẩm...", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Kiểm tra số lượng còn (theo variant nếu có)
            String stockText = txtStock.getText().toString();
            int stock;
            if (stockText.equals("Hết hàng")) {
                stock = 0;
            } else {
                try {
                    stock = Integer.parseInt(stockText);
                } catch (NumberFormatException e) {
                    stock = currentProduct.getStock(); // Fallback về stock tổng
                }
            }
            
            if (stock <= 0) {
                Toast.makeText(this, "Sản phẩm đã hết hàng", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (quantity > stock) {
                Toast.makeText(this, "Số lượng vượt quá số lượng còn lại (" + stock + ")", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Mua ngay: truyền thông tin sản phẩm qua Intent
            android.content.Intent intent = new android.content.Intent(this, CartActivity.class);
            intent.putExtra("buy_now", true);
            intent.putExtra("product_id", currentProduct.getId());
            intent.putExtra("quantity", quantity);
            intent.putExtra("color", selectedColor == null || selectedColor.isEmpty() ? "Mặc định" : selectedColor);
            intent.putExtra("size", selectedSize == null || selectedSize.isEmpty() ? "Free size" : selectedSize);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

    }

    private void loadReviews(String productId) {
        android.util.Log.d("ReviewLoad", "========================================");
        android.util.Log.d("ReviewLoad", "Loading reviews for productId: " + productId);
        android.util.Log.d("ReviewLoad", "API URL: http://10.0.2.2:3000/api/reviews/product/" + productId);
        
        ApiClient.getApiService().getProductReviews(productId).enqueue(new Callback<List<Review>>() {
            @Override
            public void onResponse(Call<List<Review>> call, Response<List<Review>> response) {
                android.util.Log.d("ReviewLoad", "Response code: " + response.code());
                android.util.Log.d("ReviewLoad", "Response isSuccessful: " + response.isSuccessful());
                android.util.Log.d("ReviewLoad", "Response body is null: " + (response.body() == null));
                
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        List<Review> reviews = response.body();
                        android.util.Log.d("ReviewLoad", "✅ Successfully loaded " + reviews.size() + " reviews from MongoDB");
                        
                        if (reviews.size() > 0) {
                            Review firstReview = reviews.get(0);
                            android.util.Log.d("ReviewLoad", "First review ID: " + firstReview.getId());
                            android.util.Log.d("ReviewLoad", "First review rating: " + firstReview.getRating());
                            android.util.Log.d("ReviewLoad", "First review comment: " + firstReview.getComment());
                            android.util.Log.d("ReviewLoad", "First review user: " + (firstReview.getUser() != null ? firstReview.getUser().getName() : "null"));
                        } else {
                            android.util.Log.d("ReviewLoad", "⚠️ No reviews found in response");
                        }
                        
                        displayReviews(reviews);
                    } else {
                        android.util.Log.e("ReviewLoad", "❌ Response body is null!");
                        displayReviews(new java.util.ArrayList<>());
                    }
                } else {
                    android.util.Log.e("ReviewLoad", "❌ Failed to load reviews: " + response.code() + " - " + response.message());
                    if (response.errorBody() != null) {
                        try {
                            String errorStr = response.errorBody().string();
                            android.util.Log.e("ReviewLoad", "Error body: " + errorStr);
                        } catch (Exception e) {
                            android.util.Log.e("ReviewLoad", "Error reading error body", e);
                        }
                    }
                    displayReviews(new java.util.ArrayList<>());
                }
                android.util.Log.d("ReviewLoad", "========================================");
            }

            @Override
            public void onFailure(Call<List<Review>> call, Throwable t) {
                android.util.Log.e("ReviewLoad", "❌❌❌ ERROR loading reviews from server ❌❌❌");
                android.util.Log.e("ReviewLoad", "Error message: " + t.getMessage());
                android.util.Log.e("ReviewLoad", "Error class: " + t.getClass().getName());
                if (t.getCause() != null) {
                    android.util.Log.e("ReviewLoad", "Cause: " + t.getCause().getMessage());
                }
                t.printStackTrace();
                displayReviews(new java.util.ArrayList<>());
                android.util.Log.d("ReviewLoad", "========================================");
            }
        });
    }

    private void loadProductRating(String productId) {
        android.util.Log.d("RatingLoad", "========================================");
        android.util.Log.d("RatingLoad", "Loading rating for productId: " + productId);
        android.util.Log.d("RatingLoad", "API URL: http://10.0.2.2:3000/api/reviews/product/" + productId + "/rating");
        
        ApiClient.getApiService().getProductRating(productId).enqueue(new Callback<ProductRatingResponse>() {
            @Override
            public void onResponse(Call<ProductRatingResponse> call, Response<ProductRatingResponse> response) {
                android.util.Log.d("RatingLoad", "Response code: " + response.code());
                android.util.Log.d("RatingLoad", "Response isSuccessful: " + response.isSuccessful());
                android.util.Log.d("RatingLoad", "Response body is null: " + (response.body() == null));
                
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        ProductRatingResponse rating = response.body();
                        android.util.Log.d("RatingLoad", "✅ Rating loaded from MongoDB:");
                        android.util.Log.d("RatingLoad", "  - Average rating: " + rating.getAverageRating());
                        android.util.Log.d("RatingLoad", "  - Total reviews: " + rating.getTotalReviews());
                        displayRating(rating.getAverageRating(), rating.getTotalReviews());
                    } else {
                        android.util.Log.e("RatingLoad", "❌ Response body is null!");
                        displayRating(0.0, 0);
                    }
                } else {
                    android.util.Log.e("RatingLoad", "❌ Failed to load rating: " + response.code() + " - " + response.message());
                    if (response.errorBody() != null) {
                        try {
                            String errorStr = response.errorBody().string();
                            android.util.Log.e("RatingLoad", "Error body: " + errorStr);
                        } catch (Exception e) {
                            android.util.Log.e("RatingLoad", "Error reading error body", e);
                        }
                    }
                    displayRating(0.0, 0);
                }
                android.util.Log.d("RatingLoad", "========================================");
            }

            @Override
            public void onFailure(Call<ProductRatingResponse> call, Throwable t) {
                android.util.Log.e("RatingLoad", "❌❌❌ ERROR loading rating ❌❌❌");
                android.util.Log.e("RatingLoad", "Error message: " + t.getMessage());
                android.util.Log.e("RatingLoad", "Error class: " + t.getClass().getName());
                if (t.getCause() != null) {
                    android.util.Log.e("RatingLoad", "Cause: " + t.getCause().getMessage());
                }
                t.printStackTrace();
                displayRating(0.0, 0);
                android.util.Log.d("RatingLoad", "========================================");
            }
        });
    }

    private void displayRating(double averageRating, int totalReviews) {
        txtAverageRating.setText(String.format(Locale.getDefault(), "%.1f", averageRating));
        txtTotalReviews.setText(String.format("(%d đánh giá)", totalReviews));

        layoutAverageRating.removeAllViews();
        int fullStars = (int) averageRating;
        boolean hasHalfStar = (averageRating - fullStars) >= 0.5;

        for (int i = 0; i < 5; i++) {
            ImageView star = new ImageView(this);
            star.setLayoutParams(new LinearLayout.LayoutParams(24, 24));
            star.setPadding(2, 0, 2, 0);
            if (i < fullStars) {
                star.setImageResource(R.drawable.ic_star);
            } else if (i == fullStars && hasHalfStar) {
                star.setImageResource(R.drawable.ic_star);
            } else {
                star.setImageResource(R.drawable.ic_star_empty);
            }
            layoutAverageRating.addView(star);
        }
    }

    private void displayReviews(List<Review> reviews) {
        android.util.Log.d("ReviewDisplay", "Displaying " + reviews.size() + " reviews");
        android.util.Log.d("ReviewDisplay", "Current views before removeAllViews: " + layoutReviews.getChildCount());
        layoutReviews.removeAllViews();
        android.util.Log.d("ReviewDisplay", "Views after removeAllViews: " + layoutReviews.getChildCount());
        
        if (reviews.isEmpty()) {
            TextView noReviews = new TextView(this);
            noReviews.setText("Chưa có đánh giá nào");
            noReviews.setTextSize(14);
            noReviews.setTextColor(0xFF999999);
            noReviews.setPadding(16, 16, 16, 16);
            layoutReviews.addView(noReviews);
            android.util.Log.d("ReviewDisplay", "Added 'No reviews' text");
            return;
        }

        for (Review review : reviews) {
            View reviewView = LayoutInflater.from(this).inflate(R.layout.item_review, layoutReviews, false);
            
            TextView txtUserName = reviewView.findViewById(R.id.txtReviewUserName);
            TextView txtComment = reviewView.findViewById(R.id.txtReviewComment);
            TextView txtDate = reviewView.findViewById(R.id.txtReviewDate);
            LinearLayout layoutStars = reviewView.findViewById(R.id.layoutStars);

            if (review.getUser() != null) {
                txtUserName.setText(review.getUser().getName() != null ? review.getUser().getName() : "Người dùng");
            } else {
                txtUserName.setText("Người dùng");
            }

            txtComment.setText(review.getComment() != null ? review.getComment() : "");

            // Format date
            if (review.getCreatedAt() != null) {
                String dateStr = review.getCreatedAt();
                try {
                    // Try ISO 8601 format
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                    Date date = inputFormat.parse(dateStr);
                    SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    txtDate.setText(outputFormat.format(date));
                } catch (Exception e1) {
                    try {
                        // Try without milliseconds
                        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                        Date date = inputFormat.parse(dateStr);
                        SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        txtDate.setText(outputFormat.format(date));
                    } catch (Exception e2) {
                        // If all parsing fails, just show the original string
                        if (dateStr.length() > 10) {
                            txtDate.setText(dateStr.substring(0, 10));
                        } else {
                            txtDate.setText(dateStr);
                        }
                    }
                }
            } else {
                txtDate.setText("");
            }

            // Display stars
            layoutStars.removeAllViews();
            int rating = review.getRating();
            for (int i = 0; i < 5; i++) {
                ImageView star = new ImageView(this);
                star.setLayoutParams(new LinearLayout.LayoutParams(20, 20));
                star.setPadding(2, 0, 2, 0);
                if (i < rating) {
                    star.setImageResource(R.drawable.ic_star);
                } else {
                    star.setImageResource(R.drawable.ic_star_empty);
                }
                layoutStars.addView(star);
            }

            // Display admin reply if exists
            LinearLayout layoutAdminReply = reviewView.findViewById(R.id.layoutAdminReply);
            TextView txtAdminReply = reviewView.findViewById(R.id.txtAdminReply);
            if (review.getAdminReply() != null && !review.getAdminReply().trim().isEmpty()) {
                layoutAdminReply.setVisibility(View.VISIBLE);
                txtAdminReply.setText(review.getAdminReply());
            } else {
                layoutAdminReply.setVisibility(View.GONE);
            }

            // Ẩn admin actions (chỉ dành cho khách hàng)
            LinearLayout layoutAdminActions = reviewView.findViewById(R.id.layoutAdminActions);
            if (layoutAdminActions != null) {
                layoutAdminActions.setVisibility(View.GONE);
            }

            layoutReviews.addView(reviewView);
            android.util.Log.d("ReviewDisplay", "Added review view for: " + (review.getUser() != null ? review.getUser().getName() : "Unknown"));
        }
        android.util.Log.d("ReviewDisplay", "Total views after displaying all reviews: " + layoutReviews.getChildCount());
    }

    private void showReviewDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_review, null);
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
                Toast.makeText(this, "Vui lòng chọn số sao", Toast.LENGTH_SHORT).show();
                return;
            }
            String comment = edtComment.getText().toString().trim();
            // Cho phép comment rỗng (optional)
            if (comment.isEmpty()) {
                comment = ""; // Gửi empty string thay vì null
            }

            submitReview(selectedRating[0], comment);
            dialog.dismiss();
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

    private void submitReview(int rating, String comment) {
        String token = UserManager.getAuthToken();
        if (token == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        ReviewRequest request = new ReviewRequest(productId, rating, comment);
        String authHeader = "Bearer " + token;

        android.util.Log.d("ReviewSubmit", "Submitting review: productId=" + productId + ", rating=" + rating + ", comment=" + comment);

        ApiClient.getApiService().createReview(authHeader, request).enqueue(new Callback<ReviewResponse>() {
            @Override
            public void onResponse(Call<ReviewResponse> call, Response<ReviewResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ReviewResponse reviewResponse = response.body();
                    Review newReview = reviewResponse.getReview();
                    
                    if (newReview != null) {
                        // Hiển thị đánh giá mới ngay lập tức trong danh sách dưới "Tất cả đánh giá"
                        // Giống như các ứng dụng mua sắm hiện nay
                        addReviewToTop(newReview);
                        android.util.Log.d("ReviewSubmit", "Review added to top, total views: " + layoutReviews.getChildCount());
                        Toast.makeText(DetailProductActivity.this, "Đánh giá thành công!", Toast.LENGTH_SHORT).show();
                        
                        // Cập nhật rating ngay lập tức
                        loadProductRating(productId);
                        
                        // Không reload reviews ngay để review vừa thêm không bị mất
                        // Chỉ reload sau 5 giây để đồng bộ với server (review đã được lưu vào MongoDB)
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            android.util.Log.d("ReviewReload", "Reloading reviews after successful submit to sync with server");
                            loadReviews(productId);
                        }, 5000);
                    } else {
                        // Nếu không có review trong response, reload ngay
                        Toast.makeText(DetailProductActivity.this, "Đánh giá thành công!", Toast.LENGTH_SHORT).show();
                        loadReviews(productId);
                        loadProductRating(productId);
                    }
                } else {
                    // Hiển thị error message chi tiết
                    String errorMessage = "Lỗi: ";
                    try {
                        if (response.errorBody() != null) {
                            okhttp3.ResponseBody errorBody = response.errorBody();
                            String errorStr = errorBody.string();
                            android.util.Log.e("ReviewError", "Error body: " + errorStr);
                            
                            // Thử parse JSON error message
                            try {
                                com.google.gson.JsonObject jsonObject = new com.google.gson.Gson().fromJson(errorStr, com.google.gson.JsonObject.class);
                                if (jsonObject.has("message")) {
                                    errorMessage += jsonObject.get("message").getAsString();
                                } else if (jsonObject.has("error")) {
                                    errorMessage += jsonObject.get("error").getAsString();
                                } else {
                                    errorMessage += errorStr;
                                }
                            } catch (Exception e) {
                                errorMessage += errorStr;
                            }
                        } else {
                            errorMessage += response.message() + " (Code: " + response.code() + ")";
                        }
                    } catch (Exception e) {
                        errorMessage += response.message() + " (Code: " + response.code() + ")";
                        android.util.Log.e("ReviewError", "Exception reading error body", e);
                    }
                    Toast.makeText(DetailProductActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    android.util.Log.e("ReviewError", "Error response: " + response.code() + " - " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ReviewResponse> call, Throwable t) {
                Toast.makeText(DetailProductActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                t.printStackTrace();
            }
        });
    }

    private void addReviewToTop(Review review) {
        android.util.Log.d("ReviewAdd", "Adding review to top: " + (review.getUser() != null ? review.getUser().getName() : "Unknown"));
        
        // Xóa text "Chưa có đánh giá nào" nếu có
        if (layoutReviews.getChildCount() > 0) {
            View firstChild = layoutReviews.getChildAt(0);
            if (firstChild instanceof TextView) {
                TextView textView = (TextView) firstChild;
                if ("Chưa có đánh giá nào".equals(textView.getText().toString())) {
                    layoutReviews.removeViewAt(0);
                    android.util.Log.d("ReviewAdd", "Removed 'No reviews' text");
                }
            }
        }

        // Tạo view cho review mới
        View reviewView = LayoutInflater.from(this).inflate(R.layout.item_review, layoutReviews, false);
        
        TextView txtUserName = reviewView.findViewById(R.id.txtReviewUserName);
        TextView txtComment = reviewView.findViewById(R.id.txtReviewComment);
        TextView txtDate = reviewView.findViewById(R.id.txtReviewDate);
        LinearLayout layoutStars = reviewView.findViewById(R.id.layoutStars);

        // Hiển thị tên người dùng
        if (review.getUser() != null && review.getUser().getName() != null) {
            txtUserName.setText(review.getUser().getName());
        } else {
            UserDto currentUser = UserManager.getCurrentUser();
            txtUserName.setText(currentUser != null && currentUser.getName() != null ? currentUser.getName() : "Người dùng");
        }

        // Hiển thị nội dung đánh giá
        txtComment.setText(review.getComment() != null ? review.getComment() : "");

        // Hiển thị ngày tháng
        if (review.getCreatedAt() != null) {
            String dateStr = review.getCreatedAt();
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                Date date = inputFormat.parse(dateStr);
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                txtDate.setText(outputFormat.format(date));
            } catch (Exception e1) {
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                    Date date = inputFormat.parse(dateStr);
                    SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    txtDate.setText(outputFormat.format(date));
                } catch (Exception e2) {
                    SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    txtDate.setText(outputFormat.format(new Date()));
                }
            }
        } else {
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            txtDate.setText(outputFormat.format(new Date()));
        }

        // Hiển thị số sao
        layoutStars.removeAllViews();
        int rating = review.getRating();
        for (int i = 0; i < 5; i++) {
            ImageView star = new ImageView(this);
            star.setLayoutParams(new LinearLayout.LayoutParams(20, 20));
            star.setPadding(2, 0, 2, 0);
            if (i < rating) {
                star.setImageResource(R.drawable.ic_star);
            } else {
                star.setImageResource(R.drawable.ic_star_empty);
            }
            layoutStars.addView(star);
        }

        // Ẩn admin reply và actions (chỉ dành cho khách hàng)
        LinearLayout layoutAdminReply = reviewView.findViewById(R.id.layoutAdminReply);
        LinearLayout layoutAdminActions = reviewView.findViewById(R.id.layoutAdminActions);
        if (layoutAdminReply != null) {
            layoutAdminReply.setVisibility(View.GONE);
        }
        if (layoutAdminActions != null) {
            layoutAdminActions.setVisibility(View.GONE);
        }

        // Thêm vào đầu danh sách (dưới "Tất cả đánh giá")
        layoutReviews.addView(reviewView, 0);
        android.util.Log.d("ReviewAdd", "Review view added at position 0, total views now: " + layoutReviews.getChildCount());
    }

    // Admin functions removed - chỉ dành cho khách hàng
}
