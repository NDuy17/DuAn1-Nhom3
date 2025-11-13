# Cấu trúc thư mục - Ứng dụng Mua sắm Quần áo

## Tổng quan
Cấu trúc thư mục được tổ chức theo kiến trúc MVVM (Model-View-ViewModel) để kết nối với MongoDB database `duan1`.

## Cấu trúc thư mục chính

### 📁 `models/`
Chứa các model dữ liệu tương ứng với các collection trong MongoDB:
- `user/` - Model người dùng
- `product/` - Model sản phẩm quần áo
- `category/` - Model danh mục sản phẩm
- `order/` - Model đơn hàng
- `review/` - Model đánh giá sản phẩm
- `favorite/` - Model sản phẩm yêu thích
- `voucher/` - Model mã giảm giá

### 📁 `api/`
Chứa các lớp xử lý kết nối và giao tiếp với MongoDB:
- `interfaces/` - Các interface định nghĩa API endpoints
- `services/` - Các service class để gọi API MongoDB

### 📁 `repository/`
Chứa các repository pattern để quản lý dữ liệu:
- `user/` - Repository quản lý người dùng
- `product/` - Repository quản lý sản phẩm
- `category/` - Repository quản lý danh mục
- `order/` - Repository quản lý đơn hàng
- `review/` - Repository quản lý đánh giá
- `favorite/` - Repository quản lý yêu thích
- `voucher/` - Repository quản lý voucher

### 📁 `viewmodel/`
Chứa các ViewModel theo MVVM pattern:
- `user/` - ViewModel cho chức năng người dùng
- `product/` - ViewModel cho sản phẩm
- `category/` - ViewModel cho danh mục
- `order/` - ViewModel cho đơn hàng
- `review/` - ViewModel cho đánh giá
- `favorite/` - ViewModel cho yêu thích
- `voucher/` - ViewModel cho voucher

### 📁 `ui/`
Chứa các thành phần giao diện người dùng:

#### `activities/`
- `auth/` - Màn hình đăng nhập, đăng ký
- `home/` - Màn hình trang chủ
- `product/` - Màn hình chi tiết sản phẩm, danh sách sản phẩm
- `cart/` - Màn hình giỏ hàng
- `order/` - Màn hình đơn hàng, lịch sử đơn hàng
- `profile/` - Màn hình thông tin cá nhân

#### `fragments/`
- `home/` - Fragment trang chủ
- `product/` - Fragment sản phẩm
- `category/` - Fragment danh mục
- `cart/` - Fragment giỏ hàng
- `order/` - Fragment đơn hàng
- `profile/` - Fragment profile

#### `adapters/`
- `product/` - Adapter cho RecyclerView sản phẩm
- `category/` - Adapter cho RecyclerView danh mục
- `order/` - Adapter cho RecyclerView đơn hàng
- `review/` - Adapter cho RecyclerView đánh giá

### 📁 `utils/`
Chứa các tiện ích và helper:
- `constants/` - Các hằng số (API URLs, keys, etc.)
- `helpers/` - Các hàm helper
- `validators/` - Các hàm validation

### 📁 `services/`
Chứa các service chạy nền (nếu cần):
- Notification service
- Background sync service

### 📁 `database/`
Chứa các lớp quản lý database local (Room/SQLite) nếu cần cache dữ liệu offline

## MongoDB Collections
Các collection trong database `duan1`:
- `users` - Thông tin người dùng
- `products` - Sản phẩm quần áo
- `categories` - Danh mục sản phẩm
- `orders` - Đơn hàng
- `reviews` - Đánh giá sản phẩm
- `favorites` - Sản phẩm yêu thích
- `vouchers` - Mã giảm giá

## Layout Resources
Thư mục `res/layout/` được tổ chức:
- `activities/` - Layout cho các Activity
- `fragments/` - Layout cho các Fragment
- `items/` - Layout cho các item trong RecyclerView

## Luồng dữ liệu
```
UI (Activity/Fragment) 
  → ViewModel 
    → Repository 
      → API Service 
        → MongoDB
```

