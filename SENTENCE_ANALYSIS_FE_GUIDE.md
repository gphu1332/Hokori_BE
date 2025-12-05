# 📝 Sentence Analysis API Guide for Frontend

## 🎯 Mục Đích

Tính năng **Phân Tích Câu** giúp người dùng học tiếng Nhật bằng cách:
- Nhập một câu tiếng Nhật (max 50 ký tự)
- Chọn trình độ JLPT (N5-N1)
- Nhận được phân tích chi tiết về **từ vựng** và **ngữ pháp** đáng chú ý
- Thay vì ChatGPT/Gemini trả về response dài dòng → Web này tách riêng từ vựng và ngữ pháp để dễ nhìn và tập trung hơn

---

## 🔌 Base URL
```
/api/ai
```

**Authorization:** 
- Public endpoints (không cần authentication)

---

## 📋 API Endpoints

### 1. Phân Tích Câu (Main API) ⭐

**Endpoint:** `POST /api/ai/sentence-analysis`

**Description:** Phân tích câu tiếng Nhật để lấy thông tin về từ vựng và ngữ pháp đáng chú ý. Tập trung vào các từ vựng và ngữ pháp phù hợp với trình độ user.

**⚠️ QUAN TRỌNG:** 
- **POST này sẽ gọi AI (Gemini) ngay lập tức** và trả về kết quả phân tích trong response body
- **KHÔNG cần GET riêng** để lấy kết quả
- **Synchronous**: FE gửi POST → Backend gọi AI → Trả về kết quả ngay trong response body
- **Response time**: Có thể mất 2-5 giây tùy vào độ phức tạp của câu

**Request:**
```json
{
  "sentence": "私は日本語を勉強しています",
  "level": "N5"
}
```

**Request Fields:**
- `sentence` (String, required, max 50 chars): Câu tiếng Nhật cần phân tích
- `level` (String, required): Trình độ JLPT (`N5`, `N4`, `N3`, `N2`, `N1`)

**Response Success (200):**
```json
{
  "success": true,
  "message": "Sentence analysis completed",
  "data": {
    "sentence": "私は日本語を勉強しています",
    "level": "N5",
    "vocabulary": [
      {
        "word": "私",
        "reading": "わたし",
        "meaningVi": "tôi",
        "jlptLevel": "N5",
        "importance": "high",
        "examples": [
          "私は学生です。",
          "私の本です。"
        ],
        "kanjiVariants": ["私", "わたし"],
        "kanjiDetails": {
          "radical": "禾",
          "strokeCount": 7,
          "onyomi": "シ",
          "kunyomi": "わたし",
          "relatedWords": ["私的", "私立"]
        }
      },
      {
        "word": "日本語",
        "reading": "にほんご",
        "meaningVi": "tiếng Nhật",
        "jlptLevel": "N5",
        "importance": "high",
        "examples": [
          "日本語を勉強します。",
          "日本語が難しいです。"
        ],
        "kanjiVariants": ["日本語", "にほんご"]
      }
    ],
    "grammar": [
      {
        "pattern": "を + verb",
        "jlptLevel": "N5",
        "explanationVi": "Trợ từ を được dùng để đánh dấu tân ngữ trực tiếp",
        "example": "本を読みます",
        "notes": "Lưu ý: Không nhầm với は (chủ đề)",
        "examples": [
          "本を読みます。",
          "コーヒーを飲みます。",
          "音楽を聞きます。"
        ],
        "confusingPatterns": [
          {
            "pattern": "は + verb",
            "difference": "は đánh dấu chủ đề, を đánh dấu tân ngữ trực tiếp",
            "example": "私は本を読みます。"
          }
        ]
      },
      {
        "pattern": "ています",
        "jlptLevel": "N5",
        "explanationVi": "Diễn tả hành động đang diễn ra hoặc trạng thái hiện tại",
        "example": "勉強しています",
        "notes": "Có thể dùng cho cả hành động và trạng thái",
        "examples": [
          "勉強しています。",
          "食べています。",
          "読んでいます。"
        ],
        "confusingPatterns": [
          {
            "pattern": "ます",
            "difference": "ます diễn tả hành động thường xuyên/tương lai, ています diễn tả hành động đang diễn ra",
            "example": "勉強します vs 勉強しています"
          }
        ]
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
      "彼は日本語を勉強しています。",
      "彼女は中国語を勉強しています。"
    ]
  }
}
```

**Response Error (200 với success: false):**
```json
{
  "success": false,
  "message": "Invalid JLPT level. Valid levels: N5, N4, N3, N2, N1",
  "data": null
}
```

**Error Cases:**
- `Invalid JLPT level`: Level không hợp lệ
- `Sentence exceeds maximum length of 50 characters`: Câu quá dài
- `Sentence analysis service is not available`: Service chưa được cấu hình
- `Sentence analysis failed: {error}`: Lỗi khi phân tích

---

### 2. Lấy Danh Sách Câu Ví Dụ (Optional - Không phải kết quả phân tích)

**Endpoint:** `GET /api/ai/sentence-examples/{level}`

**Description:** Lấy danh sách các câu ví dụ phù hợp cho sentence analysis (KHÔNG phải conversation practice). Có thể dùng để suggest câu cho user chọn thay vì tự nhập.

**⚠️ LƯU Ý:** 
- **Endpoint này CHỈ trả về danh sách câu ví dụ**, KHÔNG phải kết quả phân tích
- Sau khi user chọn câu từ danh sách này → FE phải gọi `POST /sentence-analysis` để lấy kết quả phân tích

**Path Parameters:**
- `level` (String, required): Trình độ JLPT (`N5`, `N4`, `N3`, `N2`, `N1`)

**Response Success (200):**
```json
{
  "success": true,
  "message": "Example sentences for sentence analysis - N5",
  "data": {
    "level": "N5",
    "sentences": [
      {
        "sentence": "私は毎日日本語を勉強します。",
        "translation": "Tôi học tiếng Nhật mỗi ngày."
      },
      {
        "sentence": "この本はとても面白いです。",
        "translation": "Cuốn sách này rất thú vị."
      }
    ],
    "count": 2
  }
}
```

---

### 3. Lấy Câu Ngẫu Nhiên (Optional - Không phải kết quả phân tích)

**Endpoint:** `GET /api/ai/sentence-examples/{level}/random`

**Description:** Lấy một câu ngẫu nhiên phù hợp cho sentence analysis.

**⚠️ LƯU Ý:** 
- **Endpoint này CHỈ trả về 1 câu ngẫu nhiên**, KHÔNG phải kết quả phân tích
- Sau khi nhận câu ngẫu nhiên → FE phải gọi `POST /sentence-analysis` để lấy kết quả phân tích

**Path Parameters:**
- `level` (String, required): Trình độ JLPT (`N5`, `N4`, `N3`, `N2`, `N1`)

**Response Success (200):**
```json
{
  "success": true,
  "message": "Random sentence example for N5",
  "data": {
    "sentence": "私は毎日日本語を勉強します。",
    "translation": "Tôi học tiếng Nhật mỗi ngày."
  }
}
```

---

## 📊 Response Structure Chi Tiết

### VocabularyItem
```typescript
interface VocabularyItem {
  word: string;                    // Từ tiếng Nhật
  reading: string;                 // Cách đọc hiragana
  meaningVi: string;               // Nghĩa tiếng Việt
  jlptLevel: "N5" | "N4" | "N3" | "N2" | "N1";
  importance: "high" | "medium" | "low";  // Mức độ quan trọng
  examples?: string[];             // Ví dụ sử dụng từ vựng
  kanjiVariants?: string[];        // Biến thể kanji (hiragana → kanji hoặc ngược lại)
  kanjiDetails?: {
    radical: string;               // Bộ thủ (phân tích phù hợp trình độ)
    strokeCount: number;           // Số nét
    onyomi: string;                // Cách đọc onyomi
    kunyomi: string;               // Cách đọc kunyomi
    relatedWords: string[];        // Các từ liên quan
  };
}
```

### GrammarItem
```typescript
interface GrammarItem {
  pattern: string;                 // Tên pattern ngữ pháp
  jlptLevel: "N5" | "N4" | "N3" | "N2" | "N1";
  explanationVi: string;           // Giải thích bằng tiếng Việt
  example: string;                 // Ví dụ câu
  notes?: string;                  // Ghi chú và lỗi thường gặp
  examples?: string[];             // Danh sách ví dụ sử dụng pattern
  confusingPatterns?: {
    pattern: string;               // Pattern dễ nhầm
    difference: string;            // Giải thích sự khác biệt (tiếng Việt)
    example: string;               // Ví dụ minh họa
  }[];
}
```

### SentenceBreakdown
```typescript
interface SentenceBreakdown {
  subject?: string;                // Chủ ngữ
  predicate: string;              // Vị ngữ/động từ
  object?: string;                 // Tân ngữ
  particles: string[];              // Danh sách trợ từ
  explanationVi: string;            // Giải thích chi tiết cấu trúc câu (tiếng Việt)
}
```

### SentenceAnalysisResponse
```typescript
interface SentenceAnalysisResponse {
  sentence: string;                // Câu gốc
  level: "N5" | "N4" | "N3" | "N2" | "N1";
  vocabulary: VocabularyItem[];    // Danh sách từ vựng đáng chú ý
  grammar: GrammarItem[];           // Danh sách ngữ pháp đáng chú ý
  sentenceBreakdown?: SentenceBreakdown;  // Phân tích cấu trúc câu
  relatedSentences?: string[];      // Các câu ví dụ liên quan
}
```

---

## 🔄 Luồng Sử Dụng (Flow)

### ⚠️ QUAN TRỌNG: Flow Gọi AI

**Tất cả kết quả phân tích từ AI đều trả về trong `POST /sentence-analysis`:**

```
POST /api/ai/sentence-analysis
  ↓
Backend nhận request
  ↓
Backend gọi AI (Gemini) ngay lập tức (synchronous)
  ↓
AI phân tích câu (mất 2-5 giây)
  ↓
Backend trả về kết quả trong response body của POST đó
  ↓
FE nhận kết quả và hiển thị
```

**KHÔNG có GET riêng để lấy kết quả phân tích!**

---

### Flow 1: User Nhập Câu Trực Tiếp
```
1. User vào màn hình "Phân Tích Câu"
2. User chọn trình độ JLPT (N5-N1) từ dropdown
3. User nhập câu tiếng Nhật vào input (max 50 ký tự)
4. User click "Phân Tích" hoặc tự động submit khi nhập xong
5. FE gọi: POST /api/ai/sentence-analysis
   - Request body: { "sentence": "...", "level": "N5" }
6. Hiển thị loading spinner (chờ AI xử lý, có thể mất 2-5 giây)
7. Nhận response (kết quả phân tích đã có sẵn trong response body):
   {
     "success": true,
     "data": {
       "vocabulary": [...],
       "grammar": [...],
       "sentenceBreakdown": {...},
       "relatedSentences": [...]
     }
   }
8. Hiển thị kết quả:
   - Section TỪ VỰNG (vocabulary)
   - Section NGỮ PHÁP (grammar)
   - Section CẤU TRÚC CÂU (sentenceBreakdown) - optional
   - Section CÁC CÂU LIÊN QUAN (relatedSentences) - optional
```

### Flow 2: User Chọn Câu Từ Danh Sách
```
1. User vào màn hình "Phân Tích Câu"
2. User chọn trình độ JLPT (N5-N1)
3. FE gọi: GET /api/ai/sentence-examples/{level}
   → Nhận danh sách câu ví dụ (CHỈ là câu, KHÔNG phải kết quả phân tích)
4. Hiển thị danh sách câu ví dụ cho user chọn
5. User chọn một câu
6. FE tự động điền câu đó vào input
7. FE gọi: POST /api/ai/sentence-analysis
   - Request body: { "sentence": "câu đã chọn", "level": "N5" }
8. Hiển thị loading spinner
9. Nhận response (kết quả phân tích)
10. Hiển thị kết quả phân tích
```

### Flow 3: Random Sentence
```
1. User vào màn hình "Phân Tích Câu"
2. User chọn trình độ JLPT (N5-N1)
3. User click "Câu Ngẫu Nhiên"
4. FE gọi: GET /api/ai/sentence-examples/{level}/random
   → Nhận 1 câu ngẫu nhiên (CHỈ là câu, KHÔNG phải kết quả phân tích)
5. Hiển thị câu ngẫu nhiên
6. FE tự động điền câu đó vào input
7. FE gọi: POST /api/ai/sentence-analysis
   - Request body: { "sentence": "câu ngẫu nhiên", "level": "N5" }
8. Hiển thị loading spinner
9. Nhận response (kết quả phân tích)
10. Hiển thị kết quả phân tích
```

---

## 🎨 UI/UX Recommendations

### Màn Hình Phân Tích Câu

```
┌─────────────────────────────────────────────────────────────┐
│ 📝 Phân Tích Câu Tiếng Nhật                                 │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│ Trình độ: [N5 ▼]                                            │
│                                                              │
│ [Nhập câu tiếng Nhật...]                                    │
│ 私は日本語を勉強しています                                    │
│ (0/50 ký tự)                                                │
│                                                              │
│ [🔍 Phân Tích] [📋 Chọn từ danh sách] [🎲 Câu ngẫu nhiên] │
│                                                              │
├─────────────────────────────────────────────────────────────┤
│ 📊 KẾT QUẢ PHÂN TÍCH                                         │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│ 📖 TỪ VỰNG (2 từ)                                           │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 私 (わたし) - tôi [N5] [High]                          │ │
│ │ • Kanji: 私 (7 nét, bộ thủ: 禾)                       │ │
│ │ • Onyomi: シ | Kunyomi: わたし                        │ │
│ │ • Variants: [私] [わたし]                              │ │
│ │ • Ví dụ:                                                │ │
│ │   - 私は学生です。                                      │ │
│ │   - 私の本です。                                        │ │
│ │ • Từ liên quan: 私的, 私立                             │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                              │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 日本語 (にほんご) - tiếng Nhật [N5] [High]            │ │
│ │ • Ví dụ:                                                │ │
│ │   - 日本語を勉強します。                                │ │
│ │   - 日本語が難しいです。                                │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                              │
│ 📚 NGỮ PHÁP (2 patterns)                                    │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ を + verb [N5]                                          │ │
│ │ • Giải thích: Trợ từ を được dùng để đánh dấu tân ngữ  │ │
│ │ • Ví dụ: 本を読みます。                                 │ │
│ │ • Ghi chú: Lưu ý: Không nhầm với は (chủ đề)            │ │
│ │ • Ví dụ sử dụng:                                        │ │
│ │   - 本を読みます。                                      │ │
│ │   - コーヒーを飲みます。                                │ │
│ │   - 音楽を聞きます。                                    │ │
│ │ • ⚠️ Dễ nhầm với:                                       │ │
│ │   - は + verb: は đánh dấu chủ đề, を đánh dấu tân ngữ │ │
│ │     Ví dụ: 私は本を読みます。                          │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                              │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ ています [N5]                                           │ │
│ │ • Giải thích: Diễn tả hành động đang diễn ra            │ │
│ │ • Ví dụ: 勉強しています                                 │ │
│ │ • Ghi chú: Có thể dùng cho cả hành động và trạng thái   │ │
│ │ • Ví dụ sử dụng:                                        │ │
│ │   - 勉強しています。                                    │ │
│ │   - 食べています。                                      │ │
│ │ • ⚠️ Dễ nhầm với:                                       │ │
│ │   - ます: ます diễn tả hành động thường xuyên/tương lai │ │
│ │     Ví dụ: 勉強します vs 勉強しています                 │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                              │
│ 🔍 CẤU TRÚC CÂU (Optional - có thể collapse)               │
│ • Chủ ngữ: 私                                               │
│ • Tân ngữ: 日本語                                            │
│ • Động từ: 勉強しています                                    │
│ • Trợ từ: [は] [を]                                         │
│ • Giải thích: Câu này có cấu trúc: Chủ ngữ (私) + Trợ từ...│
│                                                              │
│ 💡 CÁC CÂU LIÊN QUAN (Optional - có thể collapse)           │
│ • 私は英語を勉強しています。                                │
│ • 彼は日本語を勉強しています。                              │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Design Tips

1. **Tách riêng Vocabulary và Grammar**: 
   - Dùng 2 tabs hoặc 2 sections riêng biệt
   - Mỗi section có thể scroll độc lập

2. **Vocabulary Card**:
   - Hiển thị word lớn, reading nhỏ hơn
   - Badge cho JLPT level và importance
   - Collapse/expand cho kanji details
   - List examples dạng bullet

3. **Grammar Card**:
   - Pattern name nổi bật
   - Explanation rõ ràng
   - Confusing patterns có thể highlight hoặc expand riêng
   - Examples có thể click để copy

4. **Sentence Breakdown**:
   - Có thể collapse/expand
   - Highlight các phần trong câu gốc

5. **Related Sentences**:
   - Có thể collapse/expand
   - Click vào câu → tự động phân tích câu đó

---

## 📝 Notes

1. **Max Length**: Câu tối đa 50 ký tự
2. **Tất cả giải thích bằng tiếng Việt**: API được thiết kế cho người Việt học tiếng Nhật
3. **Tập trung vào từ vựng và ngữ pháp đáng chú ý**: Không phải tất cả từ/ngữ pháp trong câu, chỉ những cái đáng học ở trình độ đó
4. **Kanji Breakdown**: Phân tích kanji phù hợp với trình độ user (N5 sẽ đơn giản hơn N1)
5. **Confusing Patterns**: Chỉ hiển thị các pattern cùng trình độ dễ nhầm
6. **Examples**: Mỗi từ vựng và ngữ pháp đều có 2-3 ví dụ
7. **Kanji Variants**: 
   - Nếu từ là hiragana → gợi ý kanji
   - Nếu từ là kanji → hiển thị hiragana và các cách viết khác

## ⚠️ Lưu Ý Quan Trọng Về Flow

### ✅ ĐÚNG:
- **POST `/sentence-analysis`** → Gọi AI ngay → Nhận kết quả phân tích trong response body
- **GET `/sentence-examples/{level}`** → Chỉ lấy danh sách câu ví dụ (không phải kết quả phân tích)
- **GET `/sentence-examples/{level}/random`** → Chỉ lấy 1 câu ngẫu nhiên (không phải kết quả phân tích)

### ❌ SAI:
- ~~POST để submit → Sau đó GET để lấy kết quả~~ (KHÔNG đúng!)
- ~~GET `/sentence-analysis` để lấy kết quả~~ (KHÔNG có endpoint này!)

### 📊 Tóm Tắt:
- **1 endpoint duy nhất trả về kết quả phân tích từ AI**: `POST /sentence-analysis`
- **2 endpoints chỉ để lấy câu ví dụ** (không phải kết quả phân tích): `GET /sentence-examples/{level}` và `GET /sentence-examples/{level}/random`
- **Response time**: POST `/sentence-analysis` có thể mất 2-5 giây vì phải gọi AI
- **Synchronous**: Tất cả đều synchronous, không có async/polling

---

## 💻 Cách FE Call API (Code Examples)

### 1. Phân Tích Câu (Main API)

#### Sử dụng Fetch API (Vanilla JavaScript)

```javascript
// Function để phân tích câu
async function analyzeSentence(sentence, level) {
  try {
    // Hiển thị loading
    setLoading(true);
    
    // Gọi API
    const response = await fetch('/api/ai/sentence-analysis', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        sentence: sentence,
        level: level // "N5", "N4", "N3", "N2", "N1"
      })
    });
    
    const result = await response.json();
    
    // Kiểm tra kết quả
    if (result.success) {
      // result.data chứa toàn bộ kết quả phân tích
      const analysisData = result.data;
      
      console.log('Vocabulary:', analysisData.vocabulary);
      console.log('Grammar:', analysisData.grammar);
      console.log('Sentence Breakdown:', analysisData.sentenceBreakdown);
      console.log('Related Sentences:', analysisData.relatedSentences);
      
      // Hiển thị kết quả lên UI
      displayAnalysisResult(analysisData);
    } else {
      // Xử lý lỗi
      console.error('Error:', result.message);
      showError(result.message);
    }
  } catch (error) {
    console.error('Network error:', error);
    showError('Lỗi kết nối. Vui lòng thử lại.');
  } finally {
    setLoading(false);
  }
}

// Sử dụng
analyzeSentence('私は日本語を勉強しています', 'N5');
```

#### Sử dụng Axios (React/Vue)

```typescript
import axios from 'axios';

// TypeScript interfaces
interface SentenceAnalysisRequest {
  sentence: string;
  level: 'N5' | 'N4' | 'N3' | 'N2' | 'N1';
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
}

interface SentenceAnalysisResponse {
  sentence: string;
  level: string;
  vocabulary: VocabularyItem[];
  grammar: GrammarItem[];
  sentenceBreakdown?: SentenceBreakdown;
  relatedSentences?: string[];
}

// Service function
async function analyzeSentence(
  sentence: string, 
  level: 'N5' | 'N4' | 'N3' | 'N2' | 'N1'
): Promise<SentenceAnalysisResponse> {
  const response = await axios.post<ApiResponse<SentenceAnalysisResponse>>(
    '/api/ai/sentence-analysis',
    {
      sentence: sentence,
      level: level
    } as SentenceAnalysisRequest
  );
  
  if (!response.data.success) {
    throw new Error(response.data.message);
  }
  
  return response.data.data!;
}

// React Hook Example
import { useState } from 'react';

function useSentenceAnalysis() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<SentenceAnalysisResponse | null>(null);
  
  const analyze = async (sentence: string, level: 'N5' | 'N4' | 'N3' | 'N2' | 'N1') => {
    setLoading(true);
    setError(null);
    
    try {
      const data = await analyzeSentence(sentence, level);
      setResult(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Lỗi không xác định');
    } finally {
      setLoading(false);
    }
  };
  
  return { analyze, loading, error, result };
}

// Sử dụng trong component
function SentenceAnalysisComponent() {
  const { analyze, loading, error, result } = useSentenceAnalysis();
  const [sentence, setSentence] = useState('');
  const [level, setLevel] = useState<'N5' | 'N4' | 'N3' | 'N2' | 'N1'>('N5');
  
  const handleSubmit = () => {
    if (sentence.trim().length === 0) {
      alert('Vui lòng nhập câu');
      return;
    }
    
    if (sentence.length > 50) {
      alert('Câu không được quá 50 ký tự');
      return;
    }
    
    analyze(sentence, level);
  };
  
  return (
    <div>
      <input 
        value={sentence}
        onChange={(e) => setSentence(e.target.value)}
        maxLength={50}
        placeholder="Nhập câu tiếng Nhật..."
      />
      <select value={level} onChange={(e) => setLevel(e.target.value as any)}>
        <option value="N5">N5</option>
        <option value="N4">N4</option>
        <option value="N3">N3</option>
        <option value="N2">N2</option>
        <option value="N1">N1</option>
      </select>
      <button onClick={handleSubmit} disabled={loading}>
        {loading ? 'Đang phân tích...' : 'Phân Tích'}
      </button>
      
      {error && <div className="error">{error}</div>}
      
      {result && (
        <div>
          <h3>Từ Vựng ({result.vocabulary.length})</h3>
          {result.vocabulary.map((vocab, idx) => (
            <div key={idx}>
              <strong>{vocab.word}</strong> ({vocab.reading}) - {vocab.meaningVi}
            </div>
          ))}
          
          <h3>Ngữ Pháp ({result.grammar.length})</h3>
          {result.grammar.map((grammar, idx) => (
            <div key={idx}>
              <strong>{grammar.pattern}</strong> - {grammar.explanationVi}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
```

---

### 2. Lấy Danh Sách Câu Ví Dụ

```typescript
// Lấy danh sách câu ví dụ
async function getExampleSentences(level: string) {
  const response = await axios.get<ApiResponse<{
    level: string;
    sentences: Array<{
      sentence: string;
      translation: string;
    }>;
    count: number;
  }>>(`/api/ai/sentence-examples/${level}`);
  
  if (!response.data.success) {
    throw new Error(response.data.message);
  }
  
  return response.data.data!;
}

// Sử dụng
const examples = await getExampleSentences('N5');
console.log(examples.sentences); // Array of sentences
```

---

### 3. Lấy Câu Ngẫu Nhiên

```typescript
// Lấy câu ngẫu nhiên
async function getRandomSentence(level: string) {
  const response = await axios.get<ApiResponse<{
    sentence: string;
    translation: string;
  }>>(`/api/ai/sentence-examples/${level}/random`);
  
  if (!response.data.success) {
    throw new Error(response.data.message);
  }
  
  return response.data.data!;
}

// Sử dụng: Lấy câu ngẫu nhiên và tự động phân tích
async function handleRandomSentence(level: 'N5' | 'N4' | 'N3' | 'N2' | 'N1') {
  try {
    // Bước 1: Lấy câu ngẫu nhiên
    const randomSentence = await getRandomSentence(level);
    
    // Bước 2: Tự động phân tích câu đó
    const analysisResult = await analyzeSentence(randomSentence.sentence, level);
    
    // Hiển thị kết quả
    displayAnalysisResult(analysisResult);
  } catch (error) {
    console.error('Error:', error);
  }
}
```

---

### 4. Flow Hoàn Chỉnh: User Chọn Câu Từ Danh Sách

```typescript
// Component: User chọn câu từ danh sách
function SentenceExampleList({ level }: { level: string }) {
  const [examples, setExamples] = useState<Array<{sentence: string, translation: string}>>([]);
  const { analyze } = useSentenceAnalysis();
  
  useEffect(() => {
    // Load danh sách câu ví dụ khi component mount
    getExampleSentences(level).then(data => {
      setExamples(data.sentences);
    });
  }, [level]);
  
  const handleSelectSentence = (sentence: string) => {
    // User chọn câu → Tự động phân tích
    analyze(sentence, level as any);
  };
  
  return (
    <div>
      <h3>Chọn câu để phân tích:</h3>
      {examples.map((example, idx) => (
        <div 
          key={idx} 
          onClick={() => handleSelectSentence(example.sentence)}
          style={{ cursor: 'pointer' }}
        >
          <p>{example.sentence}</p>
          <p>{example.translation}</p>
        </div>
      ))}
    </div>
  );
}
```

---

### 5. Xử Lý Lỗi

```typescript
try {
  const result = await analyzeSentence(sentence, level);
  // Success
} catch (error) {
  if (axios.isAxiosError(error)) {
    // Lỗi từ API
    if (error.response) {
      const apiError = error.response.data as ApiResponse<null>;
      console.error('API Error:', apiError.message);
      
      // Xử lý các loại lỗi cụ thể
      if (apiError.message.includes('exceeds maximum length')) {
        alert('Câu quá dài. Tối đa 50 ký tự.');
      } else if (apiError.message.includes('Invalid JLPT level')) {
        alert('Trình độ không hợp lệ.');
      } else {
        alert('Lỗi: ' + apiError.message);
      }
    } else if (error.request) {
      // Không nhận được response
      alert('Không thể kết nối đến server. Vui lòng thử lại.');
    }
  } else {
    // Lỗi khác
    console.error('Unexpected error:', error);
    alert('Đã xảy ra lỗi không xác định.');
  }
}
```

---

### 6. Base URL Configuration

```typescript
// config/api.ts
const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

// Setup axios instance
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Sử dụng
const response = await apiClient.post('/api/ai/sentence-analysis', {
  sentence: sentence,
  level: level
});
```

---

## 📋 Checklist cho FE

- [ ] Validate input: sentence không rỗng, max 50 ký tự
- [ ] Validate level: chỉ nhận N5, N4, N3, N2, N1
- [ ] Hiển thị loading spinner khi gọi API (có thể mất 2-5 giây)
- [ ] Xử lý lỗi và hiển thị thông báo cho user
- [ ] Hiển thị kết quả phân tích:
  - [ ] Section Từ Vựng (vocabulary array)
  - [ ] Section Ngữ Pháp (grammar array)
  - [ ] Section Cấu Trúc Câu (sentenceBreakdown) - optional
  - [ ] Section Câu Liên Quan (relatedSentences) - optional
- [ ] Nếu dùng GET `/sentence-examples` → Nhớ gọi POST `/sentence-analysis` sau khi user chọn câu

---

## 🔗 Related APIs

- `GET /api/ai/sentence-examples/{level}` - Lấy danh sách câu ví dụ
- `GET /api/ai/sentence-examples/{level}/random` - Lấy câu ngẫu nhiên
- `POST /api/ai/translate` - Dịch câu (nếu cần)
- `POST /api/ai/text-to-speech` - Chuyển text thành giọng nói (nếu cần)

---

## ⚠️ Lưu Ý Quan Trọng

- **KHÔNG dùng `/kaiwa-sentences/{level}`**: API đó dành cho conversation practice (speaking), không phải sentence analysis
- **Dùng `/sentence-examples/{level}`**: API này dành riêng cho sentence analysis
- **Response structure**: Vocabulary và Grammar được tách riêng để FE có thể thiết kế UI riêng cho từng phần
- **Focus**: Tập trung vào từ vựng và ngữ pháp đáng chú ý, không phải tất cả

