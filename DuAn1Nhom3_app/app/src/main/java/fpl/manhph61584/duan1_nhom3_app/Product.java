package fpl.manhph61584.duan1_nhom3_app;

public class Product {
    private String _id;
    private String name;
    private String description;
    private double price;
    private Category category;   // <-- QUAN TRỌNG!
    private int stock;
    private int sold; // Số lượng đã bán
    private String image;
    private String[] colors;
    private String[] sizes;
    private String createdAt;
    private String updatedAt;

    public String getId() { return _id; }
    public void setId(String _id) { this._id = _id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public int getSold() { return sold; }
    public void setSold(int sold) { this.sold = sold; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String[] getColors() { return colors; }
    public void setColors(String[] colors) { this.colors = colors; }

    public String[] getSizes() { return sizes; }
    public void setSizes(String[] sizes) { this.sizes = sizes; }
}
