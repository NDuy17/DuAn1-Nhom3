# DuAn1-Nhom3

## Cấu hình môi trường server (MongoDB cục bộ)
Tạo file `server/.env` để trỏ về MongoDB đang chạy trên máy của bạn:

```
MONGODB_URI=mongodb://127.0.0.1:27017/duan1
JWT_SECRET=thay_bang_chuoi_bao_mat
PORT=3000
MAIL_USER=you@example.com
MAIL_PASS=app-password
```

Sau đó chạy:

```
cd server
npm install
npm start
```

## Tạo danh mục (category)
- **Tạo mới:** `POST http://localhost:3000/api/categories`
  ```json
  {
    "name": "Áo thun",
    "description": "Các mẫu áo thun"
  }
  ```
  Response trả về `_id` dùng khi thêm sản phẩm.
- **Xem danh sách:** `GET http://localhost:3000/api/categories`

## Thêm sản phẩm nhanh bằng Postman (1 request form-data)
- Endpoint: `POST http://localhost:3000/api/products` (Android emulator dùng `http://10.0.2.2:3000/api/products`)
- Body: chọn `form-data` và điền:
  | Key          | Type | Value ví dụ                         |
  |--------------|------|-------------------------------------|
  | `image`      | File | (chọn file ảnh trong máy)           |
  | `name`       | Text | Áo thun basic                       |
  | `description`| Text | Chất cotton 100%, form rộng         |
  | `price`      | Text | 199000                              |
  | `stock`      | Text | 50                                  |
  | `category`   | Text | `_id` của category (ví dụ 6760...)  |

Server sẽ tự lưu ảnh vào `uploads/` và tạo sản phẩm với đường dẫn ảnh nội bộ. Ứng dụng Android hiển thị ảnh bằng URL `http://<server>/uploads/...` (emulator: `http://10.0.2.2:3000/uploads/...`).
