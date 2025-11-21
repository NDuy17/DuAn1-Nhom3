package fpl.manhph61584.duan1_nhom3_app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class CartActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView txtTotal;
    private Button btnCheckout;
    private CartAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        recyclerView = findViewById(R.id.cartRecyclerView);
        txtTotal = findViewById(R.id.cartTotal);
        btnCheckout = findViewById(R.id.cartCheckout);

        adapter = new CartAdapter(this, new ArrayList<>(CartManager.getCartItems()));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        updateTotal();

        btnCheckout.setOnClickListener(v ->
                Toast.makeText(this, "Tính năng thanh toán sẽ cập nhật sau", Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter = new CartAdapter(this, new ArrayList<>(CartManager.getCartItems()));
        recyclerView.setAdapter(adapter);
        updateTotal();
    }

    private void updateTotal() {
        txtTotal.setText(String.format("Tổng: %,.0f₫", CartManager.getTotal()));
    }
}

