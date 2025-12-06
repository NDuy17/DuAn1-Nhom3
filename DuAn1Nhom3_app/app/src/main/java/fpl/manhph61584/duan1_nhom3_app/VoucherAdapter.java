package fpl.manhph61584.duan1_nhom3_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.VoucherViewHolder> {

    private List<Voucher> vouchers;
    private Voucher selectedVoucher;
    private OnVoucherSelectListener listener;

    public interface OnVoucherSelectListener {
        void onVoucherSelected(Voucher voucher);
    }

    public VoucherAdapter(List<Voucher> vouchers, OnVoucherSelectListener listener) {
        this.vouchers = vouchers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VoucherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_voucher, parent, false);
        return new VoucherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VoucherViewHolder holder, int position) {
        Voucher voucher = vouchers.get(position);
        holder.bind(voucher, voucher == selectedVoucher);
        
        holder.itemView.setOnClickListener(v -> {
            if (selectedVoucher == voucher) {
                selectedVoucher = null;
            } else {
                selectedVoucher = voucher;
            }
            notifyDataSetChanged();
            if (listener != null) {
                listener.onVoucherSelected(selectedVoucher);
            }
        });
    }

    @Override
    public int getItemCount() {
        return vouchers != null ? vouchers.size() : 0;
    }

    public Voucher getSelectedVoucher() {
        return selectedVoucher;
    }

    public void setSelectedVoucher(Voucher voucher) {
        this.selectedVoucher = voucher;
    }

    static class VoucherViewHolder extends RecyclerView.ViewHolder {
        private TextView txtCode, txtName, txtDiscount, txtDescription;

        public VoucherViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCode = itemView.findViewById(R.id.txtVoucherCode);
            txtName = itemView.findViewById(R.id.txtVoucherName);
            txtDiscount = itemView.findViewById(R.id.txtVoucherDiscount);
            txtDescription = itemView.findViewById(R.id.txtVoucherDescription);
        }

        public void bind(Voucher voucher, boolean isSelected) {
            txtCode.setText(voucher.getCode());
            txtName.setText(voucher.getName());
            
            String discountText = "";
            if ("percentage".equals(voucher.getDiscountType())) {
                discountText = "Giảm " + (int)voucher.getDiscountValue() + "%";
            } else {
                discountText = "Giảm " + String.format("%,.0f₫", voucher.getDiscountValue());
            }
            txtDiscount.setText(discountText);
            
            if (voucher.getDescription() != null && !voucher.getDescription().isEmpty()) {
                txtDescription.setText(voucher.getDescription());
                txtDescription.setVisibility(View.VISIBLE);
            } else {
                txtDescription.setVisibility(View.GONE);
            }
            
            itemView.setBackgroundResource(isSelected ? R.drawable.bg_selected : R.drawable.bg_unselect);
        }
    }
}

