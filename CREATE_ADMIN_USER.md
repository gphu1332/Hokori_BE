# Hướng Dẫn Tạo Tài Khoản Admin

## ✅ Cách 1: Dùng Swagger UI (Khuyến nghị)

### Bước 1: Mở Swagger UI
```
https://hokoribe-production.up.railway.app/swagger-ui.html
```

### Bước 2: Tìm endpoint
- Tìm section **"Authentication"**
- Tìm endpoint: **`POST /api/auth/register`**
- Click **"Try it out"**

### Bước 3: Nhập thông tin
Copy và paste JSON này vào Request body:

```json
{
  "username": "admin",
  "email": "admin@hokori.com",
  "password": "admin123",
  "confirmPassword": "admin123",
  "displayName": "System Administrator",
  "roleName": "ADMIN",
  "country": "Vietnam",
  "nativeLanguage": "Vietnamese",
  "currentJlptLevel": "N5"
}
```

### Bước 4: Execute
- Click nút **"Execute"**
- Kiểm tra response:
  - Status: `200 OK`
  - Response body có `accessToken` và `refreshToken`
  - User object có `role: "ADMIN"`

---

## ✅ Cách 2: Dùng cURL (Terminal)

```bash
curl -X POST "https://hokoribe-production.up.railway.app/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "email": "admin@hokori.com",
    "password": "admin123",
    "confirmPassword": "admin123",
    "displayName": "System Administrator",
    "roleName": "ADMIN",
    "country": "Vietnam",
    "nativeLanguage": "Vietnamese",
    "currentJlptLevel": "N5"
  }'
```

---

## ✅ Cách 3: Dùng Postman hoặc Insomnia

1. **Method**: `POST`
2. **URL**: `https://hokoribe-production.up.railway.app/api/auth/register`
3. **Headers**:
   ```
   Content-Type: application/json
   ```
4. **Body** (raw JSON):
   ```json
   {
     "username": "admin",
     "email": "admin@hokori.com",
     "password": "admin123",
     "confirmPassword": "admin123",
     "displayName": "System Administrator",
     "roleName": "ADMIN",
     "country": "Vietnam",
     "nativeLanguage": "Vietnamese",
     "currentJlptLevel": "N5"
   }
   ```

---

## ✅ Kiểm tra đăng nhập

Sau khi tạo xong, test đăng nhập:

### Swagger UI:
1. Tìm endpoint: **`POST /api/auth/login`**
2. Request body:
   ```json
   {
     "username": "admin",
     "password": "admin123"
   }
   ```
3. Response sẽ có `accessToken` - dùng token này để truy cập các endpoint admin

---

## ✅ Test Admin Endpoints

Sau khi có `accessToken`:

1. **Copy token** từ response
2. Trong Swagger UI, click nút **"Authorize"** (🔒 ở góc trên bên phải)
3. Nhập: `Bearer {your_access_token}`
   - Ví dụ: `Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`
4. Click **"Authorize"**
5. Test các endpoint trong section **"Admin Management"**:
   - `GET /api/admin/users` - Xem danh sách users
   - `GET /api/admin/dashboard` - Xem dashboard
   - `GET /api/admin/stats` - Xem thống kê

---

## ⚠️ Lưu ý

1. **Username và Email phải unique**: Nếu đã tồn tại, sẽ báo lỗi
2. **Password**: Phải match với `confirmPassword`
3. **Role**: Phải là một trong: `LEARNER`, `TEACHER`, `STAFF`, `ADMIN`
4. **Sau khi tạo**: Lưu lại `accessToken` để dùng cho các request admin

---

## 🔐 Bảo mật

Sau khi tạo admin thành công, nên:
- Đổi password mạnh hơn
- Không chia sẻ token
- Sử dụng HTTPS (Railway đã tự động)

