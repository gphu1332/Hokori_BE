# Hướng dẫn cập nhật FE cho Sentence Analysis

## Tổng quan thay đổi

Backend đã được cập nhật để **tự động phát hiện ngôn ngữ** và **dịch tiếng Việt sang tiếng Nhật** phù hợp với level trước khi phân tích.

## Thay đổi Response Structure

### Response mới có thêm 2 fields:

```typescript
interface SentenceAnalysisResponse {
  sentence: string;              // Câu tiếng Nhật (đã dịch hoặc gốc)
  originalSentence?: string;       // ⭐ MỚI: Câu tiếng Việt gốc (nếu có dịch)
  isTranslated?: boolean;         // ⭐ MỚI: true nếu đã dịch từ tiếng Việt
  level: string;                  // JLPT level
  vocabulary: VocabularyItem[];
  grammar: GrammarItem[];
  sentenceBreakdown?: SentenceBreakdown;
  relatedSentences?: string[];
}
```

## Các trường hợp sử dụng

### 1. User nhập tiếng Nhật (không cần thay đổi)

**Request:**
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
    "originalSentence": null,
    "isTranslated": false,
    "level": "N5",
    "vocabulary": [...],
    "grammar": [...]
  }
}
```

**FE:** Không cần thay đổi, hiển thị như cũ.

---

### 2. User nhập tiếng Việt (tính năng mới)

**Request:**
```json
{
  "sentence": "Tôi đang học tiếng Nhật",
  "level": "N5"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "sentence": "私は日本語を勉強しています",
    "originalSentence": "Tôi đang học tiếng Nhật",
    "isTranslated": true,
    "level": "N5",
    "vocabulary": [...],
    "grammar": [...]
  }
}
```

**FE:** Có thể hiển thị thêm thông tin:
- Hiển thị câu gốc tiếng Việt
- Hiển thị badge "Đã dịch tự động"
- So sánh câu gốc và câu đã dịch

---

## Cập nhật TypeScript Types

### Cập nhật interface/type definition:

```typescript
// types/sentenceAnalysis.types.ts

export interface SentenceAnalysisResponse {
  sentence: string;
  originalSentence?: string | null;  // ⭐ Thêm field mới
  isTranslated?: boolean;             // ⭐ Thêm field mới
  level: string;
  vocabulary: VocabularyItem[];
  grammar: GrammarItem[];
  sentenceBreakdown?: SentenceBreakdown;
  relatedSentences?: string[];
}

export interface VocabularyItem {
  word: string;
  reading: string;
  meaningVi: string;
  jlptLevel: string;
  importance: string;
  kanjiDetails?: KanjiDetails;
  examples?: string[];
  kanjiVariants?: string[];
}

export interface GrammarItem {
  pattern: string;
  jlptLevel: string;
  explanationVi: string;
  example: string;
  notes?: string;
  examples?: string[];
  confusingPatterns?: ConfusingPattern[];
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

## Cập nhật UI Components

### Option 1: Hiển thị đơn giản (không cần thay đổi nhiều)

Nếu FE chỉ muốn hiển thị câu tiếng Nhật đã phân tích, **không cần thay đổi gì**. Backend tự động xử lý.

```tsx
// Component hiện tại vẫn hoạt động
function SentenceAnalysisResult({ data }: { data: SentenceAnalysisResponse }) {
  return (
    <div>
      <h2>{data.sentence}</h2>
      {/* ... rest of the component */}
    </div>
  );
}
```

---

### Option 2: Hiển thị đầy đủ (khuyến nghị)

Hiển thị cả câu gốc tiếng Việt và câu đã dịch:

```tsx
function SentenceAnalysisResult({ data }: { data: SentenceAnalysisResponse }) {
  return (
    <div className="sentence-analysis-result">
      {/* Hiển thị câu gốc nếu có dịch */}
      {data.isTranslated && data.originalSentence && (
        <div className="original-sentence">
          <span className="label">Câu gốc (Tiếng Việt):</span>
          <span className="text">{data.originalSentence}</span>
          <Badge variant="info">Đã dịch tự động</Badge>
        </div>
      )}
      
      {/* Hiển thị câu tiếng Nhật */}
      <div className="japanese-sentence">
        <span className="label">Câu tiếng Nhật:</span>
        <span className="text">{data.sentence}</span>
      </div>
      
      {/* Rest of the component */}
      <VocabularyList vocabulary={data.vocabulary} />
      <GrammarList grammar={data.grammar} />
      {/* ... */}
    </div>
  );
}
```

---

### Option 3: So sánh câu gốc và câu dịch

```tsx
function SentenceComparison({ data }: { data: SentenceAnalysisResponse }) {
  if (!data.isTranslated || !data.originalSentence) {
    return null;
  }
  
  return (
    <div className="sentence-comparison">
      <div className="comparison-row">
        <div className="original">
          <label>Tiếng Việt:</label>
          <p>{data.originalSentence}</p>
        </div>
        <ArrowRight />
        <div className="translated">
          <label>Tiếng Nhật:</label>
          <p>{data.sentence}</p>
        </div>
      </div>
    </div>
  );
}
```

---

## Cập nhật API Service (nếu cần)

Nếu FE có type checking strict, cần cập nhật:

```typescript
// services/sentenceAnalysisService.ts

export interface SentenceAnalysisRequest {
  sentence: string;  // Có thể là tiếng Nhật hoặc tiếng Việt
  level: string;
}

export interface SentenceAnalysisResponse {
  sentence: string;
  originalSentence?: string | null;  // ⭐ Thêm
  isTranslated?: boolean;             // ⭐ Thêm
  level: string;
  vocabulary: VocabularyItem[];
  grammar: GrammarItem[];
  sentenceBreakdown?: SentenceBreakdown;
  relatedSentences?: string[];
}

export async function analyzeSentence(
  request: SentenceAnalysisRequest
): Promise<SentenceAnalysisResponse> {
  const response = await api.post<ApiResponse<SentenceAnalysisResponse>>(
    '/api/ai/sentence-analysis',
    request
  );
  return response.data.data;
}
```

---

## UX Recommendations

### 1. Thông báo cho user

Khi user nhập tiếng Việt và hệ thống tự động dịch:

```tsx
function SentenceInputForm() {
  const [showTranslationNotice, setShowTranslationNotice] = useState(false);
  
  const handleAnalyze = async (sentence: string, level: string) => {
    const result = await analyzeSentence({ sentence, level });
    
    if (result.isTranslated) {
      setShowTranslationNotice(true);
      // Hiển thị toast: "Đã tự động dịch câu tiếng Việt sang tiếng Nhật"
    }
    
    return result;
  };
  
  return (
    <>
      {showTranslationNotice && (
        <Alert variant="info">
          💡 Câu tiếng Việt đã được tự động dịch sang tiếng Nhật phù hợp với trình độ của bạn.
        </Alert>
      )}
      {/* ... */}
    </>
  );
}
```

### 2. Placeholder text

Cập nhật placeholder để user biết có thể nhập cả tiếng Việt:

```tsx
<Input
  placeholder="Nhập câu tiếng Nhật hoặc tiếng Việt để phân tích..."
  // ...
/>
```

### 3. Validation message

```tsx
const validateSentence = (sentence: string) => {
  if (!sentence.trim()) {
    return "Vui lòng nhập câu";
  }
  if (sentence.length > 50) {
    return "Câu không được quá 50 ký tự";
  }
  return null;
};
```

---

## Testing Checklist

- [ ] Test với câu tiếng Nhật (không dịch)
- [ ] Test với câu tiếng Việt (có dịch)
- [ ] Test với câu hỗn hợp (có cả tiếng Nhật và tiếng Việt)
- [ ] Test với các level khác nhau (N5, N4, N3, N2, N1)
- [ ] Kiểm tra hiển thị `originalSentence` khi `isTranslated = true`
- [ ] Kiểm tra không hiển thị `originalSentence` khi `isTranslated = false`
- [ ] Test error handling khi API fail

---

## Breaking Changes

**KHÔNG CÓ BREAKING CHANGES** - Response mới chỉ thêm fields, không xóa fields cũ.

- ✅ Code FE cũ vẫn hoạt động bình thường
- ✅ Chỉ cần cập nhật nếu muốn hiển thị tính năng mới
- ✅ TypeScript có thể báo warning về missing fields (optional nên không ảnh hưởng runtime)

---

## Migration Guide

### Bước 1: Cập nhật Types (nếu có)

```typescript
// Thêm 2 fields optional vào interface
originalSentence?: string | null;
isTranslated?: boolean;
```

### Bước 2: Cập nhật UI (tùy chọn)

- Nếu muốn hiển thị câu gốc: Thêm component hiển thị `originalSentence`
- Nếu không: Không cần thay đổi gì

### Bước 3: Test

- Test với cả tiếng Nhật và tiếng Việt
- Verify response structure

---

## Example Code

### Full Example Component:

```tsx
import React, { useState } from 'react';
import { analyzeSentence, SentenceAnalysisResponse } from '@/services/sentenceAnalysisService';

function SentenceAnalysisPage() {
  const [sentence, setSentence] = useState('');
  const [level, setLevel] = useState('N5');
  const [result, setResult] = useState<SentenceAnalysisResponse | null>(null);
  const [loading, setLoading] = useState(false);

  const handleAnalyze = async () => {
    setLoading(true);
    try {
      const response = await analyzeSentence({ sentence, level });
      setResult(response);
    } catch (error) {
      console.error('Analysis failed:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <input
        value={sentence}
        onChange={(e) => setSentence(e.target.value)}
        placeholder="Nhập câu tiếng Nhật hoặc tiếng Việt..."
        maxLength={50}
      />
      <select value={level} onChange={(e) => setLevel(e.target.value)}>
        <option value="N5">N5</option>
        <option value="N4">N4</option>
        {/* ... */}
      </select>
      <button onClick={handleAnalyze} disabled={loading}>
        Phân tích
      </button>

      {result && (
        <div className="result">
          {/* Hiển thị câu gốc nếu có dịch */}
          {result.isTranslated && result.originalSentence && (
            <div className="translation-info">
              <p><strong>Câu gốc:</strong> {result.originalSentence}</p>
              <span className="badge">Đã dịch tự động</span>
            </div>
          )}
          
          {/* Hiển thị câu tiếng Nhật */}
          <h2>{result.sentence}</h2>
          
          {/* Vocabulary và Grammar */}
          <VocabularyList vocabulary={result.vocabulary} />
          <GrammarList grammar={result.grammar} />
        </div>
      )}
    </div>
  );
}
```

---

## Summary

### Bắt buộc:
- ✅ **Không có** - Code cũ vẫn hoạt động

### Khuyến nghị:
- ⭐ Cập nhật TypeScript types để tránh warning
- ⭐ Hiển thị `originalSentence` khi `isTranslated = true` để UX tốt hơn
- ⭐ Thêm thông báo cho user biết hệ thống đã tự động dịch

### Tùy chọn:
- 💡 Thêm UI so sánh câu gốc và câu dịch
- 💡 Thêm badge/icon để highlight khi có dịch
- 💡 Cập nhật placeholder text để user biết có thể nhập tiếng Việt

