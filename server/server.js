require('dotenv').config();
const express = require('express');
const connectDB = require('./config/database');

const app = express();

// Kết nối MongoDB
connectDB();

// Middleware
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Serve static files từ thư mục uploads
app.use('/uploads', express.static('uploads'));

// Serve static files từ thư mục public (cho admin panel)
app.use(express.static('public'));

// Routes API (tạm thời để trống, sẽ thêm logic sau)
const usersRoutes = require('./routes/users');
const productsRoutes = require('./routes/products');
const categoriesRoutes = require('./routes/categories');
const ordersRoutes = require('./routes/orders');
const reviewsRoutes = require('./routes/reviews');
const cartRoutes = require('./routes/cart');
const favoritesRoutes = require('./routes/favorites');
const vouchersRoutes = require('./routes/vouchers');
const uploadRoutes = require('./routes/upload');

app.use('/api/users', usersRoutes);
app.use('/api/products', productsRoutes);
app.use('/api/categories', categoriesRoutes);
app.use('/api/orders', ordersRoutes);
app.use('/api/reviews', reviewsRoutes);
app.use('/api/cart', cartRoutes);
app.use('/api/favorites', favoritesRoutes);
app.use('/api/vouchers', vouchersRoutes);
app.use('/api/upload', uploadRoutes);

// Route chính
app.get('/', (req, res) => {
  res.json({ 
    message: 'Server đang chạy!',
    database: 'duan1',
    collections: ['users', 'products', 'categories', 'orders', 'reviews', 'favorites', 'vouchers', 'cart'],
    apiEndpoints: {
      users: '/api/users',
      products: '/api/products',
      categories: '/api/categories',
      orders: '/api/orders',
      reviews: '/api/reviews',
      favorites: '/api/favorites',
      vouchers: '/api/vouchers',
      upload: '/api/upload',
      cart: '/api/cart'
    }
  });
});

const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
  console.log(`Server đang chạy trên port ${PORT}`);
});

