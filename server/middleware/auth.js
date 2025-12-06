const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET || 'secret_key';

const verifyToken = (req, res, next) => {
  const authHeader = req.headers.authorization;

  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    console.log('❌ No authorization header');
    return res.status(401).json({ message: 'Vui lòng đăng nhập để tiếp tục' });
  }

  const token = authHeader.split(' ')[1];

  try {
    const decoded = jwt.verify(token, JWT_SECRET);
    console.log('🔑 Token decoded - ID:', decoded.id, ', Role:', decoded.role);
    req.user = decoded;
    next();
  } catch (error) {
    console.log('❌ Token verification failed:', error.message);
    res.status(401).json({ message: 'Token không hợp lệ', error: error.message });
  }
};

const requireAdmin = (req, res, next) => {
  // Chỉ cần kiểm tra role từ token (đã được verify trong verifyToken)
  if (!req.user) {
    console.log('❌ No user in request');
    return res.status(403).json({ message: 'Chỉ admin mới có quyền truy cập' });
  }
  
  // Kiểm tra role từ JWT token (đã được sign khi đăng nhập)
  const userRole = req.user.role;
  console.log('🔐 Checking admin role from token - Role:', userRole);
  
  if (userRole !== 'admin') {
    console.log('❌ User role is not admin. Role:', userRole);
    return res.status(403).json({ message: 'Chỉ admin mới có quyền truy cập' });
  }

  console.log('✅ Admin access granted for user ID:', req.user.id);
  next();
};

module.exports = {
  verifyToken,
  requireAdmin,
};

