# Forgot Password Flow Documentation

## 📋 Tổng Quan

Hệ thống hỗ trợ reset password qua email với OTP (One-Time Password) 6 chữ số. Flow bao gồm 3 bước:
1. **Request OTP** - User yêu cầu gửi mã OTP qua email
2. **Verify OTP** - User nhập mã OTP để xác thực
3. **Reset Password** - User đặt lại mật khẩu mới

## 🔐 Security Features

### Brute-Force Protection
- **Max Failed Attempts**: 5 lần
- **Lockout Duration**: 30 phút
- **Lockout Scope**: Theo email và/hoặc IP address
- Khi nhập sai OTP quá 5 lần, hệ thống sẽ khóa chức năng forgot password cho email/IP đó trong 30 phút

### OTP Security
- **OTP Length**: 6 chữ số (100000-999999)
- **OTP Expiry**: 15 phút sau khi tạo
- **OTP Usage**: Mỗi OTP chỉ được sử dụng 1 lần (sau khi verify thành công)
- **OTP Reuse**: Không cho phép reuse OTP đã được verify

---

## 🔄 Flow Chi Tiết

### Step 1: Request OTP

**Endpoint:** `POST /api/auth/forgot-password/request-otp`

**Request Body:**
```json
{
  "emailOrPhone": "user@example.com"
}
```

**Nghiệp vụ:**
1. Validate email format
2. Check lockout status (email/IP) - nếu bị lockout → trả về error
3. Kiểm tra user tồn tại (không tiết lộ nếu không tồn tại - security best practice)
4. Kiểm tra account active status
5. Tạo OTP 6 chữ số ngẫu nhiên
6. Lưu OTP vào database với:
   - `isUsed = false`
   - `failedAttempts = 0`
   - `expiresAt = now + 15 minutes`
7. Gửi email chứa OTP code
8. Return success (ngay cả khi email không tồn tại để không tiết lộ thông tin)

**Response Success:**
```json
{
  "success": true,
  "message": "OTP sent successfully",
  "data": {
    "message": "OTP has been sent successfully to your email",
    "method": "email"
  }
}
```

**Response Error (Lockout):**
```json
{
  "success": false,
  "message": "Password reset function is temporarily locked due to too many failed attempts. Please try again in 30 minutes.",
  "status": 429
}
```

**Response Error (Account Deactivated):**
```json
{
  "success": false,
  "message": "Account is deactivated",
  "status": 403
}
```

---

### Step 2: Verify OTP

**Endpoint:** `POST /api/auth/forgot-password/verify-otp`

**Request Body:**
```json
{
  "emailOrPhone": "user@example.com",
  "otpCode": "123456"
}
```

**Nghiệp vụ:**
1. Validate email format và OTP format (6 chữ số)
2. Check lockout status (email/IP) - nếu bị lockout → trả về error
3. Tìm OTP hợp lệ theo email:
   - `isUsed = false`
   - `expiresAt > now`
   - `failedAttempts < 5`
4. Kiểm tra OTP code:
   - Nếu đúng → Mark OTP as used (`isUsed = true`)
   - Nếu sai → Increment `failedAttempts`
     - Nếu `failedAttempts >= 5` → Tạo lockout → Trả về error
5. Return success nếu verify thành công

**Response Success:**
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

**Response Error (Invalid OTP):**
```json
{
  "success": false,
  "message": "Invalid OTP code",
  "status": 400
}
```

**Response Error (Expired OTP):**
```json
{
  "success": false,
  "message": "Invalid or expired OTP",
  "status": 400
}
```

**Response Error (Lockout - Too Many Failed Attempts):**
```json
{
  "success": false,
  "message": "Too many failed attempts. Password reset function is temporarily locked for 30 minutes.",
  "status": 429
}
```

**Response Error (Already Locked):**
```json
{
  "success": false,
  "message": "Password reset function is temporarily locked due to too many failed attempts. Please try again in 30 minutes.",
  "status": 429
}
```

---

### Step 3: Reset Password

**Endpoint:** `POST /api/auth/forgot-password/reset`

**Request Body:**
```json
{
  "emailOrPhone": "user@example.com",
  "otpCode": "123456",
  "newPassword": "newPassword123",
  "confirmPassword": "newPassword123"
}
```

**Nghiệp vụ:**
1. Validate password và confirm password match
2. Validate email format
3. Tìm OTP đã được verify (`isUsed = true`) theo email và OTP code
4. Nếu không tìm thấy OTP đã verify:
   - Fallback: Tìm OTP chưa verify và verify lại (trường hợp user skip bước verify-otp)
   - Nếu vẫn không tìm thấy → Error
5. Tìm user theo email
6. Update password (hash với BCrypt)
7. Return success

**Response Success:**
```json
{
  "success": true,
  "message": "Password reset successfully",
  "data": {
    "message": "Password reset successfully"
  }
}
```

**Response Error (OTP Not Verified):**
```json
{
  "success": false,
  "message": "Invalid or expired OTP. Please verify OTP first.",
  "status": 400
}
```

**Response Error (Password Mismatch):**
```json
{
  "success": false,
  "message": "Password and confirmation do not match",
  "status": 400
}
```

---

## 🛡️ Brute-Force Protection Details

### Khi Nào Tạo Lockout?

Lockout được tạo khi:
- User nhập sai OTP lần thứ 5 (`failedAttempts >= 5`)
- Lockout được tạo cho cả email và IP address của request đó

### Lockout Scope

Lockout áp dụng cho:
- **Email**: Tất cả requests từ email đó
- **IP Address**: Tất cả requests từ IP đó (có thể là nhiều users cùng IP)

**Lưu ý:** Lockout check cả email VÀ IP, nên nếu một trong hai bị lockout thì request sẽ bị reject.

### Lockout Duration

- **Mặc định**: 30 phút (`LOCKOUT_DURATION_MINUTES = 30`)
- **Tự động unlock**: Sau khi `unlockAt` đã qua
- **Manual unlock**: Admin có thể unlock thủ công (set `isUnlocked = true`)

### Lockout Check Points

Lockout được check tại:
1. **Request OTP**: Trước khi tạo OTP mới
2. **Verify OTP**: Trước khi verify OTP code

---

## 📊 Database Schema

### password_reset_otp Table

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGINT | Primary key |
| `email` | VARCHAR(255) | Email của user |
| `otp_code` | VARCHAR(6) | Mã OTP 6 chữ số |
| `expires_at` | TIMESTAMP | Thời gian hết hạn (15 phút sau khi tạo) |
| `is_used` | BOOLEAN | Đã sử dụng chưa (sau khi verify thành công) |
| `failed_attempts` | INTEGER | Số lần verify sai (max 5) |
| `created_at` | TIMESTAMP | Thời gian tạo OTP |

**Indexes:**
- `idx_otp_email` on `email`
- `idx_otp_code` on `otp_code`

**Unique Constraint:** Không có (cho phép nhiều OTP cho cùng một email)

---

### password_reset_lockout Table

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGINT | Primary key |
| `email` | VARCHAR(255) | Email bị khóa (nullable) |
| `ip_address` | VARCHAR(45) | IP address bị khóa (nullable) |
| `locked_at` | TIMESTAMP | Thời gian bắt đầu khóa |
| `unlock_at` | TIMESTAMP | Thời gian mở khóa (30 phút sau locked_at) |
| `reason` | VARCHAR(500) | Lý do khóa (ví dụ: "Too many failed OTP attempts") |
| `is_unlocked` | BOOLEAN | Đã mở khóa thủ công chưa |
| `created_at` | TIMESTAMP | Thời gian tạo record |

**Indexes:**
- `idx_lockout_email` on `email`
- `idx_lockout_ip` on `ip_address`
- `idx_lockout_unlock_at` on `unlock_at`
- `idx_lockout_active` on `(is_unlocked, unlock_at)` WHERE `is_unlocked = FALSE`

**Lưu ý:** 
- `email` và `ip_address` đều nullable - có thể lock theo email hoặc IP hoặc cả hai
- Lockout được coi là active nếu: `is_unlocked = false` AND `unlock_at > now`

---

## 🔧 API Endpoints Reference

### 1. Request OTP

**Endpoint:** `POST /api/auth/forgot-password/request-otp`

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "emailOrPhone": "user@example.com"
}
```

**Success Response (200):**
```json
{
  "success": true,
  "message": "OTP sent successfully",
  "data": {
    "message": "OTP has been sent successfully to your email",
    "method": "email"
  }
}
```

**Error Responses:**
- `400 BAD_REQUEST`: Invalid email format
- `403 FORBIDDEN`: Account is deactivated
- `429 TOO_MANY_REQUESTS`: Lockout active (quá nhiều lần sai)

---

### 2. Verify OTP

**Endpoint:** `POST /api/auth/forgot-password/verify-otp`

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "emailOrPhone": "user@example.com",
  "otpCode": "123456"
}
```

**Success Response (200):**
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

**Error Responses:**
- `400 BAD_REQUEST`: Invalid OTP code, expired OTP, invalid format
- `429 TOO_MANY_REQUESTS`: Lockout active (quá nhiều lần sai)

---

### 3. Reset Password

**Endpoint:** `POST /api/auth/forgot-password/reset`

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "emailOrPhone": "user@example.com",
  "otpCode": "123456",
  "newPassword": "newPassword123",
  "confirmPassword": "newPassword123"
}
```

**Success Response (200):**
```json
{
  "success": true,
  "message": "Password reset successfully",
  "data": {
    "message": "Password reset successfully"
  }
}
```

**Error Responses:**
- `400 BAD_REQUEST`: Password mismatch, invalid OTP, OTP not verified
- `404 NOT_FOUND`: User not found

---

## 💻 Frontend Integration Guide

### 1. Handle HTTP Status Codes

```javascript
// Example với axios
import axios from 'axios';

async function requestOtp(email) {
  try {
    const response = await axios.post('/api/auth/forgot-password/request-otp', {
      emailOrPhone: email
    });
    
    if (response.data.success) {
      showSuccess('OTP has been sent to your email');
      return true;
    }
  } catch (error) {
    if (error.response) {
      const status = error.response.status;
      const message = error.response.data.message;
      
      if (status === 429) {
        // Brute-force protection triggered
        showError(message);
        disableForgotPasswordForm();
        // Optional: Parse minutes và hiển thị countdown
        const minutes = parseLockoutMinutes(message);
        if (minutes) {
          startCountdownTimer(minutes);
        }
        return false;
      } else if (status === 403) {
        showError('Account is deactivated. Please contact support.');
        return false;
      } else {
        showError(message || 'Failed to send OTP');
        return false;
      }
    } else {
      showError('Network error. Please try again.');
      return false;
    }
  }
}

async function verifyOtp(email, otpCode) {
  try {
    const response = await axios.post('/api/auth/forgot-password/verify-otp', {
      emailOrPhone: email,
      otpCode: otpCode
    });
    
    if (response.data.success) {
      showSuccess('OTP verified successfully');
      return true;
    }
  } catch (error) {
    if (error.response) {
      const status = error.response.status;
      const message = error.response.data.message;
      
      if (status === 429) {
        // Lockout triggered
        showError(message);
        disableForgotPasswordForm();
        const minutes = parseLockoutMinutes(message);
        if (minutes) {
          startCountdownTimer(minutes);
        }
        return false;
      } else if (status === 400) {
        // Invalid OTP
        showError(message || 'Invalid OTP code');
        // Increment failed attempts counter (optional)
        incrementFailedAttempts();
        return false;
      } else {
        showError(message || 'Failed to verify OTP');
        return false;
      }
    } else {
      showError('Network error. Please try again.');
      return false;
    }
  }
}

async function resetPassword(email, otpCode, newPassword, confirmPassword) {
  try {
    const response = await axios.post('/api/auth/forgot-password/reset', {
      emailOrPhone: email,
      otpCode: otpCode,
      newPassword: newPassword,
      confirmPassword: confirmPassword
    });
    
    if (response.data.success) {
      showSuccess('Password reset successfully');
      redirectToLogin();
      return true;
    }
  } catch (error) {
    if (error.response) {
      const message = error.response.data.message;
      showError(message || 'Failed to reset password');
      return false;
    } else {
      showError('Network error. Please try again.');
      return false;
    }
  }
}
```

### 2. Parse Lockout Minutes (Optional)

```javascript
function parseLockoutMinutes(message) {
  // Parse từ message: "Please try again in 30 minutes"
  const match = message.match(/try again in (\d+) minutes?/i);
  return match ? parseInt(match[1]) : null;
}
```

### 3. Countdown Timer (Optional)

```javascript
function startCountdownTimer(minutes) {
  let remainingSeconds = minutes * 60;
  
  const timerElement = document.getElementById('lockoutTimer');
  const formElement = document.getElementById('forgotPasswordForm');
  
  // Disable form
  formElement.style.pointerEvents = 'none';
  formElement.style.opacity = '0.5';
  
  const interval = setInterval(() => {
    const mins = Math.floor(remainingSeconds / 60);
    const secs = remainingSeconds % 60;
    
    timerElement.textContent = `Please try again in ${mins}:${secs.toString().padStart(2, '0')}`;
    
    if (remainingSeconds <= 0) {
      clearInterval(interval);
      timerElement.textContent = '';
      formElement.style.pointerEvents = 'auto';
      formElement.style.opacity = '1';
    }
    
    remainingSeconds--;
  }, 1000);
}
```

### 4. Failed Attempts Counter (Optional)

```javascript
let failedAttempts = 0;
const MAX_ATTEMPTS = 5;

function incrementFailedAttempts() {
  failedAttempts++;
  updateAttemptsDisplay();
  
  if (failedAttempts >= MAX_ATTEMPTS) {
    showWarning(`You have ${MAX_ATTEMPTS - failedAttempts} attempts remaining`);
  }
}

function resetFailedAttempts() {
  failedAttempts = 0;
  updateAttemptsDisplay();
}

function updateAttemptsDisplay() {
  const remaining = MAX_ATTEMPTS - failedAttempts;
  document.getElementById('attemptsRemaining').textContent = 
    remaining > 0 ? `${remaining} attempts remaining` : '';
}
```

---

## 📝 Email Template

Email OTP được gửi với format:

**Subject:** `Mã OTP đặt lại mật khẩu - Hokori`

**Body:**
```
Xin chào,

Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản Hokori của bạn.

Mã OTP của bạn là: 123456

Mã này có hiệu lực trong 15 phút.

Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.

Trân trọng,
Đội ngũ Hokori
```

---

## 🔍 Error Handling

### Common Error Scenarios

| Scenario | HTTP Status | Message | Frontend Action |
|----------|-------------|---------|-----------------|
| Invalid email format | 400 | "Only email is supported..." | Show validation error |
| Account deactivated | 403 | "Account is deactivated" | Show error, suggest contact support |
| Invalid OTP code | 400 | "Invalid OTP code" | Show error, allow retry |
| Expired OTP | 400 | "Invalid or expired OTP" | Show error, suggest request new OTP |
| Lockout active | 429 | "Password reset function is temporarily locked..." | Show error, disable form, show countdown |
| Password mismatch | 400 | "Password and confirmation do not match" | Show validation error |
| OTP not verified | 400 | "Invalid or expired OTP. Please verify OTP first." | Show error, redirect to verify step |

---

## 🧪 Testing Scenarios

### Test Case 1: Happy Path
1. Request OTP → Success
2. Verify OTP với code đúng → Success
3. Reset password với password hợp lệ → Success

### Test Case 2: Invalid OTP
1. Request OTP → Success
2. Verify OTP với code sai (lần 1-4) → Error "Invalid OTP code"
3. Verify OTP với code đúng → Success

### Test Case 3: Brute-Force Protection
1. Request OTP → Success
2. Verify OTP sai 5 lần → Lockout triggered
3. Request OTP mới → Error 429 (Lockout active)
4. Verify OTP → Error 429 (Lockout active)
5. Đợi 30 phút → Lockout expires
6. Request OTP → Success

### Test Case 4: Expired OTP
1. Request OTP → Success
2. Đợi 15 phút → OTP expires
3. Verify OTP → Error "Invalid or expired OTP"
4. Request OTP mới → Success

### Test Case 5: Reset Password Without Verify
1. Request OTP → Success
2. Skip verify step, directly reset password → Error "Invalid or expired OTP. Please verify OTP first."
3. Verify OTP → Success
4. Reset password → Success

---

## 🔐 Security Best Practices

### Backend
- ✅ Không tiết lộ user không tồn tại (return success ngay cả khi email không tồn tại)
- ✅ OTP chỉ được sử dụng 1 lần (mark as used sau khi verify)
- ✅ OTP có thời gian hết hạn (15 phút)
- ✅ Brute-force protection với lockout 30 phút
- ✅ Track cả email và IP address
- ✅ Password được hash với BCrypt
- ✅ Validate tất cả inputs

### Frontend
- ✅ Handle tất cả HTTP status codes
- ✅ Hiển thị message rõ ràng cho user
- ✅ Disable form khi bị lockout
- ✅ (Optional) Hiển thị countdown timer
- ✅ (Optional) Track failed attempts trên client side
- ✅ Validate password strength trước khi submit

---

## 📚 Related Files

### Backend
- `src/main/java/com/hokori/web/service/PasswordResetService.java` - Main service logic
- `src/main/java/com/hokori/web/controller/AuthController.java` - API endpoints
- `src/main/java/com/hokori/web/entity/PasswordResetOtp.java` - OTP entity
- `src/main/java/com/hokori/web/entity/PasswordResetLockout.java` - Lockout entity
- `src/main/java/com/hokori/web/repository/PasswordResetOtpRepository.java` - OTP repository
- `src/main/java/com/hokori/web/repository/PasswordResetLockoutRepository.java` - Lockout repository
- `src/main/java/com/hokori/web/service/EmailService.java` - Email sending service
- `src/main/resources/db/migration/V2025_12_20_001__create_password_reset_lockout_table.sql` - Migration

### Frontend (Example)
- `forgot-password-page.jsx` - Forgot password page component
- `otp-verification-page.jsx` - OTP verification component
- `reset-password-page.jsx` - Reset password component

---

## 🚀 Deployment Notes

### Environment Variables

Các biến môi trường cần thiết cho email service:

```bash
# Gmail SMTP Configuration
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your-email@gmail.com
SPRING_MAIL_PASSWORD=your-app-password  # Gmail App Password (16 ký tự)
SPRING_MAIL_SMTP_AUTH=true
SPRING_MAIL_SMTP_STARTTLS_ENABLE=true
APP_EMAIL_ENABLED=true
```

### Database Migration

Migration sẽ tự động chạy khi deploy:
- `V2025_12_20_001__create_password_reset_lockout_table.sql` - Tạo bảng lockout

---

## 📞 Support & Troubleshooting

### Common Issues

**Issue 1: Email không được gửi**
- Check `APP_EMAIL_ENABLED=true`
- Check Gmail App Password đúng chưa
- Check logs trên Railway để xem error

**Issue 2: OTP không hoạt động sau khi verify**
- Đảm bảo đã verify OTP trước khi reset password
- Check OTP chưa hết hạn (15 phút)
- Check OTP chưa bị mark as used

**Issue 3: Lockout không tự động unlock**
- Lockout tự động unlock sau 30 phút
- Check `unlock_at` trong database
- Admin có thể unlock thủ công nếu cần

---

**Last Updated:** 2025-12-20  
**Version:** 1.0

