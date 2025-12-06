package fpl.manhph61584.duan1_nhom3_app;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OrderStatusAdapter extends RecyclerView.Adapter<OrderStatusAdapter.OrderStatusViewHolder> {

    public interface OnItemSelectionChangedListener {
        void onSelectionChanged(Set<Integer> selectedPositions, double total);
    }

    interface OnStatusChangedListener {
        void onStatusChanged();
    }

    private final Context context;
    public final List<CartItem> orderItems; // Made public for easier access
    private final OnStatusChangedListener listener;
    private final Set<Integer> selectedPositions = new HashSet<>();
    private OnItemSelectionChangedListener selectionListener;
    private boolean isEditMode = false;

    public OrderStatusAdapter(Context context, List<CartItem> items, OnStatusChangedListener listener) {
        this.context = context;
        this.orderItems = new ArrayList<>(items);
        this.listener = listener;
    }

    public void setSelectionListener(OnItemSelectionChangedListener listener) {
        this.selectionListener = listener;
    }

    public Set<Integer> getSelectedPositions() {
        return new HashSet<>(selectedPositions);
    }

    public void selectAll(boolean select) {
        if (select) {
            for (int i = 0; i < orderItems.size(); i++) {
                selectedPositions.add(i);
            }
        } else {
            selectedPositions.clear();
        }
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    public void setEditMode(boolean editMode) {
        isEditMode = editMode;
        if (!isEditMode) {
            selectedPositions.clear();
        }
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    private void notifySelectionChanged() {
        if (selectionListener != null) {
            double total = 0;
            for (Integer pos : selectedPositions) {
                if (pos >= 0 && pos < orderItems.size()) {
                    total += orderItems.get(pos).getSubtotal();
                }
            }
            selectionListener.onSelectionChanged(new HashSet<>(selectedPositions), total);
        }
    }

    @NonNull
    @Override
    public OrderStatusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_order_status, parent, false);
        return new OrderStatusViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderStatusViewHolder holder, int position) {
        CartItem item = orderItems.get(position);
        Product product = item.getProduct();

        holder.txtName.setText(product != null ? product.getName() : "Sản phẩm");
        holder.txtVariant.setText("Màu: " + item.getColor() + " | Size: " + item.getSize());
        holder.txtQuantity.setText("Số lượng: " + item.getQuantity());
        holder.txtPrice.setText(String.format("%,.0f₫", item.getSubtotal()));

        String imageUrl = product != null ? product.getImage() : null;
        if (imageUrl != null && imageUrl.startsWith("/uploads/")) {
            imageUrl = "http://10.0.2.2:3000" + imageUrl;
        }

        Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(holder.imgProduct);

        // Handle checkbox visibility and state
        holder.checkboxItem.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        holder.checkboxItem.setChecked(selectedPositions.contains(position));

        holder.checkboxItem.setOnCheckedChangeListener(null);
        holder.checkboxItem.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedPositions.add(position);
            } else {
                selectedPositions.remove(position);
            }
            notifySelectionChanged();
        });

        // Click vào item để xem chi tiết sản phẩm (chỉ khi không ở edit mode)
        holder.itemView.setOnClickListener(v -> {
            if (!isEditMode && product != null && product.getId() != null) {
                Intent intent = new Intent(context, DetailProductActivity.class);
                intent.putExtra("id", product.getId());
                context.startActivity(intent);
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).overridePendingTransition(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left
                    );
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return orderItems.size();
    }

    public void updateData(List<CartItem> items) {
        orderItems.clear();
        orderItems.addAll(items);
        notifyDataSetChanged();
    }

    static class OrderStatusViewHolder extends RecyclerView.ViewHolder {
        AppCompatCheckBox checkboxItem;
        ImageView imgProduct;
        TextView txtName, txtVariant, txtQuantity, txtPrice;

        OrderStatusViewHolder(@NonNull View itemView) {
            super(itemView);
            checkboxItem = itemView.findViewById(R.id.checkboxItem);
            imgProduct = itemView.findViewById(R.id.orderImgProduct);
            txtName = itemView.findViewById(R.id.orderTxtName);
            txtVariant = itemView.findViewById(R.id.orderTxtVariant);
            txtQuantity = itemView.findViewById(R.id.orderTxtQuantity);
            txtPrice = itemView.findViewById(R.id.orderTxtPrice);
        }
    }
}

