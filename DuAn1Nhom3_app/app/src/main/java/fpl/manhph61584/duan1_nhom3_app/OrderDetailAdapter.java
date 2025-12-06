package fpl.manhph61584.duan1_nhom3_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import fpl.manhph61584.duan1_nhom3_app.network.dto.OrderItemDto;

public class OrderDetailAdapter extends RecyclerView.Adapter<OrderDetailAdapter.OrderItemViewHolder> {

    private final android.content.Context context;
    private List<OrderItemDto> items;

    public OrderDetailAdapter(android.content.Context context, List<OrderItemDto> items) {
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
    public OrderItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_order_status, parent, false);
        return new OrderItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderItemViewHolder holder, int position) {
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
        }
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class OrderItemViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView txtName, txtVariant, txtQuantity, txtPrice;

        OrderItemViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.orderImgProduct);
            txtName = itemView.findViewById(R.id.orderTxtName);
            txtVariant = itemView.findViewById(R.id.orderTxtVariant);
            txtQuantity = itemView.findViewById(R.id.orderTxtQuantity);
            txtPrice = itemView.findViewById(R.id.orderTxtPrice);
        }
    }
}




