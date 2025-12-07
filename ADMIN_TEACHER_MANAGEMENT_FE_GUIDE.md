# Hướng Dẫn Frontend: Admin - Quản Lý Teacher & Xem Doanh Thu

## 📋 Mục Lục
1. [Tổng Quan](#tổng-quan)
2. [API Endpoints](#api-endpoints)
3. [Request/Response Examples](#requestresponse-examples)
4. [TypeScript Interfaces](#typescript-interfaces)
5. [UI/UX Suggestions](#uiux-suggestions)
6. [Error Handling](#error-handling)

---

## 🎯 Tổng Quan

Admin có thể:
- ✅ Xem chi tiết teacher với các đóng góp (courses, enrollments)
- ✅ Xem tổng doanh thu của teacher (total và monthly)
- ✅ Xem tất cả payments từ learners (tổng giá tiền khi thanh toán thành công)
- ✅ List tất cả teachers với basic statistics

**Base URL:** `/api/admin`

**Authorization:** 
- ✅ **BẮT BUỘC**: Role `ADMIN`
- ✅ Header: `Authorization: Bearer {token}`

---

## 🔌 API Endpoints

### 1. Xem Chi Tiết Teacher ⭐

**Endpoint:** `GET /api/admin/teachers/{teacherId}`

**Description:** Xem chi tiết teacher bao gồm thông tin cá nhân, courses, statistics và revenue.

**Path Parameters:**
- `teacherId` (Long, required): ID của teacher

**Response Success (200):**
```json
{
  "success": true,
  "message": "Teacher details retrieved successfully",
  "data": {
    "teacher": {
      "id": 5,
      "email": "teacher@example.com",
      "username": "teacher123",
      "displayName": "Nguyễn Văn A",
      "bio": "Giáo viên tiếng Nhật với 10 năm kinh nghiệm...",
      "currentJlptLevel": "N1",
      "approvalStatus": "APPROVED",
      "walletBalance": 5000000,
      "createdAt": "2024-01-15T10:00:00"
    },
    "courses": [
      {
        "id": 1,
        "title": "Khóa học N5 cơ bản",
        "slug": "khoa-hoc-n5-co-ban",
        "status": "PUBLISHED",
        "priceCents": 500000,
        "discountedPriceCents": 400000,
        "publishedAt": "2024-02-01T08:00:00Z"
      }
    ],
    "statistics": {
      "totalCourses": 5,
      "publishedCourses": 3,
      "draftCourses": 1,
      "pendingCourses": 1,
      "totalEnrollments": 150
    },
    "revenue": {
      "totalRevenueCents": 15000000,
      "totalRevenue": 150000.00,
      "monthlyRevenueCents": 2000000,
      "monthlyRevenue": 20000.00,
      "currentMonth": "2024-12"
    }
  }
}
```

---

### 2. Xem Revenue của Teacher

**Endpoint:** `GET /api/admin/teachers/{teacherId}/revenue`

**Query Parameters:**
- `year` (Integer, optional): Năm (VD: 2024)
- `month` (Integer, optional): Tháng (1-12)
- Nếu không có → Mặc định tháng hiện tại

**Response Success (200):**
```json
{
  "success": true,
  "message": "Teacher revenue retrieved successfully",
  "data": {
    "teacherId": 5,
    "teacherName": "Nguyễn Văn A",
    "period": "2024-12",
    "revenueCents": 2000000,
    "revenue": 20000.00,
    "transactionCount": 15,
    "transactions": [
      {
        "id": 101,
        "amountCents": 400000,
        "amount": 4000.00,
        "courseId": 1,
        "courseTitle": "Khóa học N5 cơ bản",
        "description": "Revenue from course sale: Khóa học N5 cơ bản",
        "createdAt": "2024-12-05T10:30:00Z"
      }
    ],
    "walletBalance": 5000000
  }
}
```

---

### 3. Xem Tất Cả Payments (Tổng Giá Tiền Thanh Toán Thành Công) ⭐

**Endpoint:** `GET /api/admin/payments`

**Query Parameters:**
- `status` (String, optional): Filter theo status
  - `PAID` - Chỉ lấy payments thành công ⭐
  - `PENDING`, `FAILED`, `CANCELLED`, `EXPIRED`
  - Không có → Lấy tất cả
- `page` (Integer, default: 0): Số trang
- `size` (Integer, default: 20): Số items mỗi trang

**Response Success (200):**
```json
{
  "success": true,
  "message": "Payments retrieved successfully",
  "data": {
    "payments": [
      {
        "id": 1,
        "orderCode": 123456789,
        "amountCents": 500000,
        "amount": 5000.00,
        "status": "PAID",
        "userId": 10,
        "description": "Payment for courses",
        "paidAt": "2024-12-05T10:30:00Z",
        "createdAt": "2024-12-05T10:25:00Z",
        "courseIds": [1, 2],
        "aiPackageId": null
      }
    ],
    "totalElements": 150,
    "totalPages": 8,
    "currentPage": 0,
    "pageSize": 20,
    "totalPaidCents": 50000000,
    "totalPaidAmount": 500000.00,
    "filterStatus": "PAID"
  }
}
```

**Ví dụ: Lấy chỉ payments thành công:**
```
GET /api/admin/payments?status=PAID&page=0&size=20
```

---

### 4. List Tất Cả Teachers

**Endpoint:** `GET /api/admin/teachers`

**Response Success (200):**
```json
{
  "success": true,
  "message": "Teachers retrieved successfully",
  "data": {
    "teachers": [
      {
        "id": 5,
        "email": "teacher1@example.com",
        "username": "teacher1",
        "displayName": "Nguyễn Văn A",
        "approvalStatus": "APPROVED",
        "publishedCourses": 5,
        "totalEnrollments": 150,
        "totalRevenueCents": 15000000,
        "totalRevenue": 150000.00,
        "walletBalance": 5000000,
        "createdAt": "2024-01-15T10:00:00"
      }
    ],
    "totalTeachers": 25,
    "approvedTeachers": 20,
    "pendingTeachers": 3
  }
}
```

---

## 📝 TypeScript Interfaces

```typescript
interface TeacherDetailsResponse {
  teacher: {
    id: number;
    email: string;
    username: string | null;
    displayName: string | null;
    bio: string | null;
    currentJlptLevel: string;
    approvalStatus: 'NONE' | 'PENDING' | 'APPROVED' | 'REJECTED';
    walletBalance: number;
    createdAt: string;
  };
  courses: Array<{
    id: number;
    title: string;
    slug: string;
    status: string;
    priceCents: number | null;
    discountedPriceCents: number | null;
    publishedAt: string | null;
  }>;
  statistics: {
    totalCourses: number;
    publishedCourses: number;
    draftCourses: number;
    pendingCourses: number;
    totalEnrollments: number;
  };
  revenue: {
    totalRevenueCents: number;
    totalRevenue: number;
    monthlyRevenueCents: number;
    monthlyRevenue: number;
    currentMonth: string;
  };
}

interface PaymentItem {
  id: number;
  orderCode: number;
  amountCents: number;
  amount: number;
  status: 'PENDING' | 'PAID' | 'CANCELLED' | 'FAILED' | 'EXPIRED';
  userId: number;
  description: string | null;
  paidAt: string | null;
  createdAt: string;
  courseIds: number[];
  aiPackageId: number | null;
}

interface PaymentsResponse {
  payments: PaymentItem[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  totalPaidCents: number;
  totalPaidAmount: number;
  filterStatus: string | 'ALL';
}
```

---

## 🎨 UI/UX Suggestions

### Payments Page:
- Filter dropdown: `[All] [PAID] [PENDING] [FAILED]`
- Hiển thị `totalPaidAmount` nổi bật ở đầu trang
- Status badges với màu:
  - `PAID` → Green ✅
  - `PENDING` → Yellow
  - `FAILED` → Red
- Pagination controls
- Click vào courseIds → Navigate đến course
- Click vào userId → Navigate đến user profile

### Teacher Details Page:
- Approval status badge với màu
- Revenue cards (Total, Monthly)
- Courses list với tabs (All/Published/Draft)
- Link đến revenue detail page

---

## ⚠️ Error Handling

**401 Unauthorized:** Redirect to login  
**403 Forbidden:** Show error, hide admin features  
**404 Not Found:** Show "Teacher not found" message  
**500 Error:** Show error message, retry button

---

## 📊 Data Formatting Helpers

```typescript
// Format currency (VND)
function formatCurrency(cents: number): string {
  const vnd = cents / 100;
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(vnd);
}

// Format approval status badge
function getApprovalStatusBadge(status: string) {
  switch (status) {
    case 'APPROVED': return { label: 'Đã Duyệt', color: 'success' };
    case 'PENDING': return { label: 'Chờ Duyệt', color: 'warning' };
    case 'REJECTED': return { label: 'Từ Chối', color: 'error' };
    default: return { label: 'Chưa Nộp', color: 'default' };
  }
}
```

---

## ✅ Checklist Implementation

- [ ] Setup API service với base URL `/api/admin`
- [ ] Add admin role check guard
- [ ] Implement teacher details page
- [ ] Implement payments page với filter `status=PAID`
- [ ] Implement teachers list page
- [ ] Implement revenue detail page
- [ ] Format currency (VND)
- [ ] Format dates
- [ ] Add status badges
- [ ] Add pagination
- [ ] Test với real data

---

**Tài liệu này cung cấp đầy đủ thông tin để FE tích hợp các tính năng admin!** 🚀

