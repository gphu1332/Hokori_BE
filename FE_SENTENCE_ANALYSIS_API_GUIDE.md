# Hướng dẫn API Sentence Analysis cho Frontend

## Tổng quan

API phân tích câu tiếng Nhật với tính năng tự động dịch:
- **Nhập tiếng Việt** → Tự động dịch sang tiếng Nhật phù hợp với level → Phân tích
- **Nhập tiếng Nhật** → Tự động dịch sang tiếng Việt → Phân tích

## Endpoint

```
POST /api/ai/sentence-analysis
```

**Authentication:** Required (JWT token)

**Content-Type:** `application/json`

---

## Request

### Request Body

```typescript
interface SentenceAnalysisRequest {
  sentence: string;  // Câu tiếng Việt hoặc tiếng Nhật (max 50 characters)
  level: string;     // JLPT level: "N5" | "N4" | "N3" | "N2" | "N1"
}
```

### Validation Rules

- `sentence`: 
  - Required
  - Min length: 1
  - Max length: 50 characters
- `level`: 
  - Required
  - Must be one of: "N5", "N4", "N3", "N2", "N1"

### Example Request

```json
{
  "sentence": "私は日本語を勉強しています",
  "level": "N5"
}
```

hoặc

```json
{
  "sentence": "Tôi đang học tiếng Nhật",
  "level": "N5"
}
```

---

## Response

### Success Response (200 OK)

```typescript
interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

interface SentenceAnalysisResponse {
  sentence: string;                    // Câu tiếng Nhật (đã dịch hoặc gốc)
  originalSentence?: string | null;    // Câu tiếng Việt gốc (nếu input là tiếng Việt)
  isTranslated?: boolean;               // true nếu đã dịch từ tiếng Việt
  vietnameseTranslation?: string | null; // Bản dịch tiếng Việt (nếu input là tiếng Nhật)
  level: string;                        // JLPT level: "N5" | "N4" | "N3" | "N2" | "N1"
  vocabulary: VocabularyItem[];        // Danh sách từ vựng
  grammar: GrammarItem[];               // Danh sách ngữ pháp
  sentenceBreakdown?: SentenceBreakdown; // Phân tích cấu trúc câu
  relatedSentences?: string[];          // Các câu liên quan
}

interface VocabularyItem {
  word: string;                         // Từ vựng (kanji/hiragana)
  reading: string;                      // Cách đọc (hiragana)
  meaningVi: string;                   // Nghĩa tiếng Việt
  jlptLevel: string;                   // JLPT level: "N5" | "N4" | "N3" | "N2" | "N1"
  importance: string;                   // "high" | "medium" | "low"
  kanjiDetails?: KanjiDetails;          // Chi tiết kanji (nếu có)
  examples?: string[];                  // Ví dụ câu
  kanjiVariants?: string[];            // Các biến thể kanji/hiragana
}

interface KanjiDetails {
  radical?: string;                     // Bộ thủ
  strokeCount?: number;                 // Số nét
  onyomi?: string;                      // Cách đọc onyomi
  kunyomi?: string;                     // Cách đọc kunyomi
  relatedWords?: string[];              // Từ liên quan
}

interface GrammarItem {
  pattern: string;                       // Tên mẫu ngữ pháp
  jlptLevel: string;                    // JLPT level
  explanationVi: string;                 // Giải thích tiếng Việt
  example: string;                       // Ví dụ
  notes?: string;                        // Ghi chú
  examples?: string[];                   // Các ví dụ khác
  confusingPatterns?: ConfusingPattern[]; // Các mẫu dễ nhầm lẫn
}

interface ConfusingPattern {
  pattern: string;                       // Tên mẫu
  difference: string;                    // Sự khác biệt (tiếng Việt)
  example: string;                       // Ví dụ
}

interface SentenceBreakdown {
  subject?: string;                      // Chủ ngữ
  predicate?: string;                    // Vị ngữ/động từ
  object?: string;                       // Tân ngữ
  particles?: string[];                  // Các trợ từ
  explanationVi?: string;                // Giải thích cấu trúc (tiếng Việt)
}
```

### Example Response - Input tiếng Nhật

```json
{
  "success": true,
  "message": "Sentence analysis completed",
  "data": {
    "sentence": "私は日本語を勉強しています",
    "originalSentence": null,
    "isTranslated": false,
    "vietnameseTranslation": "Tôi đang học tiếng Nhật",
    "level": "N5",
    "vocabulary": [
      {
        "word": "私",
        "reading": "わたし",
        "meaningVi": "tôi",
        "jlptLevel": "N5",
        "importance": "high",
        "kanjiDetails": {
          "radical": "禾",
          "strokeCount": 7,
          "onyomi": "シ",
          "kunyomi": "わたし",
          "relatedWords": ["私的", "私立"]
        },
        "examples": ["私は学生です。", "私の本です。"],
        "kanjiVariants": ["私", "わたし"]
      },
      {
        "word": "日本語",
        "reading": "にほんご",
        "meaningVi": "tiếng Nhật",
        "jlptLevel": "N5",
        "importance": "high",
        "examples": ["日本語を勉強します。"]
      }
    ],
    "grammar": [
      {
        "pattern": "を + verb",
        "jlptLevel": "N5",
        "explanationVi": "Trợ từ を được dùng để đánh dấu tân ngữ trực tiếp",
        "example": "本を読みます",
        "notes": "Lưu ý: Không nhầm với は (chủ đề)",
        "examples": ["本を読みます。", "コーヒーを飲みます。"]
      }
    ],
    "sentenceBreakdown": {
      "subject": "私",
      "predicate": "勉強しています",
      "object": "日本語",
      "particles": ["は", "を"],
      "explanationVi": "Câu này có cấu trúc: Chủ ngữ (私) + Trợ từ chủ đề (は) + Tân ngữ (日本語) + Trợ từ tân ngữ (を) + Động từ (勉強しています)"
    },
    "relatedSentences": [
      "私は英語を勉強しています。",
      "彼は日本語を勉強しています。"
    ]
  }
}
```

### Example Response - Input tiếng Việt

```json
{
  "success": true,
  "message": "Sentence analysis completed",
  "data": {
    "sentence": "私は日本語を勉強しています",
    "originalSentence": "Tôi đang học tiếng Nhật",
    "isTranslated": true,
    "vietnameseTranslation": null,
    "level": "N5",
    "vocabulary": [...],
    "grammar": [...],
    "sentenceBreakdown": {...},
    "relatedSentences": [...]
  }
}
```

---

## Error Responses

### 400 Bad Request - Invalid Level

```json
{
  "success": false,
  "message": "Invalid JLPT level. Valid levels: N5, N4, N3, N2, N1",
  "data": null
}
```

### 400 Bad Request - Unsupported Language

```json
{
  "success": false,
  "message": "Chỉ hỗ trợ tiếng Việt và tiếng Nhật. Vui lòng nhập câu tiếng Việt hoặc tiếng Nhật.",
  "data": null
}
```

### 400 Bad Request - Translation Failed

```json
{
  "success": false,
  "message": "Không thể dịch câu tiếng Việt sang tiếng Nhật: [error details]",
  "data": null
}
```

### 401 Unauthorized

```json
{
  "success": false,
  "message": "Unauthorized",
  "data": null
}
```

### 503 Service Unavailable - AI Service Disabled

```json
{
  "success": false,
  "message": "Sentence analysis service is not available",
  "data": null
}
```

---

## TypeScript Types

### Complete Type Definitions

```typescript
// Request Types
export interface SentenceAnalysisRequest {
  sentence: string;
  level: "N5" | "N4" | "N3" | "N2" | "N1";
}

// Response Types
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
}

export interface SentenceAnalysisResponse {
  sentence: string;
  originalSentence?: string | null;
  isTranslated?: boolean;
  vietnameseTranslation?: string | null;
  level: "N5" | "N4" | "N3" | "N2" | "N1";
  vocabulary: VocabularyItem[];
  grammar: GrammarItem[];
  sentenceBreakdown?: SentenceBreakdown;
  relatedSentences?: string[];
}

export interface VocabularyItem {
  word: string;
  reading: string;
  meaningVi: string;
  jlptLevel: "N5" | "N4" | "N3" | "N2" | "N1";
  importance: "high" | "medium" | "low";
  kanjiDetails?: KanjiDetails;
  examples?: string[];
  kanjiVariants?: string[];
}

export interface KanjiDetails {
  radical?: string;
  strokeCount?: number;
  onyomi?: string;
  kunyomi?: string;
  relatedWords?: string[];
}

export interface GrammarItem {
  pattern: string;
  jlptLevel: "N5" | "N4" | "N3" | "N2" | "N1";
  explanationVi: string;
  example: string;
  notes?: string;
  examples?: string[];
  confusingPatterns?: ConfusingPattern[];
}

export interface ConfusingPattern {
  pattern: string;
  difference: string;
  example: string;
}

export interface SentenceBreakdown {
  subject?: string;
  predicate?: string;
  object?: string;
  particles?: string[];
  explanationVi?: string;
}
```

---

## API Service Example

### Service Implementation

```typescript
// services/sentenceAnalysisService.ts

import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080';

export const analyzeSentence = async (
  request: SentenceAnalysisRequest
): Promise<SentenceAnalysisResponse> => {
  const token = localStorage.getItem('accessToken'); // hoặc cách lấy token của bạn
  
  const response = await axios.post<ApiResponse<SentenceAnalysisResponse>>(
    `${API_BASE_URL}/api/ai/sentence-analysis`,
    request,
    {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      }
    }
  );

  if (!response.data.success || !response.data.data) {
    throw new Error(response.data.message || 'Sentence analysis failed');
  }

  return response.data.data;
};
```

---

## Các trường hợp sử dụng

### 1. Input tiếng Nhật

**Request:**
```json
{
  "sentence": "私は日本語を勉強しています",
  "level": "N5"
}
```

**Response fields:**
- `sentence`: "私は日本語を勉強しています" (câu gốc)
- `originalSentence`: `null`
- `isTranslated`: `false`
- `vietnameseTranslation`: "Tôi đang học tiếng Nhật" (dịch để hiển thị)

### 2. Input tiếng Việt

**Request:**
```json
{
  "sentence": "Tôi đang học tiếng Nhật",
  "level": "N5"
}
```

**Response fields:**
- `sentence`: "私は日本語を勉強しています" (đã dịch sang Nhật)
- `originalSentence`: "Tôi đang học tiếng Nhật" (câu gốc)
- `isTranslated`: `true`
- `vietnameseTranslation`: `null`

### 3. Input tiếng Việt không dấu

**Request:**
```json
{
  "sentence": "Toi dang hoc tieng nhat",
  "level": "N5"
}
```

**Response:** Tương tự như input tiếng Việt có dấu (backend tự động detect)

### 4. Input có ký tự Latin (JLPT, N5, etc.)

**Request:**
```json
{
  "sentence": "私は JLPT の試験に備えるために昼も夜も勉強しています。",
  "level": "N5"
}
```

**Response:** Bình thường, backend tự động xử lý các từ viết tắt phổ biến

---

## Error Handling

### Các lỗi thường gặp

1. **Invalid Level**
   - Check: `level` phải là "N5", "N4", "N3", "N2", hoặc "N1"
   - Solution: Validate trước khi gửi request

2. **Unsupported Language**
   - Check: Input không phải tiếng Việt hoặc tiếng Nhật
   - Solution: Hiển thị message lỗi rõ ràng cho user

3. **Translation Failed**
   - Check: Backend không thể dịch câu tiếng Việt sang tiếng Nhật
   - Solution: Retry hoặc hiển thị lỗi

4. **Unauthorized**
   - Check: Token hết hạn hoặc không hợp lệ
   - Solution: Refresh token hoặc redirect login

### Error Handling Example

```typescript
try {
  const result = await analyzeSentence({
    sentence: "Tôi đang học tiếng Nhật",
    level: "N5"
  });
  
  // Success handling
  console.log("Analysis result:", result);
  
} catch (error: any) {
  if (error.response?.status === 400) {
    // Bad request - invalid input
    const message = error.response.data.message;
    console.error("Validation error:", message);
  } else if (error.response?.status === 401) {
    // Unauthorized - token expired
    console.error("Unauthorized - please login again");
    // Redirect to login
  } else if (error.response?.status === 503) {
    // Service unavailable
    console.error("AI service is currently unavailable");
  } else {
    // Other errors
    console.error("Unexpected error:", error.message);
  }
}
```

---

## Validation Rules

### Frontend Validation (trước khi gửi request)

```typescript
const validateSentenceAnalysisRequest = (
  request: SentenceAnalysisRequest
): string | null => {
  // Validate sentence
  if (!request.sentence || request.sentence.trim().length === 0) {
    return "Vui lòng nhập câu";
  }
  
  if (request.sentence.length > 50) {
    return "Câu không được quá 50 ký tự";
  }
  
  // Validate level
  const validLevels = ["N5", "N4", "N3", "N2", "N1"];
  if (!validLevels.includes(request.level)) {
    return "Vui lòng chọn level hợp lệ (N5-N1)";
  }
  
  return null; // Valid
};
```

---

## Response Field Usage Guide

### Khi nào dùng field nào?

1. **`sentence`**: Luôn hiển thị (câu tiếng Nhật để phân tích)

2. **`originalSentence`**: 
   - Chỉ có khi `isTranslated === true`
   - Hiển thị để user biết câu gốc tiếng Việt

3. **`vietnameseTranslation`**: 
   - Chỉ có khi input là tiếng Nhật (`isTranslated === false`)
   - Hiển thị để user hiểu nghĩa câu tiếng Nhật

4. **`isTranslated`**: 
   - Dùng để quyết định hiển thị `originalSentence` hay `vietnameseTranslation`

### Logic hiển thị

```typescript
if (result.isTranslated) {
  // Input là tiếng Việt
  // Hiển thị: originalSentence → sentence (đã dịch)
  displayOriginalSentence(result.originalSentence);
  displayJapaneseSentence(result.sentence);
} else {
  // Input là tiếng Nhật
  // Hiển thị: sentence → vietnameseTranslation
  displayJapaneseSentence(result.sentence);
  if (result.vietnameseTranslation) {
    displayVietnameseTranslation(result.vietnameseTranslation);
  }
}
```

---

## Notes

1. **Max length**: Câu input không được quá 50 ký tự
2. **Authentication**: Luôn cần JWT token trong header
3. **Language detection**: Backend tự động detect ngôn ngữ
4. **Translation**: 
   - Việt→Nhật: Dịch phù hợp với level (dùng Gemini)
   - Nhật→Việt: Dịch để hiển thị (dùng Google Translate)
5. **Mixed characters**: Backend tự động xử lý các ký tự Latin như "JLPT", "N5" trong câu tiếng Nhật

---

## Testing Checklist

- [ ] Test với câu tiếng Nhật thuần
- [ ] Test với câu tiếng Việt có dấu
- [ ] Test với câu tiếng Việt không dấu
- [ ] Test với câu có ký tự Latin (JLPT, N5)
- [ ] Test với các level khác nhau (N5-N1)
- [ ] Test error handling (invalid level, unsupported language)
- [ ] Test với token hết hạn
- [ ] Verify `isTranslated` flag đúng
- [ ] Verify `originalSentence` và `vietnameseTranslation` hiển thị đúng

