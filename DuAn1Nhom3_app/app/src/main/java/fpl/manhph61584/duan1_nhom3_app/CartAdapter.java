package fpl.manhph61584.duan1_nhom3_app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private final Context context;
    private final List<CartItem> items;

    public CartAdapter(Context context, List<CartItem> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_cart_item, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = items.get(position);
        Product product = item.getProduct();

        holder.txtName.setText(product.getName());
        holder.txtPrice.setText(String.format("%,.0f₫", item.getUnitPrice()));
        holder.txtQuantity.setText("Số lượng: " + item.getQuantity());
        holder.txtVariant.setText("Màu: " + item.getColor() + " | Size: " + item.getSize());
        holder.txtSubtotal.setText(String.format("Tạm tính: %,.0f₫", item.getSubtotal()));

        Glide.with(context)
                .load(product.getImage())
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.imgProduct);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView txtName, txtPrice, txtQuantity, txtVariant, txtSubtotal;

        CartViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.cartImgProduct);
            txtName = itemView.findViewById(R.id.cartTxtName);
            txtPrice = itemView.findViewById(R.id.cartTxtPrice);
            txtQuantity = itemView.findViewById(R.id.cartTxtQuantity);
            txtVariant = itemView.findViewById(R.id.cartTxtVariant);
            txtSubtotal = itemView.findViewById(R.id.cartTxtSubtotal);
        }
    }
}

