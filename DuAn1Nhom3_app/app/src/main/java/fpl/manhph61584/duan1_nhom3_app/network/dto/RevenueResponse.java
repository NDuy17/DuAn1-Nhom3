package fpl.manhph61584.duan1_nhom3_app.network.dto;

import java.util.List;

public class RevenueResponse {
    private int totalOrders;
    private double totalRevenue;
    private List<CategoryRevenue> categoryRevenue;

    public int getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(int totalOrders) {
        this.totalOrders = totalOrders;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public List<CategoryRevenue> getCategoryRevenue() {
        return categoryRevenue;
    }

    public void setCategoryRevenue(List<CategoryRevenue> categoryRevenue) {
        this.categoryRevenue = categoryRevenue;
    }

    public static class CategoryRevenue {
        private String categoryId;
        private String categoryName;
        private double revenue;

        public String getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(String categoryId) {
            this.categoryId = categoryId;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public double getRevenue() {
            return revenue;
        }

        public void setRevenue(double revenue) {
            this.revenue = revenue;
        }
    }
}

