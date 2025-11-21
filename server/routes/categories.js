const express = require('express');
const Category = require('../models/Category');

const router = express.Router();

// Tạo category mới
router.post('/', async (req, res) => {
  try {
    const category = await Category.create({
      name: req.body.name,
      description: req.body.description
    });
    res.status(201).json(category);
  } catch (error) {
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

module.exports = router;
