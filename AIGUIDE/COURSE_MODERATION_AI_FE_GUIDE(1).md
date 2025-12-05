# Hướng Dẫn Duyệt Khóa Học với AI Check - Frontend Guide

## 📋 Mục Lục
1. [Tổng Quan](#tổng-quan)
2. [Flow Tổng Quan](#flow-tổng-quan)
3. [API Endpoints](#api-endpoints)
4. [Request/Response Examples](#requestresponse-examples)
5. [Flow Chi Tiết](#flow-chi-tiết)
6. [UI/UX Recommendations](#uiux-recommendations)
7. [Error Handling](#error-handling)
8. [Code Examples](#code-examples)

---

## 🎯 Tổng Quan

Tính năng này cho phép **Moderator** duyệt các khóa học mà **Teacher** đã submit lên hệ thống.

### Quy Trình:
1. **Teacher** tạo/sửa khóa học → Click "Submit/Publish" → Khóa học chuyển sang trạng thái `PENDING_APPROVAL` (chờ duyệt)
2. **Moderator** vào màn hình duyệt → Thấy danh sách các khóa học đang chờ duyệt
3. **Moderator** xem chi tiết khóa học → Có thể bấm nút **"AI Check"** để nhận feedback từ AI
4. **Moderator** xem kết quả AI → Quyết định **Approve** (duyệt) hoặc **Reject** (từ chối)

### AI Check giúp:
- ✅ Kiểm tra nội dung an toàn (toxic content detection)
- ✅ Đánh giá độ phù hợp của nội dung
- ✅ Nhận recommendations và warnings từ AI
- ✅ Hỗ trợ Moderator đưa ra quyết định approve/reject

**Role Required:** `MODERATOR`

---

## 🔄 Flow Tổng Quan

```
┌─────────────────────────────────────────────────────────────┐
│ TEACHER SIDE                                                │
├─────────────────────────────────────────────────────────────┤
│ 1. Teacher tạo/sửa khóa học                                 │
│ 2. Teacher click "Submit/Publish"                           │
│ 3. Khóa học chuyển sang status: PENDING_APPROVAL           │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ MODERATOR SIDE                                              │
├─────────────────────────────────────────────────────────────┤
│ 4. Moderator vào màn hình duyệt khóa học                    │
│    GET /api/moderator/courses/pending                       │
│    → Hiển thị danh sách courses có status PENDING_APPROVAL │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. Moderator click vào 1 course để xem chi tiết             │
│    GET /api/moderator/courses/{id}/detail                   │
│    → Hiển thị full tree: Chapters → Lessons → Sections     │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. Moderator bấm nút "🤖 AI Check" (OPTIONAL)              │
│    GET /api/moderator/courses/{id}/ai-check                │
│    ⏳ Loading...                                            │
│    → AI trả về: Safety Check, Level Match, Recommendations │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 7. Moderator xem kết quả AI và quyết định                  │
│    ┌────────────────────────────────────────────┐           │
│    │ [✅ Approve]  [❌ Reject]  [👁️ Xem lại]   │           │
│    └────────────────────────────────────────────┘           │
└────────────────────┬────────────────────────────────────────┘
                     │
        ┌────────────┴────────────┐
        │                         │
        ▼                         ▼
┌──────────────────┐    ┌──────────────────┐
│ 8a. Approve      │    │ 8b. Reject       │
│ PUT /approve     │    │ PUT /reject      │
│ ✅ PUBLISHED     │    │ ❌ DRAFT         │
└──────────────────┘    └──────────────────┘
```

---

## 🔌 API Endpoints

### Base URL
```
/api/moderator/courses
```

**Authorization:** 
- Header: `Authorization: Bearer {token}`
- Role: `MODERATOR` (required)

---

### 1. Danh Sách Courses Đang Chờ Duyệt

**Endpoint:** `GET /api/moderator/courses/pending`

**Description:** Lấy danh sách tất cả courses có status `PENDING_APPROVAL` (các khóa học mà Teacher đã submit và đang chờ duyệt)

**Response:**
```json
{
  "status": "success",
  "message": "OK",
  "data": [
    {
      "id": 59,
      "title": "Khóa học tiếng Nhật N5",
      "status": "PENDING_APPROVAL",
      "userId": 5,
      "level": "N5",
      "priceCents": 500000,
      "currency": "VND"
    },
    {
      "id": 60,
      "title": "Khóa học Kanji N4",
      "status": "PENDING_APPROVAL",
      "userId": 6,
      "level": "N4",
      "priceCents": 300000,
      "currency": "VND"
    }
  ]
}
```

---

### 2. Chi Tiết Course (Full Tree)

**Endpoint:** `GET /api/moderator/courses/{id}/detail`

**Description:** Xem toàn bộ nội dung course (chapters → lessons → sections → contents) để review trước khi approve/reject. **Chỉ áp dụng cho courses có status `PENDING_APPROVAL`**

**Path Parameters:**
- `id` (Long, required): Course ID

**Response:**
```json
{
  "status": "success",
  "message": "OK",
  "data": {
    "id": 59,
    "title": "Khóa học tiếng Nhật N5",
    "subtitle": "Học từ cơ bản",
    "description": "Khóa học tiếng Nhật N5 từ cơ bản...",
    "level": "N5",
    "status": "PENDING_APPROVAL",
    "userId": 5,
    "priceCents": 500000,
    "currency": "VND",
    "chapters": [
      {
        "id": 1,
        "title": "Chapter 1: Giới thiệu",
        "summary": "Giới thiệu về tiếng Nhật",
        "lessons": [
          {
            "id": 1,
            "title": "Lesson 1: Bảng chữ cái",
            "sections": [
              {
                "id": 1,
                "title": "Hiragana",
                "studyType": "VIDEO",
                "contents": [
                  {
                    "id": 1,
                    "contentFormat": "VIDEO",
                    "filePath": "courses/59/sections/1/video.mp4"
                  }
                ]
              }
            ]
          }
        ]
      }
    ]
  }
}
```

**Error Codes:**
- `400 Bad Request`: Course không ở trạng thái `PENDING_APPROVAL`
- `404 Not Found`: Course không tồn tại

---

### 3. AI Check Course Content ⭐

**Endpoint:** `GET /api/moderator/courses/{id}/ai-check`

**Description:** Sử dụng AI để kiểm tra nội dung khóa học (safety check, level match). **Chỉ áp dụng cho courses có status `PENDING_APPROVAL`**. Moderator có thể bấm nút này để nhận feedback từ AI trước khi quyết định approve/reject.

**Path Parameters:**
- `id` (Long, required): Course ID

**Response Structure:**
```typescript
interface CourseAICheckResponse {
  courseId: number;
  courseTitle: string;
  checkedAt: string; // ISO 8601 timestamp
  
  safetyCheck: {
    status: "SAFE" | "WARNING" | "UNSAFE";
    score: number; // 0.0 - 1.0 (1.0 = hoàn toàn an toàn)
    hasIssues: boolean;
    summary: string;
  };
  
  levelMatch: {
    declaredLevel: string; // N5, N4, N3, etc.
    detectedLevel: string | null; // Chưa implement
    match: boolean | null; // Chưa implement
    confidence: number | null; // Chưa implement
    summary: string;
  };
  
  recommendations: string[]; // List các recommendations
  warnings: string[]; // List các warnings (nếu có)
}
```

**Response Codes:**
- `200 OK`: AI check thành công
- `400 Bad Request`: Course không ở trạng thái `PENDING_APPROVAL`
- `401 Unauthorized`: Chưa đăng nhập
- `403 Forbidden`: Không có quyền MODERATOR
- `404 Not Found`: Course không tồn tại
- `503 Service Unavailable`: AI service không khả dụng

**Note:** AI Check là **OPTIONAL** - Moderator có thể approve/reject mà không cần gọi AI Check.

---

### 4. Approve Course

**Endpoint:** `PUT /api/moderator/courses/{id}/approve`

**Description:** Duyệt và publish course. Chuyển status từ `PENDING_APPROVAL` sang `PUBLISHED`. Course sẽ được hiển thị công khai cho người dùng.

**Path Parameters:**
- `id` (Long, required): Course ID

**Response:**
```json
{
  "status": "success",
  "message": "Course approved",
  "data": {
    "id": 59,
    "title": "Khóa học tiếng Nhật N5",
    "status": "PUBLISHED"
  }
}
```

**Error Codes:**
- `400 Bad Request`: Course không ở trạng thái `PENDING_APPROVAL`
- `404 Not Found`: Course không tồn tại

---

### 5. Reject Course

**Endpoint:** `PUT /api/moderator/courses/{id}/reject?reason={reason}`

**Description:** Từ chối course. Chuyển status từ `PENDING_APPROVAL` về `DRAFT`. Teacher sẽ phải sửa lại và submit lại.

**Path Parameters:**
- `id` (Long, required): Course ID

**Query Parameters:**
- `reason` (String, optional): Lý do từ chối

**Response:**
```json
{
  "status": "success",
  "message": "Course rejected",
  "data": {
    "id": 59,
    "title": "Khóa học tiếng Nhật N5",
    "status": "DRAFT"
  }
}
```

**Error Codes:**
- `400 Bad Request`: Course không ở trạng thái `PENDING_APPROVAL`
- `404 Not Found`: Course không tồn tại

---

## 📝 Request/Response Examples

### Example 1: AI Check - Safe Content

**Request:**
```bash
GET /api/moderator/courses/59/ai-check
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response:**
```json
{
  "status": "success",
  "message": "AI check completed",
  "data": {
    "courseId": 59,
    "courseTitle": "Khóa học tiếng Nhật N5",
    "checkedAt": "2025-01-22T10:30:00.000Z",
    "safetyCheck": {
      "status": "SAFE",
      "score": 0.95,
      "hasIssues": false,
      "summary": "Nội dung an toàn, không có vấn đề"
    },
    "levelMatch": {
      "declaredLevel": "N5",
      "detectedLevel": null,
      "match": null,
      "confidence": null,
      "summary": "Level matching not implemented yet"
    },
    "recommendations": [
      "✓ Nội dung an toàn và phù hợp",
      "✓ Không có từ ngữ nhạy cảm"
    ],
    "warnings": []
  }
}
```

---

### Example 2: AI Check - Warning Content

**Response:**
```json
{
  "status": "success",
  "message": "AI check completed",
  "data": {
    "courseId": 59,
    "courseTitle": "Khóa học tiếng Nhật N5",
    "checkedAt": "2025-01-22T10:30:00.000Z",
    "safetyCheck": {
      "status": "WARNING",
      "score": 0.65,
      "hasIssues": true,
      "summary": "Nội dung có thể cần xem xét thêm"
    },
    "levelMatch": {
      "declaredLevel": "N5",
      "detectedLevel": null,
      "match": null,
      "confidence": null,
      "summary": "Level matching not implemented yet"
    },
    "recommendations": [
      "⚠ Nội dung có thể cần xem xét thêm",
      "⚠ Kiểm tra lại các từ ngữ có thể gây hiểu lầm"
    ],
    "warnings": [
      "Nội dung có thể chứa từ ngữ không phù hợp. Vui lòng xem xét kỹ."
    ]
  }
}
```

---

### Example 3: AI Check - Unsafe Content

**Response:**
```json
{
  "status": "success",
  "message": "AI check completed",
  "data": {
    "courseId": 59,
    "courseTitle": "Khóa học tiếng Nhật N5",
    "checkedAt": "2025-01-22T10:30:00.000Z",
    "safetyCheck": {
      "status": "UNSAFE",
      "score": 0.45,
      "hasIssues": true,
      "summary": "Nội dung có thể chứa từ ngữ không phù hợp"
    },
    "levelMatch": {
      "declaredLevel": "N5",
      "detectedLevel": null,
      "match": null,
      "confidence": null,
      "summary": "Level matching not implemented yet"
    },
    "recommendations": [
      "✗ Nội dung có thể chứa từ ngữ không phù hợp",
      "✗ Vui lòng review kỹ trước khi approve"
    ],
    "warnings": [
      "Nội dung có thể chứa từ ngữ không phù hợp. Vui lòng xem xét kỹ."
    ]
  }
}
```

---

## 🔄 Flow Chi Tiết

### Step 1: Teacher Submit Course
```
Teacher tạo/sửa course → Click "Submit/Publish"
→ Backend chuyển status thành PENDING_APPROVAL
→ Course xuất hiện trong danh sách chờ duyệt
```

### Step 2: Moderator Xem Danh Sách
```
Moderator vào màn hình duyệt
→ Gọi GET /api/moderator/courses/pending
→ Hiển thị danh sách courses có status PENDING_APPROVAL
→ Mỗi course hiển thị: title, level, teacher, thời gian submit
```

### Step 3: Moderator Xem Chi Tiết
```
Moderator click vào 1 course
→ Gọi GET /api/moderator/courses/{id}/detail
→ Hiển thị full tree: Chapters → Lessons → Sections → Contents
→ Moderator có thể scroll và xem toàn bộ nội dung
```

### Step 4: Moderator Bấm AI Check (Optional)
```
Moderator bấm nút "🤖 AI Check"
→ Gọi GET /api/moderator/courses/{id}/ai-check
→ Hiển thị loading spinner
→ AI phân tích nội dung (có thể mất vài giây)
→ Hiển thị kết quả:
   ✅ Safety Check: SAFE/WARNING/UNSAFE
   ✅ Level Match: (chưa implement)
   📋 Recommendations
   ⚠️ Warnings (nếu có)
```

### Step 5: Moderator Quyết Định
```
Dựa trên:
- Kết quả AI Check (nếu có)
- Review thủ công nội dung course

Moderator chọn:
→ [✅ Approve]: Gọi PUT /api/moderator/courses/{id}/approve
   → Status: PENDING_APPROVAL → PUBLISHED
   → Course được publish công khai

→ [❌ Reject]: Gọi PUT /api/moderator/courses/{id}/reject?reason=...
   → Status: PENDING_APPROVAL → DRAFT
   → Teacher phải sửa lại và submit lại
```

---

## 🎨 UI/UX Recommendations

### 1. Màn Hình Danh Sách Courses Đang Chờ Duyệt

```
┌─────────────────────────────────────────────────────────────┐
│ 📚 Courses Đang Chờ Duyệt                    [Refresh]      │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│ ┌────────────────────────────────────────────────────────┐ │
│ │ Khóa học tiếng Nhật N5                    [Xem chi tiết]│ │
│ │ 👤 Teacher: Nguyễn Văn A                                │ │
│ │ 📊 Level: N5  |  💰 500,000 VND                        │ │
│ │ ⏰ Submitted: 2 giờ trước                               │ │
│ │ 🏷️ Status: PENDING_APPROVAL                            │ │
│ └────────────────────────────────────────────────────────┘ │
│                                                              │
│ ┌────────────────────────────────────────────────────────┐ │
│ │ Khóa học Kanji N4                        [Xem chi tiết]│ │
│ │ 👤 Teacher: Trần Thị B                                 │ │
│ │ 📊 Level: N4  |  💰 300,000 VND                        │ │
│ │ ⏰ Submitted: 5 giờ trước                               │ │
│ │ 🏷️ Status: PENDING_APPROVAL                            │ │
│ └────────────────────────────────────────────────────────┘ │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 2. Màn Hình Chi Tiết Course

```
┌─────────────────────────────────────────────────────────────┐
│ ← Quay lại                                                   │
├─────────────────────────────────────────────────────────────┤
│ Khóa học tiếng Nhật N5                                      │
│ Level: N5  |  Teacher: Nguyễn Văn A                        │
│ Status: PENDING_APPROVAL                                    │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│ [🤖 AI Check] [✅ Approve] [❌ Reject]                      │
│                                                              │
│ 📑 Nội dung khóa học:                                       │
│                                                              │
│ Chapter 1: Giới thiệu                                       │
│   └─ Lesson 1: Bảng chữ cái                                │
│      └─ Section: Hiragana                                   │
│         └─ Content: Video (5:30)                           │
│                                                              │
│ Chapter 2: Ngữ pháp cơ bản                                  │
│   └─ Lesson 1: Động từ                                     │
│      └─ Section: Động từ nhóm 1                            │
│         └─ Content: Rich Text                              │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 3. Màn Hình AI Check Results

```
┌─────────────────────────────────────────────────────────────┐
│ 🤖 AI Check Results                                         │
│ Course: Khóa học tiếng Nhật N5                             │
│ Checked at: 2025-01-22 10:30                               │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│ ✅ Safety Check: SAFE                                       │
│    Score: 0.95 / 1.0                                        │
│    Summary: Nội dung an toàn, không có vấn đề              │
│                                                              │
│ ✅ Level Match: N5                                          │
│    Declared: N5                                             │
│    Summary: Level matching not implemented yet              │
│                                                              │
│ 📋 Recommendations:                                         │
│    ✓ Nội dung an toàn và phù hợp                           │
│    ✓ Không có từ ngữ nhạy cảm                              │
│                                                              │
│ ⚠️ Warnings:                                                │
│    (Không có)                                               │
│                                                              │
│ [Close] [✅ Approve] [❌ Reject]                           │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 4. Color Coding cho Safety Status

- **SAFE** (score ≥ 0.8): 🟢 Green (`#10b981`)
- **WARNING** (0.6 ≤ score < 0.8): 🟡 Yellow (`#f59e0b`)
- **UNSAFE** (score < 0.6): 🔴 Red (`#ef4444`)

### 5. Loading State cho AI Check

```
┌─────────────────────────────────────────────────────────────┐
│ 🤖 AI đang phân tích nội dung...                            │
│ ⏳ Vui lòng đợi trong giây lát                              │
│                                                              │
│ [Loading spinner]                                           │
│                                                              │
│ Đang kiểm tra:                                              │
│ • Safety check (toxic content)                              │
│ • Level matching                                            │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## ⚠️ Error Handling

### 1. AI Service Unavailable (503)

```typescript
if (error.status === 503) {
  // Hiển thị thông báo
  showError("AI service hiện không khả dụng. Vui lòng thử lại sau.");
  // Vẫn cho phép Mod approve/reject thủ công (không bắt buộc phải có AI)
}
```

### 2. Course Not Pending Approval (400)

```typescript
if (error.status === 400) {
  showError("Course không ở trạng thái chờ duyệt.");
  // Redirect về danh sách hoặc refresh danh sách
  loadPendingCourses();
}
```

### 3. Unauthorized/Forbidden (401/403)

```typescript
if (error.status === 401 || error.status === 403) {
  showError("Bạn không có quyền truy cập.");
  // Redirect về login hoặc dashboard
  redirectToLogin();
}
```

### 4. Course Not Found (404)

```typescript
if (error.status === 404) {
  showError("Không tìm thấy course.");
  // Redirect về danh sách
  navigateToPendingCourses();
}
```

---

## 💻 Code Examples

### TypeScript Types

```typescript
// types/courseModeration.ts

export interface CourseRes {
  id: number;
  title: string;
  subtitle?: string;
  description?: string;
  level: string;
  status: string;
  userId: number;
  priceCents?: number;
  currency?: string;
  chapters?: ChapterRes[];
}

export interface ChapterRes {
  id: number;
  title: string;
  summary?: string;
  lessons: LessonRes[];
}

export interface LessonRes {
  id: number;
  title: string;
  sections: SectionRes[];
}

export interface SectionRes {
  id: number;
  title: string;
  studyType: string;
  contents: ContentRes[];
}

export interface ContentRes {
  id: number;
  contentFormat: string;
  richText?: string;
  filePath?: string;
}

export interface CourseAICheckResponse {
  courseId: number;
  courseTitle: string;
  checkedAt: string;
  safetyCheck: SafetyCheck;
  levelMatch: LevelMatch;
  recommendations: string[];
  warnings: string[];
}

export interface SafetyCheck {
  status: "SAFE" | "WARNING" | "UNSAFE";
  score: number;
  hasIssues: boolean;
  summary: string;
}

export interface LevelMatch {
  declaredLevel: string;
  detectedLevel: string | null;
  match: boolean | null;
  confidence: number | null;
  summary: string;
}
```

---

### API Service

```typescript
// services/moderatorService.ts

import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080';

export const moderatorService = {
  // 1. List pending courses (courses mà Teacher đã submit)
  async listPendingCourses(): Promise<CourseRes[]> {
    const response = await axios.get(`${API_BASE_URL}/api/moderator/courses/pending`, {
      headers: {
        Authorization: `Bearer ${getToken()}`
      }
    });
    return response.data.data;
  },

  // 2. Get course detail (full tree)
  async getCourseDetail(courseId: number): Promise<CourseRes> {
    const response = await axios.get(
      `${API_BASE_URL}/api/moderator/courses/${courseId}/detail`,
      {
        headers: {
          Authorization: `Bearer ${getToken()}`
        }
      }
    );
    return response.data.data;
  },

  // 3. AI Check course (optional - Moderator có thể bấm để xem AI feedback)
  async aiCheckCourse(courseId: number): Promise<CourseAICheckResponse> {
    const response = await axios.get(
      `${API_BASE_URL}/api/moderator/courses/${courseId}/ai-check`,
      {
        headers: {
          Authorization: `Bearer ${getToken()}`
        }
      }
    );
    return response.data.data;
  },

  // 4. Approve course (duyệt và publish)
  async approveCourse(courseId: number): Promise<CourseRes> {
    const response = await axios.put(
      `${API_BASE_URL}/api/moderator/courses/${courseId}/approve`,
      {},
      {
        headers: {
          Authorization: `Bearer ${getToken()}`
        }
      }
    );
    return response.data.data;
  },

  // 5. Reject course (từ chối, chuyển về DRAFT)
  async rejectCourse(courseId: number, reason?: string): Promise<CourseRes> {
    const params = reason ? { reason } : {};
    const response = await axios.put(
      `${API_BASE_URL}/api/moderator/courses/${courseId}/reject`,
      {},
      {
        params,
        headers: {
          Authorization: `Bearer ${getToken()}`
        }
      }
    );
    return response.data.data;
  }
};

function getToken(): string {
  return localStorage.getItem('token') || '';
}
```

---

### React Component Example

```typescript
// components/CourseModerationPage.tsx

import React, { useState, useEffect } from 'react';
import { moderatorService } from '../services/moderatorService';
import { CourseRes, CourseAICheckResponse } from '../types/courseModeration';

const CourseModerationPage: React.FC = () => {
  const [courses, setCourses] = useState<CourseRes[]>([]);
  const [selectedCourse, setSelectedCourse] = useState<CourseRes | null>(null);
  const [aiCheckResult, setAiCheckResult] = useState<CourseAICheckResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [aiLoading, setAiLoading] = useState(false);

  useEffect(() => {
    loadPendingCourses();
  }, []);

  const loadPendingCourses = async () => {
    try {
      setLoading(true);
      const data = await moderatorService.listPendingCourses();
      setCourses(data);
    } catch (error) {
      console.error('Error loading courses:', error);
      alert('Không thể tải danh sách courses');
    } finally {
      setLoading(false);
    }
  };

  const handleViewDetail = async (courseId: number) => {
    try {
      setLoading(true);
      const course = await moderatorService.getCourseDetail(courseId);
      setSelectedCourse(course);
      setAiCheckResult(null); // Reset AI result khi xem course mới
    } catch (error: any) {
      console.error('Error loading course detail:', error);
      if (error.response?.status === 400) {
        alert('Course không ở trạng thái chờ duyệt');
        loadPendingCourses(); // Refresh danh sách
      } else {
        alert('Không thể tải chi tiết course');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleAICheck = async (courseId: number) => {
    try {
      setAiLoading(true);
      const result = await moderatorService.aiCheckCourse(courseId);
      setAiCheckResult(result);
    } catch (error: any) {
      console.error('Error checking AI:', error);
      if (error.response?.status === 503) {
        alert('AI service hiện không khả dụng. Vui lòng thử lại sau.');
      } else if (error.response?.status === 400) {
        alert('Course không ở trạng thái chờ duyệt');
      } else {
        alert('Không thể kiểm tra AI');
      }
    } finally {
      setAiLoading(false);
    }
  };

  const handleApprove = async (courseId: number) => {
    if (!confirm('Bạn có chắc chắn muốn approve course này?')) {
      return;
    }

    try {
      await moderatorService.approveCourse(courseId);
      alert('Course đã được approve thành công!');
      loadPendingCourses();
      setSelectedCourse(null);
      setAiCheckResult(null);
    } catch (error: any) {
      console.error('Error approving course:', error);
      if (error.response?.status === 400) {
        alert('Course không ở trạng thái chờ duyệt');
        loadPendingCourses();
      } else {
        alert('Không thể approve course');
      }
    }
  };

  const handleReject = async (courseId: number) => {
    const reason = prompt('Nhập lý do từ chối (optional):');
    
    try {
      await moderatorService.rejectCourse(courseId, reason || undefined);
      alert('Course đã được reject!');
      loadPendingCourses();
      setSelectedCourse(null);
      setAiCheckResult(null);
    } catch (error: any) {
      console.error('Error rejecting course:', error);
      if (error.response?.status === 400) {
        alert('Course không ở trạng thái chờ duyệt');
        loadPendingCourses();
      } else {
        alert('Không thể reject course');
      }
    }
  };

  const getSafetyStatusColor = (status: string) => {
    switch (status) {
      case 'SAFE':
        return '#10b981'; // green
      case 'WARNING':
        return '#f59e0b'; // yellow
      case 'UNSAFE':
        return '#ef4444'; // red
      default:
        return '#6b7280'; // gray
    }
  };

  return (
    <div className="course-moderation-page">
      <h1>📚 Duyệt Khóa Học</h1>

      {!selectedCourse ? (
        // List view - Danh sách courses đang chờ duyệt
        <div className="courses-list">
          <button onClick={loadPendingCourses}>🔄 Refresh</button>
          
          {loading ? (
            <div>Loading...</div>
          ) : courses.length === 0 ? (
            <div>Không có course nào đang chờ duyệt</div>
          ) : (
            courses.map((course) => (
              <div key={course.id} className="course-card">
                <h3>{course.title}</h3>
                <p>Level: {course.level}</p>
                <p>Teacher ID: {course.userId}</p>
                <p>Status: {course.status}</p>
                <button onClick={() => handleViewDetail(course.id)}>
                  Xem chi tiết
                </button>
              </div>
            ))
          )}
        </div>
      ) : (
        // Detail view - Chi tiết course và AI check
        <div className="course-detail">
          <button onClick={() => {
            setSelectedCourse(null);
            setAiCheckResult(null);
          }}>
            ← Quay lại
          </button>
          
          <h2>{selectedCourse.title}</h2>
          <p>Level: {selectedCourse.level}</p>
          <p>Status: {selectedCourse.status}</p>

          <div className="actions">
            <button 
              onClick={() => handleAICheck(selectedCourse.id)}
              disabled={aiLoading}
            >
              {aiLoading ? 'Đang kiểm tra...' : '🤖 AI Check'}
            </button>
            <button onClick={() => handleApprove(selectedCourse.id)}>
              ✅ Approve
            </button>
            <button onClick={() => handleReject(selectedCourse.id)}>
              ❌ Reject
            </button>
          </div>

          {/* AI Check Results */}
          {aiCheckResult && (
            <div className="ai-check-result">
              <h3>🤖 AI Check Results</h3>
              
              <div 
                className="safety-check"
                style={{ 
                  borderColor: getSafetyStatusColor(aiCheckResult.safetyCheck.status) 
                }}
              >
                <h4>
                  Safety Check: {aiCheckResult.safetyCheck.status}
                </h4>
                <p>Score: {aiCheckResult.safetyCheck.score.toFixed(2)} / 1.0</p>
                <p>{aiCheckResult.safetyCheck.summary}</p>
              </div>

              <div className="level-match">
                <h4>Level Match</h4>
                <p>Declared: {aiCheckResult.levelMatch.declaredLevel}</p>
                <p>{aiCheckResult.levelMatch.summary}</p>
              </div>

              {aiCheckResult.recommendations.length > 0 && (
                <div className="recommendations">
                  <h4>📋 Recommendations</h4>
                  <ul>
                    {aiCheckResult.recommendations.map((rec, idx) => (
                      <li key={idx}>{rec}</li>
                    ))}
                  </ul>
                </div>
              )}

              {aiCheckResult.warnings.length > 0 && (
                <div className="warnings">
                  <h4>⚠️ Warnings</h4>
                  <ul>
                    {aiCheckResult.warnings.map((warning, idx) => (
                      <li key={idx}>{warning}</li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          )}

          {/* Course tree display */}
          <div className="course-tree">
            <h3>Nội dung khóa học:</h3>
            {selectedCourse.chapters?.map((chapter) => (
              <div key={chapter.id} className="chapter">
                <h4>{chapter.title}</h4>
                {chapter.lessons.map((lesson) => (
                  <div key={lesson.id} className="lesson">
                    <h5>{lesson.title}</h5>
                    {lesson.sections.map((section) => (
                      <div key={section.id} className="section">
                        <h6>{section.title}</h6>
                        {/* Render contents */}
                      </div>
                    ))}
                  </div>
                ))}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default CourseModerationPage;
```

---

## ✅ Checklist Implementation

### Backend Integration
- [ ] Setup API base URL
- [ ] Setup authentication token storage
- [ ] Create API service functions
- [ ] Handle error responses

### UI Components
- [ ] Course list view (danh sách courses đang chờ duyệt)
- [ ] Course detail view (tree structure)
- [ ] AI check button
- [ ] AI results display component (với color coding)
- [ ] Approve/Reject buttons
- [ ] Loading states
- [ ] Error messages

### Features
- [ ] List pending courses (courses mà Teacher đã submit)
- [ ] View course detail (full tree)
- [ ] Call AI check API (optional)
- [ ] Display AI results với color coding
- [ ] Approve course (chuyển sang PUBLISHED)
- [ ] Reject course với reason (chuyển về DRAFT)
- [ ] Handle all error cases
- [ ] Refresh danh sách sau khi approve/reject

---

## 📌 Notes

1. **Flow chính xác:**
   - Teacher submit course → `PENDING_APPROVAL`
   - Moderator thấy danh sách courses đang chờ duyệt
   - Moderator có thể bấm "AI Check" để xem AI feedback (optional)
   - Moderator quyết định approve/reject

2. **AI Check là Optional**: Moderator có thể approve/reject mà không cần AI check

3. **Level Matching**: Hiện tại chưa implement, sẽ có trong tương lai

4. **Caching**: Có thể cache AI check results để tránh gọi lại nhiều lần

5. **Real-time Updates**: Có thể thêm WebSocket để update danh sách courses real-time khi có course mới được submit

---

## 🆘 Troubleshooting

### AI Check không hoạt động
- Kiểm tra Google Cloud AI đã được enable chưa
- Kiểm tra credentials đã được config đúng chưa
- Xem logs trên Railway để debug

### Course không hiển thị trong pending list
- Kiểm tra course status phải là `PENDING_APPROVAL`
- Kiểm tra Teacher đã submit course chưa (không phải chỉ save draft)
- Kiểm tra user có role `MODERATOR` không

### Approve/Reject không thành công
- Kiểm tra course status phải là `PENDING_APPROVAL`
- Kiểm tra authentication token còn valid không
- Refresh danh sách sau khi approve/reject để cập nhật status

---

**Last Updated:** 2025-01-22

