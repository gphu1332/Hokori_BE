# 📚 Frontend API Integration Guide - Hokori Web

## 🎯 Tổng Quan

Hướng dẫn chi tiết về cách tích hợp API Backend vào Frontend application.

**Base URL**: `https://YOUR_RAILWAY_URL.up.railway.app`  
**API Prefix**: `/api`  
**Response Format**: Tất cả responses đều theo format `ApiResponse<T>`

---

## 📋 Mục Lục

1. [Cấu Hình Cơ Bản](#cấu-hình-cơ-bản)
2. [Authentication](#authentication)
3. [Public Endpoints](#public-endpoints)
4. [User Profile](#user-profile)
5. [Courses](#courses)
6. [Cart](#cart)
7. [AI Services](#ai-services)
8. [Error Handling](#error-handling)

---

## 🔧 Cấu Hình Cơ Bản

### 1. Base URL Configuration

```javascript
// config/api.js
const API_BASE_URL = process.env.REACT_APP_API_URL || 'https://YOUR_RAILWAY_URL.up.railway.app';

export default API_BASE_URL;
```

### 2. API Client Setup (Axios)

```javascript
// utils/apiClient.js
import axios from 'axios';
import API_BASE_URL from '../config/api';

const apiClient = axios.create({
  baseURL: `${API_BASE_URL}/api`,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor - Thêm token vào mọi request
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - Xử lý lỗi chung
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token hết hạn hoặc không hợp lệ
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default apiClient;
```

### 3. Response Format

Tất cả API responses đều theo format:

```typescript
interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  meta?: Record<string, any>;
  timestamp: string;
}
```

**Ví dụ Response:**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "user": { ... },
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "..."
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

---

## 🔐 Authentication

### 1. Register Learner

**Endpoint**: `POST /api/auth/register/learner`

**Request Body:**
```json
{
  "username": "learner123",
  "email": "learner@example.com",
  "password": "password123",
  "displayName": "John Doe",
  "country": "Vietnam",
  "nativeLanguage": "vi",
  "currentJlptLevel": "N5"
}
```

**Response:**
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "user": { ... },
    "accessToken": "...",
    "refreshToken": "...",
    "message": "Registration successful",
    "role": "LEARNER"
  }
}
```

**Ví dụ Code:**
```javascript
import apiClient from '../utils/apiClient';

const registerLearner = async (userData) => {
  try {
    const response = await apiClient.post('/auth/register/learner', userData);
    const { accessToken, refreshToken, user } = response.data.data;
    
    // Lưu token
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    
    return { success: true, user };
  } catch (error) {
    return {
      success: false,
      error: error.response?.data?.message || 'Registration failed'
    };
  }
};
```

### 2. Register Teacher

**Endpoint**: `POST /api/auth/register/teacher`

**Request Body:**
```json
{
  "username": "teacher123",
  "email": "teacher@example.com",
  "password": "password123",
  "firstName": "Jane",
  "lastName": "Smith",
  "headline": "Japanese Language Teacher",
  "currentJlptLevel": "N1"
}
```

**Ví dụ Code:**
```javascript
const registerTeacher = async (teacherData) => {
  try {
    const response = await apiClient.post('/auth/register/teacher', teacherData);
    const { accessToken, refreshToken, user } = response.data.data;
    
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    
    return { success: true, user };
  } catch (error) {
    return {
      success: false,
      error: error.response?.data?.message || 'Registration failed'
    };
  }
};
```

### 3. Login

**Endpoint**: `POST /api/auth/login`

**Request Body:**
```json
{
  "usernameOrEmail": "user@example.com",
  "password": "password123"
}
```

**Ví dụ Code:**
```javascript
const login = async (credentials) => {
  try {
    const response = await apiClient.post('/auth/login', credentials);
    const { accessToken, refreshToken, user, roles } = response.data.data;
    
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    
    return { success: true, user, roles };
  } catch (error) {
    return {
      success: false,
      error: error.response?.data?.message || 'Login failed'
    };
  }
};
```

### 4. Firebase Authentication

**Endpoint**: `POST /api/auth/firebase`

**Request Body:**
```json
{
  "firebaseToken": "firebase_id_token_here"
}
```

**Ví dụ Code:**
```javascript
const firebaseAuth = async (firebaseToken) => {
  try {
    const response = await apiClient.post('/auth/firebase', {
      firebaseToken
    });
    const { accessToken, refreshToken, user } = response.data.data;
    
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    
    return { success: true, user };
  } catch (error) {
    return {
      success: false,
      error: error.response?.data?.message || 'Firebase authentication failed'
    };
  }
};
```

### 5. Get Available Roles

**Endpoint**: `GET /api/auth/roles`

**Ví dụ Code:**
```javascript
const getAvailableRoles = async () => {
  try {
    const response = await apiClient.get('/auth/roles');
    return response.data.data; // Array of roles
  } catch (error) {
    return [];
  }
};
```

---

## 👤 User Profile

### 1. Get Current User Profile

**Endpoint**: `GET /api/profile/me`  
**Authentication**: Required

**Ví dụ Code:**
```javascript
const getCurrentUser = async () => {
  try {
    const response = await apiClient.get('/profile/me');
    return response.data.data;
  } catch (error) {
    throw error;
  }
};
```

### 2. Update Current User Profile

**Endpoint**: `PUT /api/profile/me`  
**Authentication**: Required

**Request Body:**
```json
{
  "displayName": "New Name",
  "country": "Japan",
  "nativeLanguage": "vi",
  "currentJlptLevel": "N3"
}
```

**Ví dụ Code:**
```javascript
const updateProfile = async (profileData) => {
  try {
    const response = await apiClient.put('/profile/me', profileData);
    return response.data.data;
  } catch (error) {
    throw error;
  }
};
```

### 3. Change Password

**Endpoint**: `PUT /api/profile/me/password`  
**Authentication**: Required

**Request Body:**
```json
{
  "currentPassword": "oldpassword",
  "newPassword": "newpassword",
  "confirmPassword": "newpassword"
}
```

**Ví dụ Code:**
```javascript
const changePassword = async (passwordData) => {
  try {
    const response = await apiClient.put('/profile/me/password', passwordData);
    return { success: true, message: response.data.message };
  } catch (error) {
    return {
      success: false,
      error: error.response?.data?.message || 'Password change failed'
    };
  }
};
```

### 4. Get User by ID

**Endpoint**: `GET /api/profile/{id}`  
**Authentication**: Required

**Ví dụ Code:**
```javascript
const getUserById = async (userId) => {
  try {
    const response = await apiClient.get(`/profile/${userId}`);
    return response.data.data;
  } catch (error) {
    throw error;
  }
};
```

---

## 📚 Courses

### 1. Get Published Courses (Marketplace)

**Endpoint**: `GET /api/courses`  
**Authentication**: Not Required (Public)

**Query Parameters:**
- `level` (optional): JLPT level filter (N5, N4, N3, N2, N1)
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20)

**Ví dụ Code:**
```javascript
const getPublishedCourses = async (filters = {}) => {
  try {
    const params = new URLSearchParams({
      page: filters.page || 0,
      size: filters.size || 20,
    });
    
    if (filters.level) {
      params.append('level', filters.level);
    }
    
    const response = await apiClient.get(`/courses?${params}`);
    return response.data.data; // Page object with content array
  } catch (error) {
    throw error;
  }
};

// Sử dụng
const courses = await getPublishedCourses({
  level: 'N5',
  page: 0,
  size: 20
});
```

### 2. Get Course Tree (Detail)

**Endpoint**: `GET /api/courses/{id}/tree`  
**Authentication**: Not Required (Public, chỉ hiển thị PUBLISHED courses)

**Ví dụ Code:**
```javascript
const getCourseTree = async (courseId) => {
  try {
    const response = await apiClient.get(`/courses/${courseId}/tree`);
    return response.data.data; // CourseRes với full tree
  } catch (error) {
    if (error.response?.status === 404) {
      throw new Error('Course not found or not published');
    }
    throw error;
  }
};
```

---

## 🛒 Cart

### 1. View Cart

**Endpoint**: `GET /api/cart`  
**Authentication**: Required

**Response:**
```json
{
  "success": true,
  "data": {
    "cartId": 5,
    "items": [
      {
        "itemId": 12,
        "courseId": 101,
        "courseTitle": "Japanese N5 Course",
        "quantity": 1,
        "unitPrice": 1990000,
        "totalPrice": 1990000,
        "selected": true
      }
    ],
    "selectedSubtotal": 1990000
  }
}
```

**Ví dụ Code:**
```javascript
const getCart = async () => {
  try {
    const response = await apiClient.get('/cart');
    return response.data.data;
  } catch (error) {
    throw error;
  }
};
```

### 2. Add Item to Cart

**Endpoint**: `POST /api/cart/items`  
**Authentication**: Required

**Request Body:**
```json
{
  "courseId": 101,
  "quantity": 1
}
```

**Ví dụ Code:**
```javascript
const addToCart = async (courseId, quantity = 1) => {
  try {
    const response = await apiClient.post('/cart/items', {
      courseId,
      quantity
    });
    return response.data.data;
  } catch (error) {
    if (error.response?.status === 409) {
      throw new Error('Course already owned');
    }
    throw error;
  }
};
```

### 3. Update Cart Item

**Endpoint**: `PATCH /api/cart/items/{itemId}`  
**Authentication**: Required

**Request Body:**
```json
{
  "quantity": 3,
  "selected": true
}
```

**Ví dụ Code:**
```javascript
const updateCartItem = async (itemId, updates) => {
  try {
    const response = await apiClient.patch(`/cart/items/${itemId}`, updates);
    return response.data.data;
  } catch (error) {
    throw error;
  }
};
```

### 4. Remove Item from Cart

**Endpoint**: `DELETE /api/cart/items/{itemId}`  
**Authentication**: Required

**Ví dụ Code:**
```javascript
const removeFromCart = async (itemId) => {
  try {
    const response = await apiClient.delete(`/cart/items/${itemId}`);
    return response.data.data;
  } catch (error) {
    throw error;
  }
};
```

### 5. Select All Items

**Endpoint**: `PATCH /api/cart/items/select-all`  
**Authentication**: Required

**Request Body:**
```json
{
  "selected": true
}
```

**Ví dụ Code:**
```javascript
const selectAllCartItems = async (selected = true) => {
  try {
    const response = await apiClient.patch('/cart/items/select-all', { selected });
    return response.data.data;
  } catch (error) {
    throw error;
  }
};
```

### 6. Clear Cart

**Endpoint**: `DELETE /api/cart/items`  
**Authentication**: Required

**Ví dụ Code:**
```javascript
const clearCart = async () => {
  try {
    const response = await apiClient.delete('/cart/items');
    return response.data.data;
  } catch (error) {
    throw error;
  }
};
```

---

## 🤖 AI Services

Tất cả AI endpoints đều **PUBLIC** (không cần authentication).

### 1. Translate Text

**Endpoint**: `POST /api/ai/translate`

**Request Body:**
```json
{
  "text": "こんにちは",
  "sourceLanguage": "ja",
  "targetLanguage": "vi"
}
```

**Ví dụ Code:**
```javascript
const translateText = async (text, sourceLang, targetLang) => {
  try {
    const response = await apiClient.post('/ai/translate', {
      text,
      sourceLanguage: sourceLang,
      targetLanguage: targetLang
    });
    return response.data.data;
  } catch (error) {
    throw error;
  }
};
```

### 2. Analyze Sentiment

**Endpoint**: `POST /api/ai/analyze-sentiment`

**Request Body:**
```json
{
  "text": "I love learning Japanese!"
}
```

**Ví dụ Code:**
```javascript
const analyzeSentiment = async (text) => {
  try {
    const response = await apiClient.post('/ai/analyze-sentiment', { text });
    return response.data.data;
  } catch (error) {
    throw error;
  }
};
```

### 3. Speech to Text

**Endpoint**: `POST /api/ai/speech-to-text`

**Request Body:**
```json
{
  "audioBase64": "base64_encoded_audio_data",
  "languageCode": "ja-JP",
  "sampleRateHertz": 16000
}
```

**Ví dụ Code:**
```javascript
const speechToText = async (audioBase64, languageCode = 'ja-JP') => {
  try {
    const response = await apiClient.post('/ai/speech-to-text', {
      audioBase64,
      languageCode,
      sampleRateHertz: 16000
    });
    return response.data.data;
  } catch (error) {
    throw error;
  }
};
```

### 4. Text to Speech

**Endpoint**: `POST /api/ai/text-to-speech`

**Request Body:**
```json
{
  "text": "こんにちは",
  "languageCode": "ja-JP",
  "voiceName": "ja-JP-Standard-A",
  "speed": "normal"
}
```

**Ví dụ Code:**
```javascript
const textToSpeech = async (text, options = {}) => {
  try {
    const response = await apiClient.post('/ai/text-to-speech', {
      text,
      languageCode: options.languageCode || 'ja-JP',
      voiceName: options.voiceName || 'ja-JP-Standard-A',
      speed: options.speed || 'normal' // slow, normal, fast
    });
    return response.data.data; // Contains audioBase64
  } catch (error) {
    throw error;
  }
};
```

### 5. Sentence Analysis

**Endpoint**: `POST /api/ai/sentence-analysis`

**Request Body:**
```json
{
  "sentence": "私は日本語を勉強しています",
  "level": "N5"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "sentence": "私は日本語を勉強しています",
    "level": "N5",
    "vocabulary": [
      {
        "word": "私",
        "reading": "わたし",
        "meaning": "I, me",
        "jlptLevel": "N5"
      }
    ],
    "grammar": [
      {
        "pattern": "を + verb",
        "explanation": "Object particle",
        "jlptLevel": "N5"
      }
    ]
  }
}
```

**Ví dụ Code:**
```javascript
const analyzeSentence = async (sentence, level) => {
  try {
    const response = await apiClient.post('/ai/sentence-analysis', {
      sentence,
      level
    });
    return response.data.data;
  } catch (error) {
    throw error;
  }
};
```

### 6. Kaiwa Practice

**Endpoint**: `POST /api/ai/kaiwa-practice`

**Request Body:**
```json
{
  "sentence": "こんにちは",
  "userAudioBase64": "base64_audio",
  "level": "N5"
}
```

**Ví dụ Code:**
```javascript
const kaiwaPractice = async (sentence, audioBase64, level) => {
  try {
    const response = await apiClient.post('/ai/kaiwa-practice', {
      sentence,
      userAudioBase64: audioBase64,
      level
    });
    return response.data.data;
  } catch (error) {
    throw error;
  }
};
```

### 7. Get Kaiwa Recommendations

**Endpoint**: `GET /api/ai/kaiwa-recommendations/{level}`

**Ví dụ Code:**
```javascript
const getKaiwaRecommendations = async (level) => {
  try {
    const response = await apiClient.get(`/ai/kaiwa-recommendations/${level}`);
    return response.data.data;
  } catch (error) {
    throw error;
  }
};
```

### 8. Get Random Kaiwa Sentence

**Endpoint**: `GET /api/ai/kaiwa-sentences/{level}/random`

**Ví dụ Code:**
```javascript
const getRandomKaiwaSentence = async (level) => {
  try {
    const response = await apiClient.get(`/ai/kaiwa-sentences/${level}/random`);
    return response.data.data;
  } catch (error) {
    throw error;
  }
};
```

### 9. AI Health Check

**Endpoint**: `GET /api/ai/health`

**Ví dụ Code:**
```javascript
const checkAIHealth = async () => {
  try {
    const response = await apiClient.get('/ai/health');
    return response.data.data;
  } catch (error) {
    throw error;
  }
};
```

---

## 🏥 System Endpoints

### 1. Health Check

**Endpoint**: `GET /api/health`  
**Authentication**: Not Required

**Ví dụ Code:**
```javascript
const checkHealth = async () => {
  try {
    const response = await apiClient.get('/health');
    return response.data;
  } catch (error) {
    throw error;
  }
};
```

### 2. Hello Endpoint

**Endpoint**: `GET /api/hello?name=World`  
**Authentication**: Not Required

**Ví dụ Code:**
```javascript
const hello = async (name = 'World') => {
  try {
    const response = await apiClient.get(`/hello?name=${name}`);
    return response.data;
  } catch (error) {
    throw error;
  }
};
```

---

## ⚠️ Error Handling

### Standard Error Response Format

```json
{
  "success": false,
  "message": "Error message here",
  "data": null,
  "timestamp": "2024-01-15T10:30:00"
}
```

### HTTP Status Codes

- `200 OK`: Request thành công
- `201 Created`: Resource được tạo thành công
- `400 Bad Request`: Request không hợp lệ
- `401 Unauthorized`: Chưa đăng nhập hoặc token không hợp lệ
- `403 Forbidden`: Không có quyền truy cập
- `404 Not Found`: Resource không tồn tại
- `409 Conflict`: Conflict (ví dụ: email đã tồn tại)
- `500 Internal Server Error`: Lỗi server

### Error Handler Utility

```javascript
// utils/errorHandler.js
export const handleApiError = (error) => {
  if (error.response) {
    // Server responded with error status
    const { status, data } = error.response;
    
    switch (status) {
      case 400:
        return { message: data.message || 'Bad request', type: 'validation' };
      case 401:
        return { message: 'Unauthorized. Please login again.', type: 'auth' };
      case 403:
        return { message: 'You do not have permission to access this resource.', type: 'permission' };
      case 404:
        return { message: data.message || 'Resource not found.', type: 'notFound' };
      case 409:
        return { message: data.message || 'Conflict occurred.', type: 'conflict' };
      case 500:
        return { message: 'Server error. Please try again later.', type: 'server' };
      default:
        return { message: data.message || 'An error occurred.', type: 'unknown' };
    }
  } else if (error.request) {
    // Request was made but no response received
    return { message: 'Network error. Please check your connection.', type: 'network' };
  } else {
    // Something else happened
    return { message: error.message || 'An unexpected error occurred.', type: 'unknown' };
  }
};
```

### Usage Example

```javascript
import { handleApiError } from '../utils/errorHandler';

const fetchData = async () => {
  try {
    const response = await apiClient.get('/some-endpoint');
    return { success: true, data: response.data.data };
  } catch (error) {
    const errorInfo = handleApiError(error);
    console.error(errorInfo);
    return { success: false, error: errorInfo };
  }
};
```

---

## 📝 Best Practices

### 1. Token Management

```javascript
// utils/auth.js
export const getToken = () => {
  return localStorage.getItem('accessToken');
};

export const setTokens = (accessToken, refreshToken) => {
  localStorage.setItem('accessToken', accessToken);
  localStorage.setItem('refreshToken', refreshToken);
};

export const clearTokens = () => {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
};

export const isAuthenticated = () => {
  return !!getToken();
};
```

### 2. React Hook Example

```javascript
// hooks/useApi.js
import { useState, useEffect } from 'react';
import apiClient from '../utils/apiClient';

export const useApi = (endpoint, options = {}) => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const response = await apiClient.get(endpoint, options);
        setData(response.data.data);
        setError(null);
      } catch (err) {
        setError(err);
      } finally {
        setLoading(false);
      }
    };

    if (endpoint) {
      fetchData();
    }
  }, [endpoint]);

  return { data, loading, error };
};
```

### 3. React Query Example

```javascript
// hooks/useCourses.js
import { useQuery } from 'react-query';
import apiClient from '../utils/apiClient';

export const useCourses = (filters) => {
  return useQuery(
    ['courses', filters],
    async () => {
      const params = new URLSearchParams({
        page: filters?.page || 0,
        size: filters?.size || 20,
      });
      
      if (filters?.level) {
        params.append('level', filters.level);
      }
      
      const response = await apiClient.get(`/courses?${params}`);
      return response.data.data;
    },
    {
      staleTime: 5 * 60 * 1000, // 5 minutes
    }
  );
};
```

---

## 🔗 Swagger Documentation

Swagger UI có sẵn tại: `https://YOUR_RAILWAY_URL.up.railway.app/swagger-ui.html`

Bạn có thể:
- Xem tất cả endpoints
- Test API trực tiếp trên Swagger UI
- Xem request/response schemas
- Copy code examples

---

## 📞 Support

Nếu gặp vấn đề khi tích hợp API:
1. Kiểm tra Swagger UI để xem endpoint details
2. Kiểm tra Network tab trong DevTools
3. Kiểm tra response error messages
4. Liên hệ Backend team

---

**Tạo bởi**: Backend Team  
**Cập nhật**: 2024  
**Version**: 1.0.0

