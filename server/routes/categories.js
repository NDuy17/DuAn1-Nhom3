const express = require('express');
const Category = require('../models/Category');
const { verifyToken, requireAdmin } = require('../middleware/auth');

const router = express.Router();

// Tạo category mới (Admin only)
router.post('/', verifyToken, requireAdmin, async (req, res) => {
  try {
    const category = await Category.create({
      name: req.body.name,
      description: req.body.description
    });
    res.status(201).json(category);
  } catch (error) {
    if (error.code === 11000) {
      return res.status(409).json({ message: 'Tên danh mục đã tồn tại' });
    }
    res.status(400).json({ error: error.message });
  }
});

// Lấy danh sách category
router.get('/', async (_req, res) => {
  try {
    const categories = await Category.find().sort({ createdAt: -1 });
    res.json(categories);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Cập nhật category (Admin only)
router.put('/:id', verifyToken, requireAdmin, async (req, res) => {
  try {
    const { name, description } = req.body;
    const updateData = {};
    
    if (name) updateData.name = name;
    if (description !== undefined) updateData.description = description;
    updateData.updatedAt = new Date();
    
    const category = await Category.findByIdAndUpdate(
      req.params.id,
      { $set: updateData },
      { new: true, runValidators: true }
    );
    
    if (!category) {
      return res.status(404).json({ message: 'Không tìm thấy danh mục' });
    }
    
    res.json(category);
  } catch (error) {
    if (error.code === 11000) {
      return res.status(409).json({ message: 'Tên danh mục đã tồn tại' });
    }
    res.status(400).json({ error: error.message });
  }
});

// Xóa category (Admin only)
router.delete('/:id', verifyToken, requireAdmin, async (req, res) => {
  try {
    const category = await Category.findByIdAndDelete(req.params.id);
    
    if (!category) {
      return res.status(404).json({ message: 'Không tìm thấy danh mục' });
    }
    
    res.json({ message: 'Đã xóa danh mục thành công' });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
