const express = require('express');
const multer = require('multer');
const path = require('path');
const router = express.Router();

// Cấu hình lưu trữ file
const storage = multer.diskStorage({
  destination: function (req, file, cb) {
    cb(null, 'uploads/'); // Thư mục lưu ảnh
  },
  filename: function (req, file, cb) {
    // Tạo tên file: timestamp + tên gốc
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
    cb(null, uniqueSuffix + path.extname(file.originalname));
  }
});

// Lọc chỉ cho phép upload ảnh
const fileFilter = (req, file, cb) => {
  const allowedTypes = /jpeg|jpg|png|gif|webp/;
  const extname = allowedTypes.test(path.extname(file.originalname).toLowerCase());
  
  // Kiểm tra MIME type: chấp nhận cả "image/*" (generic) hoặc MIME type cụ thể
  const isImageMimeType = file.mimetype && (
    file.mimetype.startsWith('image/') || 
    allowedTypes.test(file.mimetype)
  );
  
  // Chỉ cần extension hợp lệ hoặc MIME type hợp lệ (không cần cả hai)
  if (extname || isImageMimeType) {
    return cb(null, true);
  } else {
    cb(new Error('Chỉ cho phép upload file ảnh (jpeg, jpg, png, gif, webp)'));
  }
};

const upload = multer({
  storage: storage,
  limits: {
    fileSize: 5 * 1024 * 1024 // Giới hạn 5MB
  },
  fileFilter: fileFilter
});

// Route upload ảnh
router.post('/', upload.single('image'), (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ error: 'Không có file được upload' });
    }

    res.json({
      message: 'Upload ảnh thành công',
      filename: req.file.filename,
      path: `/uploads/${req.file.filename}`,
      size: req.file.size
    });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Route upload nhiều ảnh
router.post('/multiple', upload.array('images', 10), (req, res) => {
  try {
    if (!req.files || req.files.length === 0) {
      return res.status(400).json({ error: 'Không có file được upload' });
    }

    const files = req.files.map(file => ({
      filename: file.filename,
      path: `/uploads/${file.filename}`,
      size: file.size
    }));

    res.json({
      message: `Upload ${files.length} ảnh thành công`,
      files: files
    });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;

