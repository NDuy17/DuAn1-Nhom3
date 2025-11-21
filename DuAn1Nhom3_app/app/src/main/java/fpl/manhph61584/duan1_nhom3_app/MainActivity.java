package fpl.manhph61584.duan1_nhom3_app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ImageView;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import fpl.manhph61584.duan1_nhom3_app.ProductAdapter;
import fpl.manhph61584.duan1_nhom3_app.network.ApiClient;
import fpl.manhph61584.duan1_nhom3_app.network.ApiService;
import fpl.manhph61584.duan1_nhom3_app.Product;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rcvProduct;
    private ProductAdapter adapter;
    private EditText edtSearch;
    private ImageView btnCart;
    private List<Product> productList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_main);
            Log.d("MainActivity", "✅ Layout loaded");

            // Ánh xạ view
            rcvProduct = findViewById(R.id.rcvProduct);
            edtSearch = findViewById(R.id.edtSearch);
            btnCart = findViewById(R.id.btnCart);

            if (rcvProduct == null) {
                Log.e("MainActivity", "❌ rcvProduct is null!");
                Toast.makeText(this, "Lỗi: không tìm thấy RecyclerView", Toast.LENGTH_SHORT).show();
                return;
            }

            if (edtSearch == null) {
                Log.e("MainActivity", "❌ edtSearch is null!");
            }

            // Setup RecyclerView
            rcvProduct.setLayoutManager(new GridLayoutManager(this, 2));
            adapter = new ProductAdapter(this, productList);
            rcvProduct.setAdapter(adapter);

            Log.d("MainActivity", "✅ RecyclerView setup done");

            if (btnCart != null) {
                btnCart.setOnClickListener(v ->
                        startActivity(new Intent(MainActivity.this, CartActivity.class))
                );
            }

            // Load sản phẩm
            loadProducts(null);

            // Tìm kiếm
            if (edtSearch != null) {
                edtSearch.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        String keyword = s.toString().trim();
                        loadProducts(keyword.isEmpty() ? null : keyword);
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
                });
            }

        } catch (Exception e) {
            Log.e("MainActivity", "❌ onCreate error: ", e);
            Toast.makeText(this, "Lỗi khởi tạo: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void loadProducts(String search) {
        try {
            Log.d("MainActivity", "🔄 Loading products...");

            ApiClient.getApiService().getProducts(search).enqueue(new Callback<List<Product>>() {
                @Override
                public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                    try {
                        Log.d("MainActivity", "📡 Response code: " + response.code());

                        if (response.isSuccessful() && response.body() != null) {
                            productList.clear();
                            productList.addAll(response.body());
                            adapter.notifyDataSetChanged();

                            Log.d("MainActivity", "✅ Loaded " + productList.size() + " products");

                            if (productList.isEmpty()) {
                                Toast.makeText(MainActivity.this, "Không có sản phẩm", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e("MainActivity", "❌ Response not successful");
                            Toast.makeText(MainActivity.this, "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("MainActivity", "❌ onResponse error: ", e);
                    }
                }

                @Override
                public void onFailure(Call<List<Product>> call, Throwable t) {
                    Log.e("MainActivity", "❌ API Failed: ", t);
                    Toast.makeText(MainActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

        } catch (Exception e) {
            Log.e("MainActivity", "❌ loadProducts error: ", e);
        }
    }
}