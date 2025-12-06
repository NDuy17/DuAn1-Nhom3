# Hướng dẫn sử dụng Admin Panel

## Vấn đề thường gặp

### Lỗi "Unexpected end of JSON input"

Lỗi này xảy ra khi bạn mở trang qua **Live Server** (port 5500) thay vì qua **Node.js server** (port 3000).

## Cách khắc phục

### Cách 1: Truy cập qua Node.js Server (Khuyến nghị)

1. **Khởi động server Node.js:**
   ```bash
   cd server
   npm start
   ```
   Server sẽ chạy trên `http://localhost:3000`

2. **Truy cập admin panel:**
   - Mở trình duyệt và vào: `http://localhost:3000/login.html`
   - Hoặc: `http://localhost:3000/admin.html` (sẽ tự động redirect nếu chưa đăng nhập)

### Cách 2: Sử dụng Live Server (Đã được sửa)

Nếu bạn vẫn muốn dùng Live Server, code đã được cập nhật để tự động gọi API đến `http://localhost:3000`. 

**Lưu ý:** Bạn vẫn cần khởi động Node.js server trên port 3000 để API hoạt động.

## Đăng nhập

1. Mở trang login: `http://localhost:3000/login.html`
2. Nhập email và password của tài khoản admin trong MongoDB
3. Click "Đăng nhập"

## Kiểm tra tài khoản admin trong MongoDB

Để đảm bảo tài khoản có role admin:

```javascript
// Trong MongoDB shell hoặc MongoDB Compass
db.users.findOne({ email: "admin@gmail.com" })

// Kiểm tra role phải là "admin"
// Nếu không, cập nhật:
db.users.updateOne(
  { email: "admin@gmail.com" },
  { $set: { role: "admin" } }
)
```

## Tạo tài khoản admin mới

Nếu chưa có tài khoản admin, bạn có thể:

1. **Đăng ký qua API:**
   ```bash
   curl -X POST http://localhost:3000/api/users/register \
     -H "Content-Type: application/json" \
     -d '{"name":"Admin","email":"admin@gmail.com","password":"admin123"}'
   ```

2. **Cập nhật role thành admin:**
   ```bash
   # Cần token của user vừa tạo (hoặc dùng MongoDB trực tiếp)
   # Hoặc trong MongoDB:
   db.users.updateOne(
     { email: "admin@gmail.com" },
     { $set: { role: "admin" } }
   )
   ```

## Troubleshooting

### Server không chạy
- Kiểm tra: `npm start` trong thư mục `server`
- Kiểm tra port 3000 có bị chiếm không: `netstat -ano | findstr :3000`

### Lỗi CORS
- Đảm bảo server đang chạy trên cùng domain hoặc đã cấu hình CORS

### Token không hợp lệ
- Xóa token cũ: Mở DevTools > Application > Local Storage > Xóa `adminToken`
- Đăng nhập lại

### Không thể đăng nhập dù đúng thông tin
- Kiểm tra role trong MongoDB phải là "admin"
- Kiểm tra console browser để xem lỗi chi tiết
- Kiểm tra server logs

