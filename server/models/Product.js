// models/Product.js
const mongoose = require("mongoose");
require("./Category");


const variantSchema = new mongoose.Schema({
  color: { type: String, required: true },
  size: { type: String, required: true },
  stock: { type: Number, required: true, min: 0, default: 0 },
  sold: { type: Number, default: 0, min: 0 }
}, { _id: false });

const productSchema = new mongoose.Schema({
  name: { type: String, required: true },
  description: { type: String },
  price: { type: Number, required: true, min: 0 },
  category: { type: mongoose.Schema.Types.ObjectId, ref: "Category", required: true },
  stock: { type: Number, required: true, min: 0, default: 0 }, // Tổng stock (tổng của tất cả variants)
  sold: { type: Number, default: 0, min: 0 }, // Tổng số lượng đã bán
  image: { type: String },
  colors: { type: [String], default: [] },
  sizes: { type: [String], default: [] },
  variants: { type: [variantSchema], default: [] }, // Stock theo từng biến thể (màu + size)
  createdAt: { type: Date, default: Date.now },
  updatedAt: { type: Date, default: Date.now }
});

module.exports = mongoose.model("Product", productSchema);
