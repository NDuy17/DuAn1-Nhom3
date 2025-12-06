package fpl.manhph61584.duan1_nhom3_app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    public interface OnItemSelectionChangedListener {
        void onSelectionChanged(Set<Integer> selectedPositions);
    }

    private final Context context;
    public final List<CartItem> items;
    private final Set<Integer> selectedPositions = new HashSet<>();
    private OnItemSelectionChangedListener selectionListener;
    private boolean isEditMode = false;

    public CartAdapter(Context context, List<CartItem> items) {
        this.context = context;
        this.items = items;
    }

    public void setSelectionListener(OnItemSelectionChangedListener listener) {
        this.selectionListener = listener;
    }

    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        if (!editMode) {
            selectedPositions.clear();
        }
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    public Set<Integer> getSelectedPositions() {
        return new HashSet<>(selectedPositions);
    }

    public void selectAll(boolean select) {
        selectedPositions.clear();
        if (select) {
            for (int i = 0; i < items.size(); i++) {
                selectedPositions.add(i);
            }
        }
        notifyDataSetChanged();
        notifySelectionChanged();
    }

    private void notifySelectionChanged() {
        if (selectionListener != null) {
            selectionListener.onSelectionChanged(new HashSet<>(selectedPositions));
        }
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
        holder.txtSubtotal.setText(String.format("%,.0f₫", item.getSubtotal()));

        String imageUrl = product.getImage();
        if (imageUrl != null && imageUrl.startsWith("/uploads/")) {
            imageUrl = "http://10.0.2.2:3000" + imageUrl;
        }

        Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.imgProduct);

        // Show/hide checkbox based on edit mode
        if (holder.checkboxItem != null) {
            holder.checkboxItem.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
            if (isEditMode) {
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
            }
        }

        // Click vào item để xem chi tiết sản phẩm (chỉ khi không ở edit mode)
        holder.itemView.setOnClickListener(v -> {
            if (!isEditMode && product.getId() != null) {
                android.content.Intent intent = new android.content.Intent(context, DetailProductActivity.class);
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
        return items.size();
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        AppCompatCheckBox checkboxItem;
        ImageView imgProduct;
        TextView txtName, txtPrice, txtQuantity, txtVariant, txtSubtotal;

        CartViewHolder(@NonNull View itemView) {
            super(itemView);
            checkboxItem = itemView.findViewById(R.id.checkboxItem);
            imgProduct = itemView.findViewById(R.id.cartImgProduct);
            txtName = itemView.findViewById(R.id.cartTxtName);
            txtPrice = itemView.findViewById(R.id.cartTxtPrice);
            txtQuantity = itemView.findViewById(R.id.cartTxtQuantity);
            txtVariant = itemView.findViewById(R.id.cartTxtVariant);
            txtSubtotal = itemView.findViewById(R.id.cartTxtSubtotal);
        }
    }
}

