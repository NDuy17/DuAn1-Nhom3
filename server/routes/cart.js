const express = require('express');
const Cart = require('../models/Cart');
const Product = require('../models/Product');
const { verifyToken } = require('../middleware/auth');

const router = express.Router();

const populateCart = (cartQuery) =>
  cartQuery.populate({
    path: 'items.product',
    select: 'name price image stock',
  });

/**
 * Lấy giỏ hàng của người dùng hiện tại
 */
router.get('/', verifyToken, async (req, res) => {
  try {
    const cart = await populateCart(
      Cart.findOne({ user: req.user.id })
    );

    res.json(cart || { user: req.user.id, items: [] });
  } catch (error) {
    res.status(500).json({ message: 'Không thể lấy giỏ hàng', error: error.message });
  }
});

/**
 * Thêm sản phẩm vào giỏ
 * Body: { productId, quantity, color, size }
 */
router.post('/', verifyToken, async (req, res) => {
  try {
    const { productId, quantity, color, size } = req.body;

    if (!productId) {
      return res.status(400).json({ message: 'Thiếu productId' });
    }

    const productExists = await Product.exists({ _id: productId });
    if (!productExists) {
      return res.status(404).json({ message: 'Không tìm thấy sản phẩm' });
    }

    const qty = Math.max(1, Number(quantity) || 1);
    const safeColor = color && color.trim() !== '' ? color.trim() : 'Mặc định';
    const safeSize = size && size.trim() !== '' ? size.trim() : 'Free size';

    let cart = await Cart.findOne({ user: req.user.id });

    if (!cart) {
      cart = await Cart.create({
        user: req.user.id,
        items: [{ product: productId, quantity: qty, color: safeColor, size: safeSize }],
      });
    } else {
      // Tìm item cùng product, color và size
      const item = cart.items.find(
        (cartItem) => 
          cartItem.product.toString() === productId &&
          cartItem.color === safeColor &&
          cartItem.size === safeSize
      );

      if (item) {
        item.quantity += qty;
      } else {
        cart.items.push({ product: productId, quantity: qty, color: safeColor, size: safeSize });
      }

      await cart.save();
    }

    const populatedCart = await populateCart(
      Cart.findById(cart._id)
    );

    res.status(200).json(populatedCart);
  } catch (error) {
    res.status(500).json({ message: 'Không thể thêm vào giỏ hàng', error: error.message });
  }
});

/**
 * Cập nhật số lượng sản phẩm trong giỏ
 * Body: { quantity }
 */
router.put('/:productId', verifyToken, async (req, res) => {
  try {
    const { productId } = req.params;
    const { quantity } = req.body;

    const qty = Number(quantity);
    if (!qty || qty < 1) {
      return res.status(400).json({ message: 'Số lượng phải lớn hơn 0' });
    }

    const cart = await Cart.findOne({ user: req.user.id });
    if (!cart) {
      return res.status(404).json({ message: 'Giỏ hàng trống' });
    }

    const item = cart.items.find(
      (cartItem) => cartItem.product.toString() === productId
    );

    if (!item) {
      return res.status(404).json({ message: 'Sản phẩm không có trong giỏ' });
    }

    item.quantity = qty;
    await cart.save();

    const populatedCart = await populateCart(
      Cart.findById(cart._id)
    );

    res.json(populatedCart);
  } catch (error) {
    res.status(500).json({ message: 'Không thể cập nhật giỏ hàng', error: error.message });
  }
});

/**
 * Xóa sản phẩm khỏi giỏ
 */
router.delete('/:productId', verifyToken, async (req, res) => {
  try {
    const { productId } = req.params;
    const cart = await Cart.findOne({ user: req.user.id });

    if (!cart) {
      return res.status(404).json({ message: 'Giỏ hàng trống' });
    }

    const initialLength = cart.items.length;
    cart.items = cart.items.filter(
      (item) => item.product.toString() !== productId
    );

    if (cart.items.length === initialLength) {
      return res.status(404).json({ message: 'Sản phẩm không có trong giỏ' });
    }

    await cart.save();

    const populatedCart = await populateCart(
      Cart.findById(cart._id)
    );

    res.json(populatedCart);
  } catch (error) {
    res.status(500).json({ message: 'Không thể xóa sản phẩm khỏi giỏ', error: error.message });
  }
});

module.exports = router;

