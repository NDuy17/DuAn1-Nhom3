const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const User = require('../models/User');
const Voucher = require('../models/Voucher');
const { verifyToken, requireAdmin } = require('../middleware/auth');

const router = express.Router();
const JWT_SECRET = process.env.JWT_SECRET || 'secret_key';

/**
 * Đăng ký người dùng mới
 * Body: { name, email, password }
 */
router.post('/register', async (req, res) => {
  try {
    const { name, email, password } = req.body;

    if (!name || !email || !password) {
      return res.status(400).json({ message: 'Vui lòng nhập đủ tên, email và mật khẩu' });
    }

    const existingUser = await User.findOne({ email });
    if (existingUser) {
      return res.status(409).json({ message: 'Email đã được sử dụng' });
    }

    const hashedPassword = await bcrypt.hash(password, 10);
    const newUser = await User.create({ name, email, password: hashedPassword });

    // Tự động tạo voucher 50% vĩnh viễn cho user mới
    try {
      const voucherCode = `WELCOME${newUser._id.toString().substring(0, 8).toUpperCase()}`;
      const now = new Date();
      const foreverDate = new Date('2099-12-31T23:59:59.999Z'); // Vĩnh viễn

      await Voucher.create({
        code: voucherCode,
        name: 'Voucher chào mừng',
        description: 'Giảm 50% cho khách hàng mới',
        discountType: 'percentage',
        discountValue: 50,
        minPurchaseAmount: 0,
        maxDiscountAmount: null,
        startDate: now,
        endDate: foreverDate,
        usageLimit: null, // Không giới hạn lượt sử dụng
        usedCount: 0,
        isActive: true,
        user: newUser._id // Voucher thuộc về user này
      });
    } catch (voucherError) {
      // Nếu tạo voucher thất bại, vẫn cho phép đăng ký thành công
      console.error('Lỗi tạo voucher cho user mới:', voucherError);
    }

    res.status(201).json({
      message: 'Đăng ký thành công',
      user: {
        id: newUser._id,
        name: newUser.name,
        email: newUser.email,
        role: newUser.role,
        avatar: newUser.avatar || null
      }
    });
  } catch (err) {
    res.status(500).json({ message: 'Lỗi server', error: err.message });
  }
});

/**
 * Đăng nhập
 * Body: { email, password }
 */
router.post('/login', async (req, res) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({ message: 'Vui lòng nhập email và mật khẩu' });
    }

    const user = await User.findOne({ email });
    if (!user) {
      return res.status(404).json({ message: 'Không tìm thấy người dùng' });
    }

    const isMatch = await bcrypt.compare(password, user.password);
    if (!isMatch) {
      return res.status(401).json({ message: 'Sai mật khẩu' });
    }

    // Đảm bảo token luôn có role
    const tokenPayload = {
      id: user._id,
      role: user.role || 'user' // Mặc định là 'user' nếu không có role
    };
    console.log('🔑 Creating JWT token with payload:', tokenPayload);
    const token = jwt.sign(tokenPayload, JWT_SECRET, { expiresIn: '24h' }); // Tăng thời gian hết hạn lên 24h

    res.json({
      message: 'Đăng nhập thành công',
      token,
      user: {
        id: user._id,
        name: user.name,
        email: user.email,
        role: user.role,
        avatar: user.avatar || null
      }
    });
  } catch (err) {
    res.status(500).json({ message: 'Lỗi server', error: err.message });
  }
});

/**
 * Lấy thông tin profile của user hiện tại
 * Headers: Authorization: Bearer <token>
 */
router.get('/profile', verifyToken, async (req, res) => {
  try {
    const userId = req.user.id;
    const user = await User.findById(userId).select('-password');
    
    if (!user) {
      return res.status(404).json({ message: 'Không tìm thấy người dùng' });
    }
    
    res.json({
      user: {
        id: user._id,
        name: user.name,
        email: user.email,
        role: user.role,
        avatar: user.avatar || null
      }
    });
  } catch (err) {
    console.error('❌ Error getting profile:', err);
    res.status(500).json({ message: 'Lỗi server', error: err.message });
  }
});

/**
 * Cập nhật thông tin profile (name và avatar)
 * Headers: Authorization: Bearer <token>
 * Body: { name?, avatar? }
 */
router.put('/profile', verifyToken, async (req, res) => {
  try {
    console.log('📝 PUT /api/users/profile - Received request');
    console.log('📝 User ID from token:', req.user.id);
    console.log('📝 Request body:', req.body);
    
    const userId = req.user.id;
    const { name, avatar } = req.body;

    const updateData = {};
    if (name !== undefined && name !== null) {
      updateData.name = name;
    }
    if (avatar !== undefined && avatar !== null) {
      updateData.avatar = avatar;
    }
    updateData.updatedAt = new Date();

    if (Object.keys(updateData).length === 0) {
      return res.status(400).json({ message: 'Không có dữ liệu để cập nhật' });
    }

    const updatedUser = await User.findByIdAndUpdate(
      userId,
      { $set: updateData },
      { new: true, runValidators: true }
    );

    if (!updatedUser) {
      return res.status(404).json({ message: 'Không tìm thấy người dùng' });
    }

    console.log('✅ Profile updated successfully');
    res.json({
      message: 'Cập nhật profile thành công',
      user: {
        id: updatedUser._id,
        name: updatedUser.name,
        email: updatedUser.email,
        role: updatedUser.role,
        avatar: updatedUser.avatar || null
      }
    });
  } catch (err) {
    console.error('❌ Error updating profile:', err);
    res.status(500).json({ message: 'Lỗi server', error: err.message });
  }
});

/**
 * Admin lấy tất cả users
 * Headers: Authorization: Bearer <token>
 */
router.get('/admin/all', verifyToken, requireAdmin, async (req, res) => {
  try {
    const users = await User.find().select('-password').sort({ createdAt: -1 });
    res.json(users.map(user => ({
      id: user._id,
      name: user.name,
      email: user.email,
      role: user.role,
      avatar: user.avatar || null,
      createdAt: user.createdAt
    })));
  } catch (err) {
    console.error('❌ Error getting all users:', err);
    res.status(500).json({ message: 'Lỗi server', error: err.message });
  }
});

/**
 * Admin cập nhật role của user
 * Headers: Authorization: Bearer <token>
 * Body: { role: 'user'|'admin' }
 */
router.put('/admin/:id/role', verifyToken, requireAdmin, async (req, res) => {
  try {
    const userId = req.params.id;
    const { role } = req.body;

    if (!role || !['user', 'admin'].includes(role)) {
      return res.status(400).json({ message: 'Role không hợp lệ' });
    }

    const user = await User.findByIdAndUpdate(
      userId,
      { $set: { role: role, updatedAt: new Date() } },
      { new: true }
    );

    if (!user) {
      return res.status(404).json({ message: 'Không tìm thấy người dùng' });
    }

    res.json({
      message: 'Đã cập nhật role thành công',
      user: {
        id: user._id,
        name: user.name,
        email: user.email,
        role: user.role,
        avatar: user.avatar || null
      }
    });
  } catch (err) {
    console.error('❌ Error updating user role:', err);
    res.status(500).json({ message: 'Lỗi server', error: err.message });
  }
});

/**
 * Admin xóa user
 * Headers: Authorization: Bearer <token>
 */
router.delete('/admin/:id', verifyToken, requireAdmin, async (req, res) => {
  try {
    const userId = req.params.id;
    const user = await User.findByIdAndDelete(userId);

    if (!user) {
      return res.status(404).json({ message: 'Không tìm thấy người dùng' });
    }

    res.json({ message: 'Đã xóa người dùng thành công' });
  } catch (err) {
    console.error('❌ Error deleting user:', err);
    res.status(500).json({ message: 'Lỗi server', error: err.message });
  }
});

module.exports = router;