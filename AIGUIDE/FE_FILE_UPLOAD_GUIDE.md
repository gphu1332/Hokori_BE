# 📁 Hướng Dẫn Upload File, Video và Hình Ảnh - Frontend Guide

## 📋 Mục Lục
1. [Tổng Quan](#tổng-quan)
2. [Cách Call API Đúng](#cách-call-api-đúng) ⭐ **QUAN TRỌNG**
3. [Upload File/Video cho Section](#upload-filevideo-cho-section)
4. [Upload Cover Image cho Course](#upload-cover-image-cho-course)
5. [Upload Avatar cho User](#upload-avatar-cho-user)
6. [Serve File từ Database](#serve-file-từ-database)
7. [Giới Hạn và Quy Tắc](#giới-hạn-và-quy-tắc)
8. [Error Handling](#error-handling)
9. [Ví Dụ Code](#ví-dụ-code)

---

## 🎯 Tổng Quan

### Cách Hoạt Động
- **Backend lưu file vào PostgreSQL database** (không phải filesystem)
- File được lưu dưới dạng binary data (BYTEA) trong database
- File được serve qua endpoint `/files/{filePath}`
- Tất cả file đều được public access (không cần authentication để xem)

### Flow Upload File
```
1. FE upload file → Backend lưu vào DB
2. Backend trả về filePath và URL
3. FE dùng filePath để tạo Content (nếu là course content)
4. FE hiển thị file qua URL trả về
```

---

## ⭐ Cách Call API Đúng

### 🔴 QUAN TRỌNG: Các Lỗi Thường Gặp

#### ❌ Lỗi 1: Gọi `/files` mà không có path
```typescript
// ❌ SAI - Request sẽ pending hoặc 400 Bad Request
fetch('/files')
fetch('/files/')

// ✅ ĐÚNG - Phải có filePath sau /files/
fetch('/files/sections/123/uuid-abc-123.mp4')
```

#### ❌ Lỗi 2: Thiếu `/files/` prefix khi hiển thị
```typescript
// ❌ SAI - 404 Not Found
<img src="/sections/123/uuid.mp4" />

// ✅ ĐÚNG - Phải có /files/ prefix
<img src="/files/sections/123/uuid.mp4" />
```

#### ❌ Lỗi 3: Set Content-Type header khi upload
```typescript
// ❌ SAI - Browser sẽ không set boundary đúng
headers: {
  'Content-Type': 'multipart/form-data', // ← KHÔNG được set
}

// ✅ ĐÚNG - Để browser tự động set
headers: {
  'Authorization': `Bearer ${token}`,
  // KHÔNG có Content-Type
}
```

### ✅ Flow Đúng Khi Upload và Hiển Thị File

```
Bước 1: Upload file
POST /api/teacher/courses/sections/{sectionId}/files
→ Response: { filePath: "sections/123/uuid.mp4", url: "/files/sections/123/uuid.mp4" }

Bước 2: Tạo Content (nếu cần)
POST /api/teacher/courses/sections/{sectionId}/contents
Body: { filePath: "sections/123/uuid.mp4" }  // ← Dùng filePath từ bước 1

Bước 3: Hiển thị file
GET /files/sections/123/uuid.mp4  // ← Dùng filePath hoặc url từ bước 1
→ Hiển thị: <img src="${API_BASE_URL}/files/${filePath}" />
```

### 📝 Checklist Trước Khi Call API

- [ ] **Upload**: Dùng `FormData` với field name = `"file"`
- [ ] **Upload**: KHÔNG set `Content-Type` header (browser tự set)
- [ ] **Upload**: Có `Authorization: Bearer {token}` header
- [ ] **Tạo Content**: Dùng `filePath` từ upload response (không phải `url`)
- [ ] **Hiển thị**: URL phải có format: `${API_BASE_URL}/files/${filePath}`
- [ ] **Hiển thị**: KHÔNG gọi `/files` mà không có path

---

## 📤 Upload File/Video cho Section

### Endpoint
```
POST /api/teacher/courses/sections/{sectionId}/files
Content-Type: multipart/form-data
Authorization: Bearer {token}
```

### Request
- **Method**: `POST`
- **Content-Type**: `multipart/form-data`
- **Body**: Form data với field name `file`
- **Path Parameter**: `sectionId` (Long)

### Response
```json
{
  "filePath": "sections/123/uuid-abc-123.mp4",
  "url": "/files/sections/123/uuid-abc-123.mp4"
}
```

### Ví Dụ Code (React/TypeScript)

```typescript
// Upload file cho section
async function uploadSectionFile(
  sectionId: number,
  file: File
): Promise<{ filePath: string; url: string }> {
  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch(
    `${API_BASE_URL}/api/teacher/courses/sections/${sectionId}/files`,
    {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${getToken()}`,
        // KHÔNG set Content-Type header, browser sẽ tự động set với boundary
      },
      body: formData,
    }
  );

  if (!response.ok) {
    throw new Error(`Upload failed: ${response.statusText}`);
  }

  return await response.json();
}

// Sử dụng
const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
  const file = event.target.files?.[0];
  if (!file) return;

  try {
    // 1. Upload file
    const { filePath, url } = await uploadSectionFile(sectionId, file);
    
    // 2. Tạo Content với filePath
    await createContent({
      sectionId,
      contentFormat: 'ASSET',
      filePath: filePath, // Dùng filePath từ upload response
      primaryContent: true, // true nếu là video chính
      orderIndex: 0
    });

    console.log('File uploaded successfully:', url);
  } catch (error) {
    console.error('Upload error:', error);
  }
};
```

### Sau Khi Upload
Sau khi upload file thành công, bạn cần tạo Content để gắn file vào Section:

```typescript
// Tạo Content với filePath từ upload
POST /api/teacher/courses/sections/{sectionId}/contents
{
  "contentFormat": "ASSET",
  "filePath": "sections/123/uuid-abc-123.mp4", // từ upload response
  "primaryContent": true, // true cho video chính (GRAMMAR)
  "orderIndex": 0
}
```

---

## 🖼️ Upload Cover Image cho Course

### Endpoint
```
POST /api/teacher/courses/{courseId}/cover-image
Content-Type: multipart/form-data
Authorization: Bearer {token}
```

### Request
- **Method**: `POST`
- **Content-Type**: `multipart/form-data`
- **Body**: Form data với field name `file`
- **Path Parameter**: `courseId` (Long)

### Response
```json
{
  "id": 1,
  "title": "Khóa học tiếng Nhật N5",
  "coverImagePath": "courses/1/cover/uuid-xyz-789.jpg",
  // ... other course fields
}
```

### Ví Dụ Code

```typescript
async function uploadCourseCoverImage(
  courseId: number,
  file: File
): Promise<CourseRes> {
  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch(
    `${API_BASE_URL}/api/teacher/courses/${courseId}/cover-image`,
    {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${getToken()}`,
      },
      body: formData,
    }
  );

  if (!response.ok) {
    throw new Error(`Upload failed: ${response.statusText}`);
  }

  return await response.json();
}

// Sử dụng
const handleCoverImageUpload = async (file: File) => {
  try {
    const course = await uploadCourseCoverImage(courseId, file);
    console.log('Cover image uploaded:', course.coverImagePath);
    // Hiển thị ảnh: /files/${course.coverImagePath}
  } catch (error) {
    console.error('Upload error:', error);
  }
};
```

### Hiển Thị Cover Image
```typescript
// URL để hiển thị cover image
const coverImageUrl = `${API_BASE_URL}/files/${course.coverImagePath}`;

// Trong JSX
<img src={coverImageUrl} alt="Course cover" />
```

---

## 👤 Upload Avatar cho User

### Endpoint
```
POST /api/profile/me/avatar
Content-Type: multipart/form-data
Authorization: Bearer {token}
```

### Request
- **Method**: `POST`
- **Content-Type**: `multipart/form-data`
- **Body**: Form data với field name `file`
- **Authentication**: Required (any authenticated user)

### Response
```json
{
  "avatarUrl": "/files/avatars/2/uuid-avatar-123.jpg"
}
```

### Ví Dụ Code

```typescript
async function uploadAvatar(file: File): Promise<string> {
  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch(
    `${API_BASE_URL}/api/profile/me/avatar`,
    {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${getToken()}`,
      },
      body: formData,
    }
  );

  if (!response.ok) {
    throw new Error(`Upload failed: ${response.statusText}`);
  }

  const data = await response.json();
  return data.avatarUrl; // "/files/avatars/2/uuid-avatar-123.jpg"
}

// Sử dụng
const handleAvatarUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
  const file = event.target.files?.[0];
  if (!file) return;

  // Validate file type
  if (!file.type.startsWith('image/')) {
    alert('Chỉ chấp nhận file ảnh');
    return;
  }

  // Validate file size (ví dụ: max 5MB)
  if (file.size > 5 * 1024 * 1024) {
    alert('File quá lớn. Tối đa 5MB');
    return;
  }

  try {
    const avatarUrl = await uploadAvatar(file);
    console.log('Avatar uploaded:', avatarUrl);
    // Update UI với avatarUrl
  } catch (error) {
    console.error('Upload error:', error);
  }
};
```

### Hiển Thị Avatar
```typescript
// URL để hiển thị avatar
const avatarUrl = user.avatarUrl 
  ? `${API_BASE_URL}${user.avatarUrl}` // "/files/avatars/2/uuid.jpg"
  : '/default-avatar.png';

// Trong JSX
<img src={avatarUrl} alt="Avatar" className="avatar" />
```

---

## 📥 Serve File từ Database

### Endpoint
```
GET /files/{filePath}
```

### ⚠️ QUAN TRỌNG: Phải có filePath sau /files/

```typescript
// ❌ SAI - Request sẽ pending hoặc 400 Bad Request
GET /files
GET /files/

// ✅ ĐÚNG - Phải có filePath
GET /files/sections/123/uuid-abc-123.mp4
GET /files/courses/1/cover/uuid-xyz-789.jpg
GET /files/avatars/2/uuid-avatar-123.jpg
```

### Cách Sử Dụng

```typescript
// 1. Từ upload response - Dùng filePath hoặc url
const { filePath, url } = await uploadSectionFile(sectionId, file);
// filePath = "sections/123/uuid-abc-123.mp4"
// url = "/files/sections/123/uuid-abc-123.mp4"

// Cách 1: Dùng filePath
const videoUrl = `${API_BASE_URL}/files/${filePath}`;

// Cách 2: Dùng url (nếu url đã có /files/ prefix)
const videoUrl = url.startsWith('/files/') 
  ? `${API_BASE_URL}${url}`
  : `${API_BASE_URL}/files/${filePath}`;

// 2. Từ course data
const coverImageUrl = `${API_BASE_URL}/files/${course.coverImagePath}`;

// 3. Từ user data
const avatarUrl = user.avatarUrl 
  ? user.avatarUrl.startsWith('/files/')
    ? `${API_BASE_URL}${user.avatarUrl}`  // Đã có /files/ prefix
    : `${API_BASE_URL}/files/${user.avatarUrl}`  // Chỉ có filePath
  : '/default-avatar.png';

// 4. Từ content data
const contentUrl = `${API_BASE_URL}/files/${content.filePath}`;

// 5. Hiển thị trong JSX
<video src={videoUrl} controls />
<img src={coverImageUrl} alt="Cover" />
<img src={avatarUrl} alt="Avatar" />
<img src={contentUrl} alt="Content" />
```

### Lưu Ý
- **Public Access**: Không cần authentication để xem file
- **CORS**: Backend đã config CORS, không cần lo lắng
- **Content-Type**: Backend tự động set Content-Type dựa trên file type
- **Cache**: Backend set cache headers (1 hour), browser sẽ cache file
- **Error Handling**: Nếu file không tồn tại → 404 Not Found

---

## ⚠️ Giới Hạn và Quy Tắc

### File Size Limits
- **Max file size**: `512MB` (backend config)
- **Max request size**: `512MB`
- **Khuyến nghị FE**: Validate file size trước khi upload
  - Video: Max 500MB
  - Image: Max 10MB
  - PDF/Document: Max 50MB

### File Types Được Hỗ Trợ

#### Video
- ✅ `.mp4` (recommended)
- ✅ `.webm`
- ✅ `.mov`
- ✅ `.avi`

#### Image
- ✅ `.jpg`, `.jpeg`
- ✅ `.png`
- ✅ `.gif`
- ✅ `.webp`

#### Document
- ✅ `.pdf`
- ✅ `.doc`, `.docx`
- ✅ `.txt`

### Content Format Rules

#### ASSET (File/Video)
- ✅ Cần `filePath` (từ upload response)
- ✅ Có thể là `primaryContent=true` (video chính)
- ✅ Dùng cho: Video, PDF, Image, Document

#### RICH_TEXT
- ✅ Cần `richText` (HTML/text)
- ❌ Không được `primaryContent=true` (phải là `false`)
- ✅ Dùng cho: Giải thích, hướng dẫn, notes

#### FLASHCARD_SET
- ✅ Chỉ dùng cho section VOCABULARY
- ✅ Cần `flashcardSetId`

---

## 🚨 Error Handling

### Common Errors

#### 1. File Too Large (413 Payload Too Large)
```typescript
if (file.size > 512 * 1024 * 1024) {
  alert('File quá lớn. Tối đa 512MB');
  return;
}
```

#### 2. Invalid File Type
```typescript
const allowedTypes = ['video/mp4', 'image/jpeg', 'image/png', 'application/pdf'];
if (!allowedTypes.includes(file.type)) {
  alert('File type không được hỗ trợ');
  return;
}
```

#### 3. Network Error
```typescript
try {
  const result = await uploadFile(file);
} catch (error) {
  if (error instanceof TypeError && error.message.includes('fetch')) {
    alert('Lỗi kết nối. Vui lòng thử lại.');
  } else {
    alert('Upload thất bại. Vui lòng thử lại.');
  }
}
```

#### 4. Unauthorized (401)
```typescript
if (response.status === 401) {
  // Token expired, redirect to login
  window.location.href = '/login';
}
```

### Error Response Format
```json
{
  "message": "File too large",
  "status": "error",
  "timestamp": "2025-01-20T10:30:00Z"
}
```

---

## 💻 Ví Dụ Code Hoàn Chỉnh

### React Component - File Upload với Progress

```typescript
import React, { useState } from 'react';

interface FileUploadProps {
  sectionId: number;
  onUploadSuccess: (filePath: string, url: string) => void;
}

const FileUpload: React.FC<FileUploadProps> = ({ sectionId, onUploadSuccess }) => {
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState<string | null>(null);

  const handleFileChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    // Validate file size
    const maxSize = 512 * 1024 * 1024; // 512MB
    if (file.size > maxSize) {
      setError('File quá lớn. Tối đa 512MB');
      return;
    }

    // Validate file type
    const allowedTypes = [
      'video/mp4',
      'video/webm',
      'image/jpeg',
      'image/png',
      'image/gif',
      'application/pdf'
    ];
    if (!allowedTypes.includes(file.type)) {
      setError('File type không được hỗ trợ');
      return;
    }

    setUploading(true);
    setError(null);
    setProgress(0);

    try {
      const formData = new FormData();
      formData.append('file', file);

      const xhr = new XMLHttpRequest();

      // Track upload progress
      xhr.upload.addEventListener('progress', (e) => {
        if (e.lengthComputable) {
          const percentComplete = (e.loaded / e.total) * 100;
          setProgress(percentComplete);
        }
      });

      // Handle response
      xhr.addEventListener('load', () => {
        if (xhr.status === 200) {
          const response = JSON.parse(xhr.responseText);
          onUploadSuccess(response.filePath, response.url);
          setProgress(100);
        } else {
          setError(`Upload failed: ${xhr.statusText}`);
        }
        setUploading(false);
      });

      // Handle error
      xhr.addEventListener('error', () => {
        setError('Network error. Please try again.');
        setUploading(false);
      });

      // Send request
      xhr.open('POST', `${API_BASE_URL}/api/teacher/courses/sections/${sectionId}/files`);
      xhr.setRequestHeader('Authorization', `Bearer ${getToken()}`);
      xhr.send(formData);

    } catch (error) {
      setError('Upload failed. Please try again.');
      setUploading(false);
    }
  };

  return (
    <div className="file-upload">
      <input
        type="file"
        onChange={handleFileChange}
        disabled={uploading}
        accept="video/*,image/*,.pdf"
      />
      
      {uploading && (
        <div className="progress-bar">
          <div 
            className="progress-fill" 
            style={{ width: `${progress}%` }}
          />
          <span>{Math.round(progress)}%</span>
        </div>
      )}
      
      {error && <div className="error">{error}</div>}
    </div>
  );
};

export default FileUpload;
```

### React Hook - useFileUpload

```typescript
import { useState } from 'react';

interface UploadResult {
  filePath: string;
  url: string;
}

export function useFileUpload() {
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const uploadSectionFile = async (
    sectionId: number,
    file: File
  ): Promise<UploadResult> => {
    setUploading(true);
    setError(null);

    try {
      const formData = new FormData();
      formData.append('file', file);

      const response = await fetch(
        `${API_BASE_URL}/api/teacher/courses/sections/${sectionId}/files`,
        {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${getToken()}`,
          },
          body: formData,
        }
      );

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `Upload failed: ${response.statusText}`);
      }

      const data = await response.json();
      return data;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Upload failed';
      setError(message);
      throw err;
    } finally {
      setUploading(false);
    }
  };

  const uploadCoverImage = async (
    courseId: number,
    file: File
  ): Promise<string> => {
    setUploading(true);
    setError(null);

    try {
      const formData = new FormData();
      formData.append('file', file);

      const response = await fetch(
        `${API_BASE_URL}/api/teacher/courses/${courseId}/cover-image`,
        {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${getToken()}`,
          },
          body: formData,
        }
      );

      if (!response.ok) {
        throw new Error(`Upload failed: ${response.statusText}`);
      }

      const course = await response.json();
      return course.coverImagePath;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Upload failed';
      setError(message);
      throw err;
    } finally {
      setUploading(false);
    }
  };

  const uploadAvatar = async (file: File): Promise<string> => {
    setUploading(true);
    setError(null);

    try {
      const formData = new FormData();
      formData.append('file', file);

      const response = await fetch(
        `${API_BASE_URL}/api/profile/me/avatar`,
        {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${getToken()}`,
          },
          body: formData,
        }
      );

      if (!response.ok) {
        throw new Error(`Upload failed: ${response.statusText}`);
      }

      const data = await response.json();
      return data.avatarUrl;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Upload failed';
      setError(message);
      throw err;
    } finally {
      setUploading(false);
    }
  };

  return {
    uploadSectionFile,
    uploadCoverImage,
    uploadAvatar,
    uploading,
    error,
  };
}
```

---

## 📝 Checklist cho FE Team

### Trước Khi Upload
- [ ] Validate file size (< 512MB)
- [ ] Validate file type (video/image/document)
- [ ] Check authentication token
- [ ] Show loading state

### Khi Upload
- [ ] Dùng `FormData` với field name = `"file"`
- [ ] **KHÔNG set `Content-Type` header** (browser tự động set với boundary)
- [ ] Có `Authorization: Bearer {token}` header
- [ ] Show progress bar (nếu file lớn)
- [ ] Disable upload button khi đang upload
- [ ] Handle network errors

### Sau Khi Upload
- [ ] Lưu `filePath` và `url` từ response
- [ ] **Dùng `filePath` để tạo Content** (không phải `url`)
- [ ] Tạo Content với `filePath` (nếu là course content)
- [ ] Hiển thị file preview với URL: `${API_BASE_URL}/files/${filePath}`
- [ ] Handle errors từ backend

### Hiển Thị File
- [ ] **URL format: `${API_BASE_URL}/files/${filePath}`** (phải có `/files/` prefix)
- [ ] **KHÔNG gọi `/files` mà không có path**
- [ ] Set đúng `alt` text cho images
- [ ] Add loading state cho video/images
- [ ] Handle 404 (file not found)
- [ ] Handle 400 (bad request - empty path)

### Debug Khi Request Pending
- [ ] Kiểm tra URL có đúng format không: `/files/{filePath}`
- [ ] Kiểm tra filePath có giá trị không (không null/undefined)
- [ ] Kiểm tra Network tab xem request có được gửi đi không
- [ ] Kiểm tra Console có error không
- [ ] Kiểm tra backend logs trên Railway

---

## 🔗 Related APIs

### Course Content APIs
- `POST /api/teacher/courses/sections/{sectionId}/contents` - Tạo content với filePath
- `GET /api/learner/lessons/{lessonId}/detail` - Lấy lesson với filePath

### File Serving
- `GET /files/**` - Serve file từ database (public)

---

## 📞 Support

Nếu có vấn đề hoặc câu hỏi, vui lòng liên hệ backend team hoặc tạo issue trên GitHub.

---

## 🐛 Troubleshooting

### Request `/files` Bị Pending

**Triệu chứng**: Request `/files` hiển thị `(pending)` trong Network tab, không có response.

**Nguyên nhân**:
1. FE đang gọi `/files` mà không có filePath
2. filePath bị null/undefined
3. Backend chưa deploy code mới

**Cách fix**:
```typescript
// ❌ SAI
const url = '/files';  // Thiếu filePath

// ✅ ĐÚNG
const url = `/files/${filePath}`;  // Phải có filePath
// Hoặc
const url = `${API_BASE_URL}/files/${filePath}`;
```

### File Không Hiển Thị (404 Not Found)

**Nguyên nhân**:
1. File chưa được upload vào database
2. filePath không khớp với database
3. File đã bị xóa (soft delete)

**Cách fix**:
1. Kiểm tra file đã upload thành công chưa
2. Kiểm tra `filePath` trong database có đúng không
3. Kiểm tra `deletedFlag` = false trong database

### Upload Thất Bại (400/500 Error)

**Nguyên nhân**:
1. File quá lớn (> 512MB)
2. File type không được hỗ trợ
3. Token hết hạn
4. Backend error (check logs)

**Cách fix**:
1. Validate file size trước khi upload
2. Validate file type trước khi upload
3. Refresh token nếu cần
4. Check backend logs trên Railway

---

**Last Updated**: 2025-01-22
**Version**: 1.1.0

