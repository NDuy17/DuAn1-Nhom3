package fpl.manhph61584.duan1_nhom3_app;

public class Voucher {
    private String _id;
    private String code;
    private String name;
    private String description;
    private String discountType; // "percentage" or "fixed"
    private double discountValue;
    private double minPurchaseAmount;
    private Double maxDiscountAmount;
    private String startDate;
    private String endDate;
    private Integer usageLimit;
    private int usedCount;
    private boolean isActive;

    public String getId() {
        return _id;
    }

    public void setId(String _id) {
        this._id = _id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public double getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(double discountValue) {
        this.discountValue = discountValue;
    }

    public double getMinPurchaseAmount() {
        return minPurchaseAmount;
    }

    public void setMinPurchaseAmount(double minPurchaseAmount) {
        this.minPurchaseAmount = minPurchaseAmount;
    }

    public Double getMaxDiscountAmount() {
        return maxDiscountAmount;
    }

    public void setMaxDiscountAmount(Double maxDiscountAmount) {
        this.maxDiscountAmount = maxDiscountAmount;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public Integer getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(Integer usageLimit) {
        this.usageLimit = usageLimit;
    }

    public int getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(int usedCount) {
        this.usedCount = usedCount;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    // Helper method để tính số tiền giảm giá
    public double calculateDiscount(double totalAmount) {
        // Nếu minPurchaseAmount = 0 hoặc null, áp dụng ngay
        // Nếu minPurchaseAmount > 0, chỉ áp dụng khi đơn hàng >= minPurchaseAmount
        if (minPurchaseAmount > 0 && totalAmount < minPurchaseAmount) {
            return 0;
        }

        double discount = 0;
        if ("percentage".equals(discountType)) {
            discount = totalAmount * discountValue / 100;
            if (maxDiscountAmount != null && discount > maxDiscountAmount) {
                discount = maxDiscountAmount;
            }
        } else if ("fixed".equals(discountType)) {
            discount = discountValue;
            if (discount > totalAmount) {
                discount = totalAmount;
            }
        }
        return discount;
    }
    
    // Kiểm tra voucher có thể áp dụng cho đơn hàng này không
    public boolean canApply(double totalAmount) {
        return minPurchaseAmount == 0 || totalAmount >= minPurchaseAmount;
    }
}







