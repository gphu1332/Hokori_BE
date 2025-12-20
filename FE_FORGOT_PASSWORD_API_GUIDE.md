# Forgot Password API Guide for Frontend

## 📋 Tổng Quan

Hệ thống hỗ trợ reset password qua email với OTP (One-Time Password) 6 chữ số. Flow bao gồm 3 bước:
1. **Request OTP** - User yêu cầu gửi mã OTP qua email
2. **Verify OTP** - User nhập mã OTP để xác thực
3. **Reset Password** - User đặt lại mật khẩu mới

**Base URL:** 
- **Production:** `https://api.hokori-backend.org` (hoặc Railway URL của bạn)
- **Local Development:** `http://localhost:8080` (khi chạy backend local)

**Lưu ý về CORS:**
- ✅ **FE local (localhost) CÓ THỂ gọi API** - Backend đã cấu hình CORS để cho phép localhost
- ✅ **FE deployed trên Vercel CÓ THỂ gọi API** - Backend cho phép tất cả origins
- ⚠️ **Nếu FE local không gọi được API**, kiểm tra:
  1. Backend có đang chạy không? (http://localhost:8080/api/health)
  2. FE có đang dùng đúng API endpoint không?
  3. Browser console có lỗi CORS không?

---

## 🔄 Flow Diagram

```
User Request OTP
    ↓
[POST /api/auth/forgot-password/request-otp]y8


ul
    ↓
OTP sent to email
    ↓
User Enter OTP
    ↓
[POST /api/auth/forgot-password/verify-otp]
    ↓
OTP Verified ✅
    ↓
User Enter New Password
    ↓
[POST /api/auth/forgot-password/reset]
    ↓
Password Reset Success ✅
```

---

## 📡 API Endpoints

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

**Success Response (200 OK):**
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

| Status | Message | Action |
|--------|---------|--------|
| `400` | "Only email is supported for password reset. Please provide a valid email address." | Show validation error |
| `403` | "Account is deactivated" | Show error, suggest contact support |
| `429` | "Password reset function is temporarily locked due to too many failed attempts. Please try again in X minutes." | Show error, disable form, show countdown |

**Lưu ý:**
- Ngay cả khi email không tồn tại, API vẫn trả về success (security best practice)
- Không tiết lộ thông tin user không tồn tại

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

**Validation:**
- `emailOrPhone`: Required, must contain "@"
- `otpCode`: Required, must be exactly 6 digits (0-9)

**Success Response (200 OK):**
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

| Status | Message | Action |
|--------|---------|--------|
| `400` | "Only email is supported. Please provide a valid email address." | Show validation error |
| `400` | "Invalid OTP code" | Show error, allow retry, display failed attempts info |
| `400` | "Invalid or expired OTP" | Show error, suggest request new OTP |
| `429` | "Password reset function is temporarily locked due to too many failed attempts. Please try again in X minutes." | Show error, disable form, show countdown |
| `429` | "Too many failed attempts. Password reset function is temporarily locked for 30 minutes." | Show error, disable form, show countdown |
| `429` | "OTP has been locked due to too many failed attempts. Password reset function is temporarily locked for 30 minutes." | Show error, disable form, show countdown |

**Error Response với Failed Attempts Info (400 - Invalid OTP code):**

Khi nhập sai OTP, response sẽ bao gồm thông tin về số lần đã nhập sai:

```json
{
  "success": false,
  "message": "Invalid OTP code",
  "data": {
    "message": "Invalid OTP code",
    "failedAttempts": 2,
    "remainingAttempts": 3,
    "maxAttempts": 5
  },
  "meta": {},
  "timestamp": "2025-12-20T13:39:37.512052204"
}
```

**Fields trong `data`:**
- `failedAttempts` (integer): Số lần đã nhập sai OTP (1-5)
- `remainingAttempts` (integer): Số lần còn lại có thể thử (4-0)
- `maxAttempts` (integer): Tổng số lần tối đa được phép (5)

**Lưu ý quan trọng:**
- Sau khi nhập sai OTP **5 lần**, lần thứ 6 sẽ trả về **429 (TOO_MANY_REQUESTS)** với lockout message, KHÔNG phải 400
- Lockout được áp dụng ngay lập tức sau lần sai thứ 5
- FE nên hiển thị `remainingAttempts` để user biết còn bao nhiêu lần thử lại

**Lưu ý:**
- OTP có hiệu lực trong 15 phút
- Mỗi OTP chỉ được sử dụng 1 lần
- Sau khi verify thành công, user có thể reset password

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

**Validation:**
- `emailOrPhone`: Required, must contain "@"
- `otpCode`: Required, must be exactly 6 digits (0-9)
- `newPassword`: Required, minimum 6 characters
- `confirmPassword`: Required, must match `newPassword`

**Success Response (200 OK):**
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

| Status | Message | Action |
|--------|---------|--------|
| `400` | "Only email is supported. Please provide a valid email address." | Show validation error |
| `400` | "Password and confirmation do not match" | Show validation error |
| `400` | "Invalid or expired OTP. Please verify OTP first." | Show error, redirect to verify step |
| `404` | "User not found" | Show error |

**Lưu ý:**
- Phải verify OTP trước khi reset password
- OTP đã được verify có thể dùng để reset password ngay cả khi đã hết hạn (vì đã được verify)

---

## 🛡️ Brute-Force Protection

### Khi Nào Bị Lockout?

- Khi user nhập sai OTP **5 lần** (`MAX_FAILED_ATTEMPTS = 5`)
- Lockout áp dụng cho cả **email** và **IP address**

### Lockout Duration

- **30 phút** (`LOCKOUT_DURATION_MINUTES = 30`)
- Tự động unlock sau khi hết hạn

### Lockout Scope

Lockout check cả email VÀ IP, nên nếu một trong hai bị lockout thì request sẽ bị reject.

### Lockout Check Points

Lockout được check tại:
1. **Request OTP** - Trước khi tạo OTP mới
2. **Verify OTP** - Trước khi verify OTP code

---

## 🔍 Error Handling Guide

### HTTP Status Codes

| Status Code | Meaning | When |
|-------------|---------|------|
| `200 OK` | Success | Request thành công |
| `400 BAD_REQUEST` | Invalid input | Email format sai, OTP invalid, password mismatch |
| `403 FORBIDDEN` | Permission denied | Account deactivated |
| `429 TOO_MANY_REQUESTS` | Rate limited | Lockout active (quá nhiều lần sai) |
| `404 NOT_FOUND` | Resource not found | User không tồn tại (chỉ ở reset password) |
| `500 INTERNAL_SERVER_ERROR` | Server error | Lỗi hệ thống |

### Common Error Messages

**Request OTP:**
- `"Only email is supported for password reset. Please provide a valid email address."` (400)
- `"Account is deactivated"` (403)
- `"Password reset function is temporarily locked due to too many failed attempts. Please try again in X minutes."` (429)

**Verify OTP:**
- `"Invalid OTP code"` (400)
- `"Invalid or expired OTP"` (400)
- `"Password reset function is temporarily locked due to too many failed attempts. Please try again in X minutes."` (429)
- `"Too many failed attempts. Password reset function is temporarily locked for 30 minutes."` (429)

**Reset Password:**
- `"Password and confirmation do not match"` (400)
- `"Invalid or expired OTP. Please verify OTP first."` (400)
- `"User not found"` (404)

---

## 💻 Frontend Implementation Guide

### 1. API Client Setup

```javascript
// Example với axios
import axios from 'axios';

// Detect environment: local development or production
const isDevelopment = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
const API_BASE_URL = isDevelopment 
  ? 'http://localhost:8080'  // Local backend
  : 'https://api.hokori-backend.org';  // Production backend (Railway)

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
});
```

**Hoặc sử dụng environment variable:**

```javascript
// .env.local (cho local development)
// NEXT_PUBLIC_API_URL=http://localhost:8080

// .env.production (cho production)
// NEXT_PUBLIC_API_URL=https://api.hokori-backend.org

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'https://api.hokori-backend.org';
```

### 2. Request OTP Function

```javascript
async function requestOtp(email) {
  try {
    const response = await apiClient.post('/api/auth/forgot-password/request-otp', {
      emailOrPhone: email.trim()
    });
    
    if (response.data.success) {
      return {
        success: true,
        message: response.data.data.message
      };
    }
  } catch (error) {
    return handleError(error);
  }
}
```

### 3. Verify OTP Function

```javascript
async function verifyOtp(email, otpCode) {
  try {
    const response = await apiClient.post('/api/auth/forgot-password/verify-otp', {
      emailOrPhone: email.trim(),
      otpCode: otpCode.trim()
    });
    
    if (response.data.success) {
      return {
        success: true,
        message: response.data.data.message
      };
    }
  } catch (error) {
    // Check if error response contains failed attempts info
    if (error.response && error.response.data && error.response.data.data) {
      const errorData = error.response.data.data;
      if (errorData.failedAttempts !== undefined) {
        return {
          success: false,
          error: 'INVALID_OTP',
          message: error.response.data.message || 'Invalid OTP code',
          failedAttempts: errorData.failedAttempts,
          remainingAttempts: errorData.remainingAttempts,
          maxAttempts: errorData.maxAttempts
        };
      }
    }
    return handleError(error);
  }
}
```

### 4. Reset Password Function

```javascript
async function resetPassword(email, otpCode, newPassword, confirmPassword) {
  try {
    const response = await apiClient.post('/api/auth/forgot-password/reset', {
      emailOrPhone: email.trim(),
      otpCode: otpCode.trim(),
      newPassword: newPassword,
      confirmPassword: confirmPassword
    });
    
    if (response.data.success) {
      return {
        success: true,
        message: response.data.data.message
      };
    }
  } catch (error) {
    return handleError(error);
  }
}
```

### 5. Error Handler

```javascript
function handleError(error) {
  if (error.response) {
    const status = error.response.status;
    const message = error.response.data.message || error.response.data.error || 'An error occurred';
    
    // Handle lockout (429)
    if (status === 429) {
      const minutes = parseLockoutMinutes(message);
      return {
        success: false,
        error: 'LOCKOUT',
        message: message,
        minutes: minutes
      };
    }
    
    // Handle other errors
    return {
      success: false,
      error: 'API_ERROR',
      message: message,
      status: status
    };
  } else if (error.request) {
    // Network error
    return {
      success: false,
      error: 'NETWORK_ERROR',
      message: 'Network error. Please check your connection and try again.'
    };
  } else {
    // Other error
    return {
      success: false,
      error: 'UNKNOWN_ERROR',
      message: error.message || 'An unexpected error occurred'
    };
  }
}
```

### 6. Parse Lockout Minutes (Helper)

```javascript
function parseLockoutMinutes(message) {
  // Parse từ message: "Please try again in 30 minutes"
  const match = message.match(/try again in (\d+) minutes?/i);
  return match ? parseInt(match[1]) : null;
}
```

---

## 🎯 User Experience Flow

### Step 1: Request OTP

**User Actions:**
1. Nhập email vào input field
2. Click "Send OTP" button
3. Hiển thị loading state
4. Hiển thị success message: "OTP has been sent to your email"
5. Enable OTP input field và "Verify OTP" button

**Error Handling:**
- Invalid email format → Show validation error
- Account deactivated → Show error với link contact support
- Lockout active → Show error với countdown timer, disable form

---

### Step 2: Verify OTP

**User Actions:**
1. Nhập OTP code (6 chữ số)
2. Click "Verify OTP" button
3. Hiển thị loading state
4. Nếu thành công → Enable "Reset Password" form
5. Nếu sai → Show error, increment failed attempts counter

**Failed Attempts Tracking:**
- API trả về thông tin trong `data` object:
  - `failedAttempts`: Số lần đã nhập sai (1-5)
  - `remainingAttempts`: Số lần còn lại (4-0)
  - `maxAttempts`: Tổng số lần tối đa (5)
- Hiển thị: "X attempts remaining" sử dụng `remainingAttempts` từ response
- Khi đạt 5 lần sai → Lockout triggered → Show lockout message (429 error)

**Error Handling:**
- Invalid OTP → Show error, allow retry
- Expired OTP → Show error, suggest request new OTP
- Lockout active → Show error với countdown timer, disable form

---

### Step 3: Reset Password

**User Actions:**
1. Nhập new password
2. Nhập confirm password
3. Click "Reset Password" button
4. Hiển thị loading state
5. Nếu thành công → Show success message → Redirect to login page

**Validation:**
- Password minimum 6 characters
- Password và confirm password must match
- Validate trước khi submit (client-side)

**Error Handling:**
- Password mismatch → Show validation error
- OTP not verified → Show error, redirect to verify step
- User not found → Show error (rare case)

---

## 🔐 Security Considerations

### Frontend Best Practices

1. **Never store OTP in localStorage/sessionStorage**
   - OTP chỉ tồn tại trong memory
   - Clear OTP sau khi verify thành công

2. **Validate inputs client-side**
   - Email format validation
   - OTP format validation (6 digits)
   - Password strength validation
   - Password match validation

3. **Handle errors gracefully**
   - Không expose sensitive information
   - Show user-friendly error messages
   - Log errors for debugging (không log sensitive data)

4. **Rate limiting awareness**
   - Track failed attempts trên client (optional)
   - Disable form khi bị lockout
   - Show countdown timer khi lockout

5. **Network error handling**
   - Retry logic cho network errors
   - Timeout handling
   - Offline detection

---

## 📊 State Management

### Recommended State Variables

```javascript
// Forgot Password State
{
  // Step tracking
  currentStep: 'request' | 'verify' | 'reset', // Current step in flow
  
  // User input
  email: string,
  otpCode: string,
  newPassword: string,
  confirmPassword: string,
  
  // UI state
  isLoading: boolean,
  error: string | null,
  success: string | null,
  
  // OTP state
  otpSent: boolean,
  otpVerified: boolean,
  
  // Lockout state
  isLockedOut: boolean,
  lockoutMinutes: number | null,
  lockoutMessage: string | null,
  
  // Failed attempts tracking (optional)
  failedAttempts: number,
  remainingAttempts: number
}
```

### State Transitions

```
Initial State
    ↓
[Request OTP] → otpSent = true
    ↓
[Verify OTP] → otpVerified = true
    ↓
[Reset Password] → Success → Redirect to Login
```

---

## 🧪 Testing Checklist

### Happy Path
- [ ] Request OTP với email hợp lệ → Success
- [ ] Verify OTP với code đúng → Success
- [ ] Reset password với password hợp lệ → Success
- [ ] Redirect to login sau khi reset thành công

### Error Cases
- [ ] Request OTP với email không hợp lệ → Show validation error
- [ ] Verify OTP với code sai (1-4 lần) → Show error, allow retry
- [ ] Verify OTP với code sai 5 lần → Lockout triggered
- [ ] Request OTP khi bị lockout → Show lockout message
- [ ] Verify OTP khi bị lockout → Show lockout message
- [ ] Reset password với password không match → Show validation error
- [ ] Reset password với OTP chưa verify → Show error

### Edge Cases
- [ ] Request OTP với email không tồn tại → Still success (security)
- [ ] Verify OTP với OTP đã hết hạn → Show expired error
- [ ] Verify OTP với OTP đã được sử dụng → Show invalid error
- [ ] Reset password với OTP đã verify nhưng đã hết hạn → Should work (BE handles this)

### Network Errors
- [ ] Network timeout → Show retry option
- [ ] Server error (500) → Show generic error message
- [ ] No internet connection → Show offline message

---

## 📝 Field Validation Rules

### Email
- **Required**: Yes
- **Format**: Must contain "@"
- **Example**: `user@example.com`
- **Client-side validation**: Regex `/^[^\s@]+@[^\s@]+\.[^\s@]+$/`

### OTP Code
- **Required**: Yes
- **Format**: Exactly 6 digits (0-9)
- **Pattern**: `^[0-9]{6}$`
- **Example**: `123456`
- **Client-side validation**: Regex `/^[0-9]{6}$/`

### New Password
- **Required**: Yes
- **Min Length**: 6 characters
- **Client-side validation**: `password.length >= 6`

### Confirm Password
- **Required**: Yes
- **Must Match**: `newPassword === confirmPassword`
- **Client-side validation**: Check match before submit

---

## 🔄 Error Response Format

Tất cả error responses đều follow format:

```json
{
  "success": false,
  "message": "Error message here",
  "data": null,
  "meta": {},
  "timestamp": "2025-12-20T13:39:37.512052204"
}
```

**Frontend nên:**
- Check `success === false` để detect error
- Display `message` field cho user
- Handle `status` code để determine error type

---

## 📱 Mobile Considerations

### OTP Input
- Use numeric keyboard cho OTP input
- Auto-focus next input khi nhập số
- Auto-submit khi đủ 6 chữ số (optional)

### Password Input
- Show/hide password toggle
- Password strength indicator (optional)
- Auto-focus confirm password sau khi nhập password

### Lockout Display
- Show countdown timer prominently
- Disable all inputs khi lockout
- Show clear message về thời gian còn lại

---

## 🎨 UI/UX Recommendations

### Loading States
- Show spinner/loading indicator khi API đang call
- Disable buttons khi loading
- Prevent multiple submissions

### Success States
- Show success message với icon
- Auto-redirect sau 2-3 giây (optional)
- Clear form sau khi success

### Error States
- Show error message với icon
- Highlight invalid fields
- Allow retry (trừ khi lockout)

### Lockout State
- Show prominent warning message
- Display countdown timer
- Disable all form inputs
- Show "Try again in X minutes" message

---

## 📚 Related Documentation

- Backend API Documentation: `FORGOT_PASSWORD_DOCUMENTATION.md`
- API Base URL: Check environment configuration
- Error Codes Reference: See "Error Handling Guide" section above

---

**Last Updated:** 2025-12-20  
**Version:** 1.0

