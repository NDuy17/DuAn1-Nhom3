const express = require("express");
const multer = require("multer");
const path = require("path");
const Product = require("../models/Product");
const { verifyToken, requireAdmin } = require("../middleware/auth");

const router = express.Router();

const storage = multer.diskStorage({
  destination: function (_req, _file, cb) {
    cb(null, "uploads/");
  },
  filename: function (_req, file, cb) {
    const uniqueSuffix = Date.now() + "-" + Math.round(Math.random() * 1e9);
    cb(null, uniqueSuffix + path.extname(file.originalname));
  },
});

const fileFilter = (_req, file, cb) => {
  const allowedTypes = /jpeg|jpg|png|gif|webp/;
  const extname = allowedTypes.test(path.extname(file.originalname).toLowerCase());
  const mimetype = allowedTypes.test(file.mimetype);

  if (mimetype && extname) {
    cb(null, true);
  } else {
    cb(new Error("Chỉ cho phép upload file ảnh (jpeg, jpg, png, gif, webp)"));
  }
};

const upload = multer({
  storage,
  limits: { fileSize: 5 * 1024 * 1024 },
  fileFilter,
});

router.post("/", verifyToken, requireAdmin, upload.single("image"), async (req, res) => {
  try {
    const imagePath = req.file ? `/uploads/${req.file.filename}` : req.body.image;
    const { name, description, price, stock, category, colors, sizes } = req.body;

    // Parse colors và sizes thành mảng
    let colorsArray = [];
    let sizesArray = [];

    if (colors) {
      if (typeof colors === 'string') {
        // Nếu là string JSON, parse nó
        try {
          colorsArray = JSON.parse(colors);
        } catch (e) {
          // Nếu không phải JSON, coi như là một giá trị đơn
          colorsArray = [colors];
        }
      } else if (Array.isArray(colors)) {
        colorsArray = colors;
      }
      // Lọc bỏ giá trị rỗng
      colorsArray = colorsArray.filter(c => c && c.trim() !== '');
    }

    if (sizes) {
      if (typeof sizes === 'string') {
        try {
          sizesArray = JSON.parse(sizes);
        } catch (e) {
          sizesArray = [sizes];
        }
      } else if (Array.isArray(sizes)) {
        sizesArray = sizes;
      }
      // Lọc bỏ giá trị rỗng
      sizesArray = sizesArray.filter(s => s && s.trim() !== '');
    }

    // Parse variants nếu có
    let variantsArray = [];
    if (req.body.variants) {
      try {
        if (typeof req.body.variants === 'string') {
          variantsArray = JSON.parse(req.body.variants);
        } else if (Array.isArray(req.body.variants)) {
          variantsArray = req.body.variants;
        }
      } catch (e) {
        // Nếu không parse được, bỏ qua
      }
    }

    const product = await Product.create({
      name,
      description,
      price: Number(price),
      stock: Number(stock),
      sold: Number(req.body.sold || 0),
      category,
      image: imagePath,
      colors: colorsArray,
      sizes: sizesArray,
      variants: variantsArray,
    });
    res.status(201).json(product);
  } catch (err) {
    // Handle duplicate key errors (especially for old variant indexes)
    if (err.code === 11000 || err.message.includes('duplicate key')) {
      const errorMessage = err.message.includes('variants') 
        ? 'Lỗi: Index cũ trong database đang gây xung đột. Vui lòng chạy script fix-indexes.js để sửa lỗi này.'
        : 'Lỗi: Dữ liệu trùng lặp. ' + err.message;
      return res.status(400).json({ 
        error: errorMessage,
        details: err.message,
        fix: err.message.includes('variants') ? 'Chạy: node scripts/fix-indexes.js' : null
      });
    }
    res.status(400).json({ error: err.message });
  }
});

// Lấy danh sách sản phẩm
router.get("/", async (req, res) => {
  try {
    const { search, category } = req.query;
    const query = {};
    
    if (search) {
      query.name = { $regex: search, $options: "i" };
    }
    
    if (category) {
      query.category = category;
    }
    
    const products = await Product.find(query).populate("category");
    res.json(products);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Xem chi tiết sản phẩm
router.get("/:id", async (req, res) => {
  try {
    const product = await Product.findById(req.params.id).populate("category");
    if (!product) return res.status(404).json({ message: "Không tìm thấy sản phẩm" });
    res.json(product);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Lấy số lượng tồn kho theo màu và size
// GET /api/products/:id/stock?color=Đỏ&size=M
// Nếu không có color và size: trả về tổng stock
router.get("/:id/stock", async (req, res) => {
  try {
    const { color, size } = req.query;
    const product = await Product.findById(req.params.id);
    
    if (!product) {
      return res.status(404).json({ message: "Không tìm thấy sản phẩm" });
    }

    // Nếu không có color và size, trả về tổng stock
    if (!color && !size) {
      return res.json({ 
        stock: product.stock || 0,
        totalStock: product.stock || 0
      });
    }

    // Tìm variant theo color và size
    const normalizedColor = color || "Mặc định";
    const normalizedSize = size || "Free size";
    
    const variant = product.variants?.find(
      v => v.color === normalizedColor && v.size === normalizedSize
    );

    if (variant) {
      return res.json({ 
        stock: variant.stock || 0,
        sold: variant.sold || 0,
        color: variant.color,
        size: variant.size
      });
    }

    // Nếu không tìm thấy variant, trả về 0
    return res.json({ 
      stock: 0,
      color: normalizedColor,
      size: normalizedSize
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Cập nhật sản phẩm
router.put("/:id", verifyToken, requireAdmin, upload.single("image"), async (req, res) => {
  try {
    const productId = req.params.id;
    const product = await Product.findById(productId);
    
    if (!product) {
      return res.status(404).json({ message: "Không tìm thấy sản phẩm" });
    }

    // Cập nhật image nếu có file mới (chỉ cập nhật nếu có file thực sự, không phải empty part)
    if (req.file && req.file.filename) {
      product.image = `/uploads/${req.file.filename}`;
    }

    // Cập nhật các trường khác
    if (req.body.name) product.name = req.body.name;
    if (req.body.description !== undefined) product.description = req.body.description;
    if (req.body.price) product.price = Number(req.body.price);
    if (req.body.stock !== undefined) product.stock = Number(req.body.stock);
    if (req.body.sold !== undefined) product.sold = Number(req.body.sold);
    if (req.body.category) product.category = req.body.category;

    // Parse colors và sizes
    if (req.body.colors !== undefined) {
      let colorsArray = [];
      if (typeof req.body.colors === 'string') {
        try {
          colorsArray = JSON.parse(req.body.colors);
        } catch (e) {
          colorsArray = [req.body.colors];
        }
      } else if (Array.isArray(req.body.colors)) {
        colorsArray = req.body.colors;
      }
      product.colors = colorsArray.filter(c => c && c.trim() !== '');
    }

    if (req.body.sizes !== undefined) {
      let sizesArray = [];
      if (typeof req.body.sizes === 'string') {
        try {
          sizesArray = JSON.parse(req.body.sizes);
        } catch (e) {
          sizesArray = [req.body.sizes];
        }
      } else if (Array.isArray(req.body.sizes)) {
        sizesArray = req.body.sizes;
      }
      product.sizes = sizesArray.filter(s => s && s.trim() !== '');
    }

    // Parse variants nếu có
    if (req.body.variants !== undefined) {
      try {
        if (typeof req.body.variants === 'string') {
          product.variants = JSON.parse(req.body.variants);
        } else if (Array.isArray(req.body.variants)) {
          product.variants = req.body.variants;
        }
      } catch (e) {
        // Ignore parse error
      }
    }

    product.updatedAt = new Date();
    await product.save();

    res.json(product);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// Xóa sản phẩm
router.delete("/:id", verifyToken, requireAdmin, async (req, res) => {
  try {
    const productId = req.params.id;
    const product = await Product.findByIdAndDelete(productId);
    
    if (!product) {
      return res.status(404).json({ message: "Không tìm thấy sản phẩm" });
    }

    res.json({ message: "Đã xóa sản phẩm thành công" });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
