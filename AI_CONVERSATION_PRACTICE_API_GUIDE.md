# AI Conversation Practice - API Guide cho Frontend

## Tổng quan

Tài liệu này mô tả các endpoint và response structure của tính năng AI Trò chuyện (Conversation Practice). FE cần hiểu cấu trúc này để parse và hiển thị feedback cho user.

---

## Các Endpoint

### 1. POST `/api/ai/conversation/start`
**Mục đích:** Bắt đầu một session luyện trò chuyện mới

**Request:**
```json
{
  "level": "N5",  // N5, N4, N3, N2, N1
  "scenario": "restaurant"  // restaurant, greeting, shopping, etc.
}
```

**Response Structure:**
```json
{
  "success": true,
  "message": "Conversation started",
  "data": {
    "conversationId": "conv-abc123",  // Temporary ID, FE quản lý
    "level": "N5",
    "scenario": "restaurant",
    "aiQuestion": "こんにちは、いらっしゃいませ",  // Câu hỏi tiếng Nhật
    "aiQuestionVi": "Xin chào, chào mừng quý khách",  // Bản dịch tiếng Việt
    "audioUrl": "base64...",  // Audio của câu hỏi (base64)
    "conversationHistory": [
      {
        "role": "ai",
        "text": "こんにちは、いらっしゃいませ",
        "textVi": "Xin chào, chào mừng quý khách"
      }
    ],
    "turnNumber": 1,
    "maxTurns": 7,
    "learningMaterials": {  // Optional
      "vocabularyPreview": ["メニュー", "注文", ...],
      "grammarPoints": ["Điểm ngữ pháp 1", ...],
      "tips": ["Mẹo 1", ...]
    },
    "startingSuggestions": [  // Optional
      "いらっしゃいませ (Xin chào)",
      "すみません (Xin lỗi)",
      ...
    ]
  }
}
```

**Lưu ý:**
- `conversationId` là temporary ID, FE tự quản lý (có thể dùng UUID hoặc timestamp)
- FE cần lưu `conversationHistory` để gửi lại trong các request tiếp theo
- `turnNumber` bắt đầu từ 1, tăng dần mỗi lượt
- `maxTurns` thường là 7 (tối đa 7 lượt trò chuyện)

---

### 2. POST `/api/ai/conversation/respond`
**Mục đích:** User trả lời câu hỏi của AI và nhận câu hỏi tiếp theo

**Request:**
```json
{
  "conversationId": "conv-abc123",
  "conversationHistory": [
    {
      "role": "ai",
      "text": "こんにちは、いらっしゃいませ",
      "textVi": "Xin chào, chào mừng quý khách"
    },
    {
      "role": "user",
      "text": "こんにちは",
      "textVi": "Xin chào"
    }
  ],
  "audioData": "base64...",  // Audio của user (base64)
  "audioFormat": "wav",  // wav, mp3, flac, ogg, webm
  "language": "ja",  // ja (Japanese)
  "level": "N5",  // Optional, mặc định N5
  "scenario": "restaurant"  // Optional, mặc định greeting
}
```

**Response Structure:**
```json
{
  "success": true,
  "message": "Conversation response processed",
  "data": {
    "conversationId": "conv-abc123",
    "level": "N5",
    "scenario": "restaurant",
    "userTranscript": "こんにちは",  // Transcript từ audio của user
    "userTranscriptVi": "Xin chào",  // Bản dịch transcript
    "aiQuestion": "何名様ですか？",  // Câu hỏi tiếp theo của AI
    "aiQuestionVi": "Có bao nhiêu người?",  // Bản dịch
    "audioUrl": "base64...",  // Audio của câu hỏi mới
    "conversationHistory": [
      // Full history bao gồm cả lượt mới
    ],
    "turnNumber": 2,
    "maxTurns": 7,
    "turnFeedback": {  // Optional - feedback cho lượt này
      "isCorrect": true,
      "feedbackVi": "Tốt lắm! Hãy tiếp tục.",
      "suggestionVi": "Có thể thử nói 'いらっしゃいませ' để chào lại."
    }
  }
}
```

**Lưu ý:**
- FE phải gửi lại toàn bộ `conversationHistory` (bao gồm cả các lượt trước)
- `turnNumber` tăng dần mỗi lượt
- `turnFeedback` có thể không có nếu AI không tạo feedback cho lượt này

---

### 3. POST `/api/ai/conversation/end`
**Mục đích:** Kết thúc session và nhận đánh giá tổng thể từ AI

**Request:**
```json
{
  "conversationId": "conv-abc123",
  "conversationHistory": [
    // Toàn bộ lịch sử trò chuyện
  ],
  "level": "N5",  // Optional
  "scenario": "restaurant"  // Optional
}
```

**Response Structure:**
```json
{
  "success": true,
  "message": "Conversation ended",
  "data": {
    "conversationId": "conv-abc123",
    "level": "N5",
    "scenario": "restaurant",
    "fullConversation": [
      // Toàn bộ lịch sử trò chuyện
    ],
    "turnNumber": 3,  // Tổng số lượt đã trò chuyện
    "evaluation": {
      // ⭐ PHẦN QUAN TRỌNG: Đánh giá tổng thể
      "overallScore": 85.0,  // Điểm tổng thể (0-100)
      "accuracyScore": 80.0,  // Độ chính xác (0-100)
      "fluencyScore": 90.0,  // Độ trôi chảy (0-100)
      "grammarScore": 75.0,  // Ngữ pháp (0-100)
      "vocabularyScore": 85.0,  // Từ vựng (0-100)
      
      // ⭐ TÓM TẮT NGẮN GỌN (MỚI)
      "summaryVi": "Bạn đã hoàn thành tốt cuộc trò chuyện với điểm tổng thể 85/100. Điểm mạnh là phát âm rõ ràng và sử dụng từ vựng phù hợp, nhưng cần cải thiện ngữ pháp và cách sử dụng trợ từ.",
      
      // ⭐ NHẬN XÉT TỔNG QUAN CHI TIẾT
      "overallFeedbackVi": "Cuộc trò chuyện đã hoàn thành. Bạn đã thể hiện sự tham gia tích cực và cố gắng giao tiếp. Phát âm của bạn khá rõ ràng và tự nhiên. Tuy nhiên, vẫn còn một số điểm cần cải thiện về ngữ pháp, đặc biệt là cách sử dụng trợ từ. Hãy tiếp tục luyện tập để nâng cao trình độ!",
      
      // ĐIỂM MẠNH (3-5 điểm)
      "strengthsVi": [
        "Sử dụng đúng trợ từ を trong câu 'りんごを食べます'",
        "Phát âm rõ ràng các từ khó như 'ありがとうございます'",
        "Phản ứng nhanh và tự nhiên trong cuộc trò chuyện",
        "Sử dụng từ vựng phù hợp với tình huống nhà hàng"
      ],
      
      // ĐIỂM CẦN CẢI THIỆN (3-5 điểm)
      "improvementsVi": [
        "Lỗi chia động từ: '食べる' nên là '食べます' trong ngữ cảnh lịch sự",
        "Thiếu trợ từ に khi nói về địa điểm: '学校行きます' nên là '学校に行きます'",
        "Cần chú ý hơn đến cách sử dụng trợ từ で và に",
        "Một số từ vựng có thể được thay thế bằng cách diễn đạt tự nhiên hơn"
      ],
      
      // GỢI Ý CẢI THIỆN (3-5 điểm)
      "suggestionsVi": [
        "Luyện tập thêm cách sử dụng trợ từ に và で để phân biệt địa điểm và phương tiện",
        "Học thêm từ vựng về chủ đề nhà hàng như 'メニュー', '注文', 'お会計'",
        "Thực hành thêm các mẫu câu lịch sự trong ngữ cảnh nhà hàng",
        "Luyện tập phát âm các từ dài và khó hơn"
      ],
      
      // PHÂN TÍCH CHI TIẾT TỪNG LƯỢT
      "detailedAnalysisVi": [
        {
          "turn": 1,  // Số lượt
          "userResponse": "こんにちは",  // Câu trả lời của user
          "errors": [
            "Thiếu trợ từ に",
            "Chia động từ chưa đúng"
          ],
          "corrections": [
            "Thêm trợ từ に: '学校に行きます'",
            "Chia động từ: '行く' → '行きます'"
          ],
          "betterResponse": "学校に行きます"  // Câu trả lời tốt hơn
        },
        {
          "turn": 2,
          "userResponse": "ありがとうございます",
          "errors": [],
          "corrections": [],
          "betterResponse": "ありがとうございます"  // Đã đúng
        }
        // ... các lượt khác
      ]
    }
  }
}
```

---

## Cấu trúc Evaluation Response (Quan trọng)

### Các Field trong `evaluation`:

1. **Điểm số (Scores):**
   - `overallScore`: Điểm tổng thể (0-100)
   - `accuracyScore`: Độ chính xác (0-100)
   - `fluencyScore`: Độ trôi chảy (0-100)
   - `grammarScore`: Ngữ pháp (0-100)
   - `vocabularyScore`: Từ vựng (0-100)

2. **Tóm tắt và Feedback:**
   - `summaryVi`: **Tóm tắt ngắn gọn 1-2 câu** - Giúp user nắm được tình hình ngay lập tức
   - `overallFeedbackVi`: **Nhận xét tổng quan chi tiết 3-4 câu** - Giải thích rõ về điểm số và tình hình tổng thể

3. **Phân tích:**
   - `strengthsVi`: Mảng các điểm mạnh (3-5 điểm) với ví dụ cụ thể
   - `improvementsVi`: Mảng các điểm cần cải thiện (3-5 điểm) với ví dụ cụ thể
   - `suggestionsVi`: Mảng các gợi ý cải thiện (3-5 điểm) thực tế, có thể áp dụng ngay

4. **Phân tích chi tiết:**
   - `detailedAnalysisVi`: Mảng các object phân tích từng lượt trả lời
     - `turn`: Số lượt
     - `userResponse`: Câu trả lời của user
     - `errors`: Mảng các lỗi cụ thể
     - `corrections`: Mảng các cách sửa
     - `betterResponse`: Câu trả lời tốt hơn

---

## Gợi ý hiển thị cho FE

### 1. **Tóm tắt nhanh (Summary):**
- Hiển thị `summaryVi` ở đầu trang kết quả
- Có thể kèm theo `overallScore` với màu sắc phù hợp (xanh nếu >80, vàng nếu 60-80, đỏ nếu <60)

### 2. **Điểm số (Scores):**
- Hiển thị dạng progress bar hoặc circular progress
- Có thể group thành: Overall, Accuracy, Fluency, Grammar, Vocabulary

### 3. **Feedback tổng quan:**
- Hiển thị `overallFeedbackVi` với format dễ đọc (paragraph)

### 4. **Điểm mạnh và cần cải thiện:**
- Hiển thị `strengthsVi` và `improvementsVi` dạng list với icon phù hợp
- Mỗi item có thể có ví dụ cụ thể từ cuộc trò chuyện

### 5. **Gợi ý:**
- Hiển thị `suggestionsVi` dạng actionable items
- Có thể thêm button "Luyện tập ngay" cho mỗi gợi ý

### 6. **Phân tích chi tiết:**
- Hiển thị `detailedAnalysisVi` dạng accordion hoặc tabs theo từng lượt
- Mỗi lượt hiển thị:
  - Câu trả lời của user
  - Các lỗi (nếu có)
  - Cách sửa
  - Câu trả lời tốt hơn

---

## Lưu ý quan trọng

1. **JSON Response:** Tất cả response đều là JSON hợp lệ, FE có thể parse trực tiếp bằng `JSON.parse()` hoặc thư viện tương ứng

2. **Error Handling:** 
   - Nếu `success: false`, kiểm tra `message` để hiển thị lỗi
   - Các lỗi phổ biến:
     - Quota hết: "You have used all available AI requests..."
     - Audio không hợp lệ: "Invalid audio format..."
     - Service không available: "Conversation practice service is not available"

3. **Conversation History:**
   - FE phải quản lý và gửi lại toàn bộ history trong mỗi request
   - Format: `[{"role": "ai"|"user", "text": "...", "textVi": "..."}]`

4. **Quota Management:**
   - Mỗi lượt trò chuyện (start, respond) sẽ trừ 1 request từ quota
   - End conversation không trừ quota
   - FE nên check quota trước khi bắt đầu conversation

5. **Audio Format:**
   - Chấp nhận: wav, mp3, flac, ogg, webm
   - Gửi dạng base64 string
   - Audio phải có độ dài hợp lệ (không quá ngắn)

---

## Flow hoàn chỉnh

1. **Start:** User chọn level và scenario → Gọi `/start` → Nhận câu hỏi đầu tiên
2. **Respond:** User ghi âm và gửi → Gọi `/respond` → Nhận câu hỏi tiếp theo
3. **Repeat:** Lặp lại bước 2 cho đến khi đủ lượt hoặc user muốn kết thúc
4. **End:** Gọi `/end` với full history → Nhận evaluation và feedback chi tiết

---

## Response Format chuẩn

Tất cả response đều theo format:
```json
{
  "success": true|false,
  "message": "Message string",
  "data": {
    // Data object
  }
}
```

FE nên check `success` trước khi xử lý `data`.

---

**Lưu ý cuối:** File này chỉ mô tả cấu trúc API và response. FE tự implement UI/UX để hiển thị các thông tin này một cách thân thiện và dễ hiểu cho user.

