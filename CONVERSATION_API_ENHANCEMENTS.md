# Conversation Practice API - Các Cải Tiến Mới

## Tổng Quan

API Conversation Practice đã được cải thiện với các field mới để hỗ trợ học tập tốt hơn. Tất cả các endpoint giữ nguyên, chỉ thêm các field mới vào response.

---

## 📋 Các Field Mới Đã Thêm

### 1. `/api/ai/conversation/start` - Bắt Đầu Conversation

#### Response Structure (Cập Nhật)

```json
{
  "success": true,
  "message": "Conversation started",
  "data": {
    // Các field cũ (giữ nguyên)
    "conversationId": "conv-abc123",
    "level": "N5",
    "scenario": "restaurant",
    "originalScenario": "restaurant",
    "aiQuestion": "こんにちは、いらっしゃいませ",
    "aiQuestionVi": "Xin chào, chào mừng quý khách",
    "audioUrl": "base64...",
    "audioFormat": "wav",
    "conversationHistory": [...],
    "turnNumber": 1,
    "maxTurns": 7,
    
    // ✨ CÁC FIELD MỚI
    "scenarioDescription": "nhà hàng (ordering food, asking about menu)",
    "vocabularyPreview": [
      "メニュー",
      "注文",
      "おすすめ",
      "お会計",
      "いただきます"
    ],
    "grammarPoints": [
      "Cách nói lịch sự です/ます",
      "Câu hỏi với か",
      "Từ chỉ định これ/それ/あれ"
    ],
    "tips": [
      "Hãy lắng nghe kỹ câu hỏi trước khi trả lời",
      "Sử dụng cách nói lịch sự trong các tình huống trang trọng",
      "Nhớ nói \"いただきます\" trước khi ăn",
      "Khi gọi món, có thể dùng \"お願いします\""
    ]
  },
  "timestamp": "2025-12-13T..."
}
```

#### Field Mới Chi Tiết

| Field | Type | Mô Tả | Ví Dụ |
|-------|------|-------|-------|
| `scenarioDescription` | `string` | Mô tả chi tiết về tình huống sẽ luyện tập | "nhà hàng (ordering food, asking about menu)" |
| `vocabularyPreview` | `string[]` | Danh sách 5-8 từ vựng quan trọng sẽ dùng trong conversation (tiếng Nhật) | `["メニュー", "注文", "おすすめ"]` |
| `grammarPoints` | `string[]` | Danh sách 3-5 điểm ngữ pháp sẽ luyện tập (mô tả bằng tiếng Việt) | `["Cách nói lịch sự です/ます", "Câu hỏi với か"]` |
| `tips` | `string[]` | Danh sách 3-5 mẹo hữu ích để thành công trong tình huống này (tiếng Việt) | `["Hãy lắng nghe kỹ câu hỏi", "Nhớ nói いただきます"]` |

---

### 2. `/api/ai/conversation/respond` - Trả Lời và Nhận Câu Hỏi Tiếp Theo

#### Response Structure (Cập Nhật)

```json
{
  "success": true,
  "message": "Conversation response processed",
  "data": {
    // Các field cũ (giữ nguyên)
    "conversationId": "conv-abc123",
    "userTranscript": "こんにちは",
    "userTranscriptVi": "Xin chào",
    "confidence": 0.95,
    "aiNextQuestion": "いらっしゃいませ、何名様ですか",
    "aiNextQuestionVi": "Chào mừng quý khách, có mấy người ạ?",
    "audioUrl": "base64...",
    "audioFormat": "wav",
    "conversationHistory": [...],
    "turnNumber": 2,
    "maxTurns": 7,
    "isEnding": false,
    
    // ✨ FIELD MỚI
    "turnFeedback": {
      "isCorrect": true,
      "feedbackVi": "Tốt lắm! Câu trả lời phù hợp với tình huống.",
      "suggestionVi": "Có thể thêm từ \"お願いします\" để lịch sự hơn"
    }
  },
  "timestamp": "2025-12-13T..."
}
```

#### Field Mới Chi Tiết

| Field | Type | Mô Tả | Ví Dụ |
|-------|------|-------|-------|
| `turnFeedback` | `object` | Feedback sau mỗi turn trả lời của user | Xem structure bên dưới |

##### `turnFeedback` Object Structure

```typescript
{
  isCorrect: boolean;        // Câu trả lời có đúng/phù hợp không
  feedbackVi: string;        // Nhận xét ngắn gọn (tiếng Việt)
  suggestionVi: string;      // Gợi ý cải thiện (tiếng Việt, có thể rỗng)
}
```

**Ví dụ:**
```json
{
  "isCorrect": true,
  "feedbackVi": "Tốt lắm! Câu trả lời phù hợp với tình huống.",
  "suggestionVi": "Có thể thêm từ \"お願いします\" để lịch sự hơn"
}
```

---

### 3. `/api/ai/conversation/end` - Kết Thúc và Nhận Evaluation

#### Response Structure (Không Thay Đổi)

Endpoint này giữ nguyên structure, chỉ có `evaluation` object đã được cải thiện từ trước với `detailedAnalysisVi`.

---

## 💻 Hướng Dẫn Sử Dụng Cho FE

### TypeScript Interface (Gợi Ý)

```typescript
// Conversation Start Response
interface ConversationStartResponse {
  success: boolean;
  message: string;
  data: {
    conversationId: string;
    level: string;
    scenario: string;
    originalScenario: string;
    aiQuestion: string;
    aiQuestionVi: string;
    audioUrl: string;
    audioFormat: string;
    conversationHistory: ConversationMessage[];
    turnNumber: number;
    maxTurns: number;
    
    // New fields
    scenarioDescription: string;
    vocabularyPreview: string[];
    grammarPoints: string[];
    tips: string[];
  };
  timestamp: string;
}

// Conversation Respond Response
interface ConversationRespondResponse {
  success: boolean;
  message: string;
  data: {
    conversationId: string;
    userTranscript: string;
    userTranscriptVi: string;
    confidence: number;
    aiNextQuestion: string;
    aiNextQuestionVi: string;
    audioUrl: string;
    audioFormat: string;
    conversationHistory: ConversationMessage[];
    turnNumber: number;
    maxTurns: number;
    isEnding: boolean;
    
    // New field
    turnFeedback: {
      isCorrect: boolean;
      feedbackVi: string;
      suggestionVi: string;
    };
  };
  timestamp: string;
}

interface ConversationMessage {
  role: "ai" | "user";
  text: string;
  textVi: string;
}
```

### Ví Dụ Code Sử Dụng

#### 1. Hiển Thị Learning Materials Khi Bắt Đầu

```typescript
// Khi nhận response từ /conversation/start
const startConversation = async (level: string, scenario: string) => {
  const response = await api.post('/api/ai/conversation/start', {
    level,
    scenario
  });
  
  if (response.data.success) {
    const data = response.data.data;
    
    // Hiển thị learning materials
    displayScenarioDescription(data.scenarioDescription);
    displayVocabularyPreview(data.vocabularyPreview);
    displayGrammarPoints(data.grammarPoints);
    displayTips(data.tips);
    
    // Xử lý conversation như bình thường
    startConversationFlow(data);
  }
};
```

#### 2. Hiển Thị Turn Feedback Sau Mỗi Lần Trả Lời

```typescript
// Khi nhận response từ /conversation/respond
const respondToConversation = async (audioData: string, history: ConversationMessage[]) => {
  const response = await api.post('/api/ai/conversation/respond', {
    conversationId: currentConversationId,
    conversationHistory: history,
    audioData: audioData,
    audioFormat: 'wav',
    language: 'ja-JP',
    level: currentLevel,
    scenario: currentScenario
  });
  
  if (response.data.success) {
    const data = response.data.data;
    
    // Hiển thị turn feedback
    if (data.turnFeedback) {
      showFeedback({
        isCorrect: data.turnFeedback.isCorrect,
        message: data.turnFeedback.feedbackVi,
        suggestion: data.turnFeedback.suggestionVi
      });
    }
    
    // Xử lý conversation tiếp tục
    continueConversation(data);
  }
};
```

### UI/UX Gợi Ý

#### 1. Learning Materials Panel (Khi Start)

```
┌─────────────────────────────────────┐
│ 📚 Tài Liệu Học Tập                 │
├─────────────────────────────────────┤
│ 📖 Tình Huống: nhà hàng             │
│    (ordering food, asking menu)     │
│                                     │
│ 📝 Từ Vựng Quan Trọng:              │
│    • メニュー (menu)                 │
│    • 注文 (order)                    │
│    • おすすめ (recommendation)       │
│                                     │
│ 📖 Điểm Ngữ Pháp:                   │
│    • Cách nói lịch sự です/ます      │
│    • Câu hỏi với か                  │
│                                     │
│ 💡 Mẹo:                             │
│    • Hãy lắng nghe kỹ câu hỏi       │
│    • Nhớ nói いただきます            │
└─────────────────────────────────────┘
```

#### 2. Turn Feedback Badge (Sau Mỗi Turn)

```
┌─────────────────────────────────────┐
│ ✅ Tốt lắm!                         │
│ Câu trả lời phù hợp với tình huống. │
│                                     │
│ 💡 Gợi ý:                          │
│ Có thể thêm từ "お願いします" để     │
│ lịch sự hơn                         │
└─────────────────────────────────────┘
```

Hoặc với icon:
- ✅ `isCorrect: true` → Màu xanh, icon check
- ⚠️ `isCorrect: false` → Màu vàng, icon warning
- 💡 `suggestionVi` → Hiển thị gợi ý nếu có

---

## 🔄 Backward Compatibility

- ✅ Tất cả các field cũ vẫn giữ nguyên
- ✅ Các field mới là **optional** - FE có thể check `if (data.vocabularyPreview)` trước khi dùng
- ✅ Nếu Gemini API fail, vẫn có fallback data → luôn có giá trị
- ✅ Không breaking changes - FE cũ vẫn hoạt động bình thường

---

## 📝 Lưu Ý

1. **Field Mới Là Optional**: Luôn check field tồn tại trước khi dùng
   ```typescript
   if (data.vocabularyPreview && data.vocabularyPreview.length > 0) {
     // Hiển thị vocabulary
   }
   ```

2. **Fallback Data**: Nếu Gemini API fail, backend sẽ trả về static data dựa trên scenario → luôn có data

3. **Performance**: Các field mới được generate một lần khi start conversation, không ảnh hưởng performance

4. **Turn Feedback**: Được generate sau mỗi turn, có thể mất thời gian nhỏ (~1-2s) nhưng không block conversation flow

---

## 🎯 Use Cases

### Use Case 1: Hiển Thị Learning Materials Trước Khi Bắt Đầu
- User chọn level và scenario
- FE gọi `/conversation/start`
- Hiển thị panel với vocabulary, grammar, tips
- User có thể xem trước trước khi bắt đầu conversation

### Use Case 2: Real-time Feedback Sau Mỗi Turn
- User trả lời câu hỏi của AI
- FE gọi `/conversation/respond`
- Hiển thị feedback badge với nhận xét và gợi ý
- User biết ngay câu trả lời của mình tốt hay cần cải thiện

### Use Case 3: Progressive Learning
- FE có thể lưu vocabulary và grammar points
- Tạo flashcards từ vocabulary preview
- Hiển thị tips trong suốt conversation để nhắc nhở user

---

## 📞 Support

Nếu có vấn đề hoặc câu hỏi về các field mới, vui lòng liên hệ backend team.

**Last Updated:** 2025-12-13

