# Hướng Dẫn Test Password Reset Qua Swagger

File này hướng dẫn test forgot password flow qua Swagger UI để xác định lỗi từ backend hay frontend.

## 📋 Mục Đích

- Test độc lập backend không cần frontend
- Xác định chính xác lỗi từ backend hay frontend
- Debug `failedAttempts` và lockout mechanism

---

## 🔧 Setup

1. **Truy cập Swagger UI:**
   - Local: `http://localhost:8080/swagger-ui.html`
   - Production: `https://your-railway-url/swagger-ui.html`

2. **Tìm API Authentication:**
   - Mở section `Authentication`
   - Tìm các endpoints:
     - `POST /api/auth/forgot-password/request-otp`
     - `POST /api/auth/forgot-password/verify-otp`
     - `POST /api/auth/forgot-password/reset-password`
     - `GET /api/debug/otp-check` (debug endpoint)

---

## 🧪 Test Case 1: Request OTP

### Bước 1: Request OTP

**Endpoint:** `POST /api/auth/forgot-password/request-otp`

**Request Body:**
```json
{
  "emailOrPhone": "khoacaper@gmail.com"
}
```

**Expected Response (200):**
```json
{
  "success": true,
  "message": "OTP sent successfully",
  "data": {
    "message": "OTP has been sent to your email"
  }
}
```

**Kiểm tra:**
- ✅ Status code: `200`
- ✅ Response có `success: true`
- ✅ Check email có nhận được OTP không

---

## 🧪 Test Case 2: Verify OTP - Nhập Sai Lần 1-4

### Bước 2: Nhập Sai OTP (Lần 1)

**Endpoint:** `POST /api/auth/forgot-password/verify-otp`

**Request Body:**
```json
{
  "emailOrPhone": "khoacaper@gmail.com",
  "otpCode": "123456"
}
```
*(Nhập OTP sai - không phải OTP thực tế)*

**Expected Response (400):**
```json
{
  "success": false,
  "message": "Invalid OTP code",
  "data": {
    "message": "Invalid OTP code",
    "failedAttempts": 1,
    "remainingAttempts": 4,
    "maxAttempts": 5
  }
}
```

**Kiểm tra:**
- ✅ Status code: `400`
- ✅ `failedAttempts: 1`
- ✅ `remainingAttempts: 4`

### Bước 3: Check Database (Debug Endpoint)

**Endpoint:** `GET /api/debug/otp-check?email=khoacaper@gmail.com`

**Expected Response:**
```json
{
  "latestOtp": {
    "id": 46,
    "email": "khoacaper@gmail.com",
    "otpCode": "598459",
    "failedAttempts": 1,  // ← PHẢI LÀ 1, KHÔNG PHẢI 0
    "isUsed": false,
    "createdAt": "2025-12-20T17:23:18",
    "expiresAt": "2025-12-20T17:38:18",
    "isExpired": false,
    "minutesUntilExpiry": 14
  },
  "summary": {
    "email": "khoacaper@gmail.com",
    "hasValidOtp": true,
    "hasActiveLockout": false,
    "currentFailedAttempts": 1,  // ← PHẢI LÀ 1
    "maxFailedAttempts": 5,
    "shouldBeLocked": false
  }
}
```

**Kiểm tra quan trọng:**
- ✅ `latestOtp.failedAttempts` phải là `1` (không phải `0`)
- ✅ `summary.currentFailedAttempts` phải là `1`

### Bước 4: Nhập Sai OTP (Lần 2-4)

Lặp lại **Bước 2** với cùng OTP sai.

**Expected Response sau mỗi lần:**
- Lần 2: `failedAttempts: 2`, `remainingAttempts: 3`
- Lần 3: `failedAttempts: 3`, `remainingAttempts: 2`
- Lần 4: `failedAttempts: 4`, `remainingAttempts: 1`

**Sau mỗi lần, check debug endpoint:**
- ✅ `failedAttempts` phải tăng: 1 → 2 → 3 → 4

---

## 🧪 Test Case 3: Verify OTP - Nhập Sai Lần 5 (Lockout)

### Bước 5: Nhập Sai OTP (Lần 5)

**Endpoint:** `POST /api/auth/forgot-password/verify-otp`

**Request Body:**
```json
{
  "emailOrPhone": "khoacaper@gmail.com",
  "otpCode": "123456"
}
```

**Expected Response (429):**
```json
{
  "success": false,
  "message": "Too many failed attempts. Password reset function is temporarily locked for 30 minutes."
}
```

**Kiểm tra:**
- ✅ Status code: `429` (Too Many Requests)
- ✅ Message có chứa "locked" hoặc "30 minutes"

### Bước 6: Check Lockout (Debug Endpoint)

**Endpoint:** `GET /api/debug/otp-check?email=khoacaper@gmail.com`

**Expected Response:**
```json
{
  "latestOtp": {
    "failedAttempts": 5  // ← PHẢI LÀ 5
  },
  "activeLockouts": [
    {
      "id": 1,
      "email": "khoacaper@gmail.com",
      "lockedAt": "2025-12-20T17:30:00",
      "unlockAt": "2025-12-20T18:00:00",
      "reason": "Too many failed OTP attempts",
      "isUnlocked": false,
      "lockoutStatus": "ACTIVE",
      "minutesUntilUnlock": 30
    }
  ],
  "summary": {
    "hasActiveLockout": true,  // ← PHẢI LÀ true
    "currentFailedAttempts": 5,
    "shouldBeLocked": true
  }
}
```

**Kiểm tra quan trọng:**
- ✅ `latestOtp.failedAttempts` phải là `5`
- ✅ `activeLockouts` phải có ít nhất 1 record
- ✅ `summary.hasActiveLockout` phải là `true`

---

## 🧪 Test Case 4: Request OTP Khi Đang Lockout

### Bước 7: Request OTP Mới Khi Đang Lockout

**Endpoint:** `POST /api/auth/forgot-password/request-otp`

**Request Body:**
```json
{
  "emailOrPhone": "khoacaper@gmail.com"
}
```

**Expected Response (429):**
```json
{
  "success": false,
  "message": "Password reset function is temporarily locked due to too many failed attempts. Please try again in 30 minutes."
}
```

**Kiểm tra:**
- ✅ Status code: `429`
- ✅ Không được tạo OTP mới
- ✅ Không được gửi email

---

## 🧪 Test Case 5: Verify OTP Đúng

### Bước 8: Request OTP Mới (Sau Khi Hết Lockout)

**Chờ 30 phút hoặc xóa lockout record trong database**, sau đó:

**Endpoint:** `POST /api/auth/forgot-password/request-otp`

**Request Body:**
```json
{
  "emailOrPhone": "khoacaper@gmail.com"
}
```

**Expected Response (200):**
- ✅ Status code: `200`
- ✅ OTP được gửi qua email

### Bước 9: Verify OTP Đúng

**Lấy OTP từ email**, sau đó:

**Endpoint:** `POST /api/auth/forgot-password/verify-otp`

**Request Body:**
```json
{
  "emailOrPhone": "khoacaper@gmail.com",
  "otpCode": "598459"  // ← OTP thực tế từ email
}
```

**Expected Response (200):**
```json
{
  "success": true,
  "message": "OTP verified successfully",
  "data": {
    "message": "OTP verified successfully",
    "verified": true
  }
}
```

**Kiểm tra:**
- ✅ Status code: `200`
- ✅ `verified: true`
- ✅ Check debug endpoint: `latestOtp.isUsed` phải là `true`

---

## 🧪 Test Case 6: Reset Password

### Bước 10: Reset Password

**Endpoint:** `POST /api/auth/forgot-password/reset-password`

**Request Body:**
```json
{
  "emailOrPhone": "khoacaper@gmail.com",
  "otpCode": "598459",  // ← OTP đã verify ở bước 9
  "newPassword": "NewPassword123!",
  "confirmPassword": "NewPassword123!"
}
```

**Expected Response (200):**
```json
{
  "success": true,
  "message": "Password reset successfully"
}
```

**Kiểm tra:**
- ✅ Status code: `200`
- ✅ Có thể login với password mới

---

## 🔍 Debug Checklist

### Nếu `failedAttempts` không tăng:

1. **Check logs trên Railway:**
   - Tìm log: `"Incremented failed attempts for OTP ID"`
   - Xem `old` và `new` values

2. **Check transaction:**
   - Log phải có: `"Incremented failed attempts for OTP ID: X, old: Y, new: Z"`
   - Nếu `old` và `new` giống nhau → transaction rollback

3. **Check database trực tiếp:**
   - Query: `SELECT id, email, failed_attempts FROM password_reset_otp WHERE email = 'khoacaper@gmail.com' ORDER BY created_at DESC LIMIT 1;`
   - `failed_attempts` phải tăng sau mỗi lần nhập sai

### Nếu không lockout sau 5 lần:

1. **Check `failedAttempts`:**
   - Phải đạt `5` trước khi lockout
   - Debug endpoint: `summary.currentFailedAttempts` phải là `5`

2. **Check lockout table:**
   - Query: `SELECT * FROM password_reset_lockout WHERE email = 'khoacaper@gmail.com' ORDER BY locked_at DESC LIMIT 1;`
   - Phải có record với `is_unlocked = false` và `unlock_at > NOW()`

---

## 📝 Notes

- **Test tuần tự:** Phải test theo thứ tự từ Test Case 1 → 6
- **Email thực tế:** Dùng email thực để nhận OTP
- **OTP từ email:** Copy OTP chính xác từ email (6 chữ số)
- **Timeout:** OTP hết hạn sau 15 phút
- **Lockout:** Lockout kéo dài 30 phút

---

## 🐛 Troubleshooting

### Vấn đề: `failedAttempts` luôn là 0

**Nguyên nhân có thể:**
- Transaction rollback khi throw exception
- Code chưa được deploy
- Cache issue

**Giải pháp:**
- Check logs xem có `"Incremented failed attempts"` không
- Check code đã có `@Transactional(propagation = Propagation.REQUIRES_NEW)` chưa
- Restart application

### Vấn đề: Không lockout sau 5 lần

**Nguyên nhân có thể:**
- `failedAttempts` không đạt 5
- Logic lockout chưa được trigger

**Giải pháp:**
- Check `failedAttempts` qua debug endpoint
- Check logs xem có `"Password reset lockout created"` không

---

## ✅ Kết Luận

Sau khi test qua Swagger:

- **Nếu backend hoạt động đúng:** `failedAttempts` tăng đúng, lockout sau 5 lần → Lỗi từ frontend
- **Nếu backend không hoạt động đúng:** `failedAttempts` không tăng hoặc không lockout → Lỗi từ backend, cần fix code

