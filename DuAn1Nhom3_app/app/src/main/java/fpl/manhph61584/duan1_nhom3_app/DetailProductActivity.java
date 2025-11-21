package fpl.manhph61584.duan1_nhom3_app;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import fpl.manhph61584.duan1_nhom3_app.R;
import fpl.manhph61584.duan1_nhom3_app.network.ApiClient;
import fpl.manhph61584.duan1_nhom3_app.network.ApiService;
import fpl.manhph61584.duan1_nhom3_app.Product;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetailProductActivity extends AppCompatActivity {

    private ImageView imgProduct, btnPlus, btnMinus;
    private TextView txtName, txtPrice, txtDesc, txtQuantity, txtTotalPrice;
    private Button btnAddToCart, btnGoToCart;
    private LinearLayout layoutColors, layoutSizes;

    private int quantity = 1;
    private String selectedColor = "";
    private String selectedSize = "";
    private Product currentProduct;
    private double unitPrice = 0;

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
            btnPlus = findViewById(R.id.btnPlus);
            btnMinus = findViewById(R.id.btnMinus);
            btnAddToCart = findViewById(R.id.btnAddToCart);
            btnGoToCart = findViewById(R.id.btnGoToCart);
            layoutColors = findViewById(R.id.layoutColors);
            layoutSizes = findViewById(R.id.layoutSizes);

            String id = getIntent().getStringExtra("id");
            if (id == null || id.isEmpty()) {
                Toast.makeText(this, "Lỗi: Không có ID sản phẩm", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            loadProduct(id);
            handleQuantity();
            handleActions();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
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
                updateTotalPrice();

                String imageUrl = p.getImage();
                if (imageUrl != null && imageUrl.startsWith("/uploads/")) {
                    imageUrl = "http://10.0.2.2:3000" + imageUrl;
                }

                Glide.with(DetailProductActivity.this)
                        .load(imageUrl)
                        .into(imgProduct);

                layoutColors.removeAllViews();
                layoutSizes.removeAllViews();

                // Fake màu + size tạm thời
                addColor("Đỏ");
                addColor("Đen");
                addColor("Xanh");

                addSize("S");
                addSize("M");
                addSize("L");
            }

            @Override
            public void onFailure(Call<Product> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void addColor(String color) {
        TextView tv = new TextView(this);
        tv.setText(color);
        tv.setPadding(30, 10, 30, 10);
        tv.setBackgroundResource(R.drawable.bg_unselect);
        tv.setTextSize(14);
        tv.setOnClickListener(v -> {
            selectedColor = color;
            resetSelection(layoutColors);
            tv.setBackgroundResource(R.drawable.bg_selected);
        });
        layoutColors.addView(tv);
    }

    private void addSize(String size) {
        TextView tv = new TextView(this);
        tv.setText(size);
        tv.setPadding(30, 10, 30, 10);
        tv.setBackgroundResource(R.drawable.bg_unselect);
        tv.setTextSize(14);
        tv.setOnClickListener(v -> {
            selectedSize = size;
            resetSelection(layoutSizes);
            tv.setBackgroundResource(R.drawable.bg_selected);
        });
        layoutSizes.addView(tv);
    }

    private void resetSelection(LinearLayout layout) {
        for (int i = 0; i < layout.getChildCount(); i++) {
            layout.getChildAt(i).setBackgroundResource(R.drawable.bg_unselect);
        }
    }

    private void handleQuantity() {
        btnPlus.setOnClickListener(v -> {
            quantity++;
            txtQuantity.setText(String.valueOf(quantity));
            updateTotalPrice();
        });

        btnMinus.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                txtQuantity.setText(String.valueOf(quantity));
                updateTotalPrice();
            }
        });
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
            CartManager.addToCart(currentProduct, quantity, selectedColor, selectedSize);
            Toast.makeText(this, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
        });

        btnGoToCart.setOnClickListener(v ->
                startActivity(new android.content.Intent(this, CartActivity.class))
        );
    }
}
