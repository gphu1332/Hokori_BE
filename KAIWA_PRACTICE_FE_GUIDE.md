# Hướng Dẫn Tích Hợp Kaiwa Practice API cho Frontend (React Vite)

## 📋 Mục Lục
1. [Tổng Quan](#tổng-quan)
2. [Cài Đặt Dependencies](#cài-đặt-dependencies)
3. [Record Audio trong React](#record-audio-trong-react)
4. [Format Audio (Base64 Encoding)](#format-audio-base64-encoding)
5. [API Endpoints](#api-endpoints)
6. [Code Examples](#code-examples)
7. [Xử Lý Response](#xử-lý-response)
8. [Error Handling](#error-handling)
9. [Best Practices](#best-practices)

---

## 🎯 Tổng Quan

Kaiwa Practice là tính năng luyện nói tiếng Nhật giống Elsa Speak, cho phép:
- User chọn trình độ JLPT (N5-N1)
- Record audio phát âm câu tiếng Nhật
- Nhận feedback về độ chính xác và phát âm
- So sánh với câu mẫu

**Luồng hoạt động:**
1. User chọn level (N5-N1)
2. Lấy câu mẫu từ API (hoặc tự nhập)
3. Record audio
4. Convert audio sang Base64
5. Gửi lên BE để phân tích
6. Nhận kết quả và hiển thị

---

## 📦 Cài Đặt Dependencies

### 1. Install các package cần thiết:

```bash
npm install axios
# hoặc
yarn add axios
```

### 2. Các API Browser cần thiết (built-in, không cần install):
- `navigator.mediaDevices.getUserMedia()` - Record audio
- `MediaRecorder API` - Xử lý audio recording
- `FileReader API` - Convert audio sang Base64

---

## 🎤 Record Audio trong React

### Hook để Record Audio:

```typescript
// hooks/useAudioRecorder.ts
import { useState, useRef, useCallback } from 'react';

interface UseAudioRecorderReturn {
  isRecording: boolean;
  audioBlob: Blob | null;
  startRecording: () => Promise<void>;
  stopRecording: () => void;
  resetRecording: () => void;
  error: string | null;
}

export const useAudioRecorder = (): UseAudioRecorderReturn => {
  const [isRecording, setIsRecording] = useState(false);
  const [audioBlob, setAudioBlob] = useState<Blob | null>(null);
  const [error, setError] = useState<string | null>(null);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const chunksRef = useRef<Blob[]>([]);

  const startRecording = useCallback(async () => {
    try {
      setError(null);
      
      // Request microphone permission
      const stream = await navigator.mediaDevices.getUserMedia({ 
        audio: {
          channelCount: 1,        // Mono
          sampleRate: 16000,      // 16kHz (recommended for Japanese)
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true
        } 
      });

      // Create MediaRecorder with WAV format
      const mimeType = 'audio/webm;codecs=opus'; // Fallback to webm
      const options: MediaRecorderOptions = {
        mimeType: mimeType,
        audioBitsPerSecond: 128000
      };

      const mediaRecorder = new MediaRecorder(stream, options);
      mediaRecorderRef.current = mediaRecorder;
      chunksRef.current = [];

      mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          chunksRef.current.push(event.data);
        }
      };

      mediaRecorder.onstop = () => {
        const blob = new Blob(chunksRef.current, { type: 'audio/webm' });
        setAudioBlob(blob);
        
        // Stop all tracks to release microphone
        stream.getTracks().forEach(track => track.stop());
      };

      mediaRecorder.onerror = (event) => {
        setError('Recording error occurred');
        console.error('MediaRecorder error:', event);
      };

      mediaRecorder.start();
      setIsRecording(true);
    } catch (err) {
      console.error('Error starting recording:', err);
      setError('Failed to access microphone. Please check permissions.');
    }
  }, []);

  const stopRecording = useCallback(() => {
    if (mediaRecorderRef.current && isRecording) {
      mediaRecorderRef.current.stop();
      setIsRecording(false);
    }
  }, [isRecording]);

  const resetRecording = useCallback(() => {
    setAudioBlob(null);
    setError(null);
    chunksRef.current = [];
  }, []);

  return {
    isRecording,
    audioBlob,
    startRecording,
    stopRecording,
    resetRecording,
    error
  };
};
```

---

## 🔄 Format Audio (Base64 Encoding)

### Convert Audio Blob sang Base64:

```typescript
// utils/audioUtils.ts

/**
 * Convert audio Blob sang Base64 string
 * @param audioBlob - Audio Blob từ MediaRecorder
 * @returns Promise<string> - Base64 encoded string
 */
export const convertBlobToBase64 = (audioBlob: Blob): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    
    reader.onloadend = () => {
      if (typeof reader.result === 'string') {
        // Remove data URL prefix (data:audio/webm;base64,)
        const base64String = reader.result.split(',')[1];
        resolve(base64String);
      } else {
        reject(new Error('Failed to convert blob to base64'));
      }
    };
    
    reader.onerror = () => {
      reject(new Error('Error reading audio file'));
    };
    
    reader.readAsDataURL(audioBlob);
  });
};

/**
 * Convert audio Blob sang WAV format (nếu cần)
 * Note: Browser thường record WebM/Opus, BE sẽ tự convert
 */
export const getAudioFormat = (blob: Blob): string => {
  if (blob.type.includes('webm')) {
    return 'webm'; // BE sẽ convert sang wav
  }
  if (blob.type.includes('wav')) {
    return 'wav';
  }
  if (blob.type.includes('mp3')) {
    return 'mp3';
  }
  return 'wav'; // Default
};

/**
 * Validate audio size (max 10MB)
 */
export const validateAudioSize = (blob: Blob): boolean => {
  const maxSize = 10 * 1024 * 1024; // 10MB
  return blob.size <= maxSize;
};
```

---

## 🌐 API Endpoints

### Base URL:
```
Production: https://your-api-domain.com/api/ai
Development: http://localhost:8080/api/ai
```

### 1. Lấy câu mẫu theo Level
```typescript
GET /api/ai/kaiwa-sentences/{level}
// level: N5, N4, N3, N2, N1

Response:
{
  "success": true,
  "message": "Suggested sentences for N5",
  "data": {
    "level": "N5",
    "sentences": [
      {
        "text": "こんにちは",
        "translation": "Xin chào",
        "difficulty": "easy"
      },
      // ...
    ],
    "count": 10
  }
}
```

### 2. Lấy câu ngẫu nhiên
```typescript
GET /api/ai/kaiwa-sentences/{level}/random

Response:
{
  "success": true,
  "message": "Random sentence for N5",
  "data": {
    "text": "私は日本語を勉強しています",
    "translation": "Tôi đang học tiếng Nhật",
    "difficulty": "medium"
  }
}
```

### 3. Lấy recommendations cho level
```typescript
GET /api/ai/kaiwa-recommendations/{level}

Response:
{
  "success": true,
  "message": "Kaiwa recommendations for N5",
  "data": {
    "level": "N5",
    "recommendedSpeed": "normal",
    "recommendedSpeakingRate": 1.0,
    "levelInfo": {
      "level": "N5",
      "description": "Recommended settings for N5 level practice"
    }
  }
}
```

### 4. **Kaiwa Practice (Main API)**
```typescript
POST /api/ai/kaiwa-practice

Request Body:
{
  "targetText": "私は日本語を勉強しています",  // Required
  "audioData": "UklGRiQAAABXQVZFZm10IBAAAAAB...", // Required (Base64)
  "level": "N5",                              // Optional (default: N5)
  "language": "ja-JP",                       // Optional (default: ja-JP)
  "audioFormat": "wav"                       // Optional (default: wav)
}

Response:
{
  "success": true,
  "message": "Kaiwa practice completed",
  "data": {
    "targetText": "私は日本語を勉強しています",
    "userTranscript": "私は日本語を勉強しています",
    "level": "N5",
    "accuracyScore": 0.95,           // 0-1 (95%)
    "pronunciationScore": 0.88,       // 0-1 (88%)
    "overallScore": 0.92,             // 0-1 (92%)
    "confidence": 0.88,               // Speech recognition confidence
    "isAccurate": true,               // accuracyScore >= threshold
    "needsPractice": false,           // overallScore < threshold
    "feedback": {
      "overallFeedbackVi": "Phát âm xuất sắc! Bạn đã nói rất chính xác.",
      "accuracyFeedbackVi": "Độ chính xác: 95%. Bạn đã phát âm đúng câu.",
      "pronunciationFeedbackVi": "Phát âm tốt! (độ chính xác: 88%)",
      "suggestionsVi": "Tiếp tục luyện tập để cải thiện phát âm.",
      "pronunciationTips": [
        "Chú ý phát âm rõ ràng các từ",
        "Luyện tập ngữ điệu tự nhiên"
      ],
      "levelInfo": {
        "level": "N5",
        "levelNameVi": "Sơ cấp",
        "descriptionVi": "Trình độ cơ bản nhất"
      }
    },
    "recommendations": {
      "recommendedSpeed": "normal",
      "recommendedSpeakingRate": 1.0,
      "tolerance": 0.15,
      "accuracyThreshold": 0.75,
      "practiceThreshold": 0.7
    }
  }
}
```

---

## 💻 Code Examples

### 1. Service để gọi API:

```typescript
// services/kaiwaService.ts
import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/ai';

interface KaiwaPracticeRequest {
  targetText: string;
  audioData: string;        // Base64 string
  level?: string;            // N5, N4, N3, N2, N1
  language?: string;         // ja-JP (default)
  audioFormat?: string;      // wav, mp3, flac, ogg
}

interface KaiwaPracticeResponse {
  success: boolean;
  message: string;
  data: {
    targetText: string;
    userTranscript: string;
    level: string;
    accuracyScore: number;
    pronunciationScore: number;
    overallScore: number;
    confidence: number;
    isAccurate: boolean;
    needsPractice: boolean;
    feedback: {
      overallFeedbackVi: string;
      accuracyFeedbackVi: string;
      pronunciationFeedbackVi: string;
      suggestionsVi: string;
      pronunciationTips: string[];
      levelInfo: {
        level: string;
        levelNameVi: string;
        descriptionVi: string;
      };
    };
    recommendations: {
      recommendedSpeed: string;
      recommendedSpeakingRate: number;
      tolerance: number;
      accuracyThreshold: number;
      practiceThreshold: number;
    };
  };
}

export const kaiwaService = {
  /**
   * Lấy danh sách câu mẫu theo level
   */
  async getSuggestedSentences(level: string): Promise<any> {
    const response = await axios.get(`${API_BASE_URL}/kaiwa-sentences/${level}`);
    return response.data;
  },

  /**
   * Lấy câu ngẫu nhiên
   */
  async getRandomSentence(level: string): Promise<any> {
    const response = await axios.get(`${API_BASE_URL}/kaiwa-sentences/${level}/random`);
    return response.data;
  },

  /**
   * Lấy recommendations cho level
   */
  async getRecommendations(level: string): Promise<any> {
    const response = await axios.get(`${API_BASE_URL}/kaiwa-recommendations/${level}`);
    return response.data;
  },

  /**
   * Gửi audio để practice
   */
  async practiceKaiwa(request: KaiwaPracticeRequest): Promise<KaiwaPracticeResponse> {
    const response = await axios.post(
      `${API_BASE_URL}/kaiwa-practice`,
      request,
      {
        headers: {
          'Content-Type': 'application/json',
        },
        timeout: 30000, // 30 seconds timeout
      }
    );
    return response.data;
  },
};
```

### 2. Component sử dụng:

```typescript
// components/KaiwaPractice.tsx
import { useState } from 'react';
import { useAudioRecorder } from '../hooks/useAudioRecorder';
import { convertBlobToBase64, validateAudioSize } from '../utils/audioUtils';
import { kaiwaService } from '../services/kaiwaService';

export const KaiwaPractice = () => {
  const [level, setLevel] = useState<string>('N5');
  const [targetText, setTargetText] = useState<string>('');
  const [result, setResult] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const {
    isRecording,
    audioBlob,
    startRecording,
    stopRecording,
    resetRecording,
    error: recordingError,
  } = useAudioRecorder();

  // Lấy câu ngẫu nhiên
  const loadRandomSentence = async () => {
    try {
      setLoading(true);
      const response = await kaiwaService.getRandomSentence(level);
      if (response.success) {
        setTargetText(response.data.text);
        setResult(null);
      }
    } catch (err: any) {
      setError(err.message || 'Failed to load sentence');
    } finally {
      setLoading(false);
    }
  };

  // Submit audio để practice
  const handlePractice = async () => {
    if (!audioBlob) {
      setError('Please record audio first');
      return;
    }

    if (!targetText.trim()) {
      setError('Please enter or select a target sentence');
      return;
    }

    // Validate audio size
    if (!validateAudioSize(audioBlob)) {
      setError('Audio file is too large (max 10MB)');
      return;
    }

    try {
      setLoading(true);
      setError(null);

      // Convert audio to Base64
      const base64Audio = await convertBlobToBase64(audioBlob);

      // Call API
      const response = await kaiwaService.practiceKaiwa({
        targetText: targetText,
        audioData: base64Audio,
        level: level,
        language: 'ja-JP',
        audioFormat: 'wav', // BE sẽ tự convert nếu cần
      });

      if (response.success) {
        setResult(response.data);
      } else {
        setError(response.message || 'Practice failed');
      }
    } catch (err: any) {
      console.error('Practice error:', err);
      setError(
        err.response?.data?.message || 
        err.message || 
        'Failed to process practice. Please try again.'
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      {/* Level Selection */}
      <select 
        value={level} 
        onChange={(e) => {
          setLevel(e.target.value);
          setResult(null);
          setTargetText('');
        }}
      >
        <option value="N5">N5 - Sơ cấp</option>
        <option value="N4">N4 - Sơ trung cấp</option>
        <option value="N3">N3 - Trung cấp</option>
        <option value="N2">N2 - Trung cao cấp</option>
        <option value="N1">N1 - Cao cấp</option>
      </select>

      {/* Load Random Sentence */}
      <button onClick={loadRandomSentence} disabled={loading}>
        {loading ? 'Loading...' : 'Lấy câu ngẫu nhiên'}
      </button>

      {/* Target Text Input */}
      <textarea
        value={targetText}
        onChange={(e) => setTargetText(e.target.value)}
        placeholder="Nhập hoặc chọn câu tiếng Nhật để luyện tập"
      />

      {/* Recording Controls */}
      <div>
        {!isRecording ? (
          <button onClick={startRecording}>
            Bắt đầu ghi âm
          </button>
        ) : (
          <button onClick={stopRecording}>
            Dừng ghi âm
          </button>
        )}
        
        {audioBlob && (
          <>
            <audio src={URL.createObjectURL(audioBlob)} controls />
            <button onClick={resetRecording}>Xóa</button>
          </>
        )}
      </div>

      {/* Practice Button */}
      <button 
        onClick={handlePractice} 
        disabled={!audioBlob || !targetText || loading}
      >
        {loading ? 'Đang xử lý...' : 'Gửi để luyện tập'}
      </button>

      {/* Error Display */}
      {(error || recordingError) && (
        <div style={{ color: 'red' }}>
          {error || recordingError}
        </div>
      )}

      {/* Results Display */}
      {result && (
        <div>
          <h3>Kết quả:</h3>
          <p>Độ chính xác: {(result.accuracyScore * 100).toFixed(1)}%</p>
          <p>Phát âm: {(result.pronunciationScore * 100).toFixed(1)}%</p>
          <p>Tổng điểm: {(result.overallScore * 100).toFixed(1)}%</p>
          
          <div>
            <h4>Feedback:</h4>
            <p>{result.feedback.overallFeedbackVi}</p>
            <p>{result.feedback.accuracyFeedbackVi}</p>
            <p>{result.feedback.pronunciationFeedbackVi}</p>
            
            {result.feedback.pronunciationTips.length > 0 && (
              <ul>
                {result.feedback.pronunciationTips.map((tip: string, index: number) => (
                  <li key={index}>{tip}</li>
                ))}
              </ul>
            )}
          </div>

          <div>
            <p>Bạn đã nói: {result.userTranscript}</p>
            <p>Câu mẫu: {result.targetText}</p>
          </div>
        </div>
      )}
    </div>
  );
};
```

---

## 📥 Xử Lý Response

### Response Structure:

```typescript
interface KaiwaPracticeResult {
  // Scores (0-1, multiply by 100 for percentage)
  accuracyScore: number;        // Độ chính xác so với câu mẫu
  pronunciationScore: number;  // Độ chính xác phát âm
  overallScore: number;        // Điểm tổng hợp
  
  // Flags
  isAccurate: boolean;         // accuracyScore >= threshold
  needsPractice: boolean;      // overallScore < threshold
  
  // Transcripts
  targetText: string;          // Câu mẫu
  userTranscript: string;      // Câu user đã nói
  
  // Feedback (tiếng Việt)
  feedback: {
    overallFeedbackVi: string;
    accuracyFeedbackVi: string;
    pronunciationFeedbackVi: string;
    suggestionsVi: string;
    pronunciationTips: string[];
  };
}
```

### Cách hiển thị scores:

```typescript
// Convert score (0-1) to percentage
const scoreToPercentage = (score: number): number => {
  return Math.round(score * 100);
};

// Example: result.overallScore = 0.92 → 92%
const percentage = scoreToPercentage(result.overallScore);
```

---

## ⚠️ Error Handling

### Common Errors:

```typescript
// 1. Invalid audio format
if (response.message.includes('Invalid audio format')) {
  // Show: "Định dạng audio không hợp lệ. Vui lòng thử lại."
}

// 2. Invalid JLPT level
if (response.message.includes('Invalid JLPT level')) {
  // Show: "Trình độ không hợp lệ. Vui lòng chọn N5-N1."
}

// 3. Audio too large
if (response.message.includes('exceed 10MB')) {
  // Show: "File audio quá lớn (tối đa 10MB). Vui lòng ghi âm ngắn hơn."
}

// 4. Transcription failed
if (response.message.includes('Could not transcribe')) {
  // Show: "Không thể nhận diện giọng nói. Vui lòng nói rõ ràng hơn."
}

// 5. Network error
try {
  await kaiwaService.practiceKaiwa(request);
} catch (err) {
  if (err.code === 'ECONNABORTED') {
    // Timeout
    setError('Request timeout. Vui lòng thử lại.');
  } else if (!err.response) {
    // Network error
    setError('Không thể kết nối đến server. Vui lòng kiểm tra kết nối.');
  } else {
    // Server error
    setError(err.response.data?.message || 'Có lỗi xảy ra. Vui lòng thử lại.');
  }
}
```

---

## ✅ Best Practices

### 1. Audio Recording:
- ✅ Sử dụng `sampleRate: 16000` (16kHz) - tốt nhất cho tiếng Nhật
- ✅ Enable `echoCancellation`, `noiseSuppression`, `autoGainControl`
- ✅ Validate audio size trước khi gửi (max 10MB)
- ✅ Show loading state khi đang record

### 2. Base64 Encoding:
- ✅ Remove data URL prefix (`data:audio/webm;base64,`) trước khi gửi
- ✅ Chỉ gửi phần Base64 string thuần

### 3. API Calls:
- ✅ Set timeout hợp lý (30s cho audio processing)
- ✅ Show loading indicator khi đang xử lý
- ✅ Handle errors gracefully với message tiếng Việt

### 4. User Experience:
- ✅ Cho phép user chọn level trước khi practice
- ✅ Có thể tự nhập câu hoặc chọn câu mẫu
- ✅ Hiển thị audio playback để user nghe lại
- ✅ Show scores và feedback rõ ràng
- ✅ Cho phép practice lại nhiều lần

### 5. Performance:
- ✅ Cleanup MediaRecorder khi component unmount
- ✅ Stop all tracks để release microphone
- ✅ Debounce API calls nếu cần

### 6. Code Structure:
```typescript
// Recommended folder structure:
src/
  components/
    KaiwaPractice.tsx
  hooks/
    useAudioRecorder.ts
  services/
    kaiwaService.ts
  utils/
    audioUtils.ts
  types/
    kaiwa.types.ts
```

---

## 🔗 Related APIs

### Get Default Settings:
```typescript
GET /api/ai/defaults

// Returns default language, voice settings for Vietnamese users
```

### Text to Speech (Optional - để phát câu mẫu):
```typescript
POST /api/ai/text-to-speech

Request:
{
  "text": "私は日本語を勉強しています",
  "voice": "ja-JP-Standard-A",
  "speed": "normal",
  "audioFormat": "mp3"
}

Response:
{
  "success": true,
  "data": {
    "audioData": "base64_encoded_audio",
    "audioFormat": "mp3"
  }
}
```

---

## 📝 Notes

1. **Audio Format**: Browser thường record WebM/Opus, nhưng BE sẽ tự convert sang WAV. Bạn chỉ cần gửi Base64 string.

2. **Level Selection**: User phải chọn level (N5-N1) trước khi practice. Level này ảnh hưởng đến:
   - Scoring thresholds
   - Tolerance cho accuracy
   - Feedback messages

3. **Scores**: Tất cả scores là số thập phân từ 0-1. Nhân với 100 để hiển thị phần trăm.

4. **Feedback**: Tất cả feedback đều bằng tiếng Việt, phù hợp cho người Việt học tiếng Nhật.

5. **Timeout**: Audio processing có thể mất 10-30 giây tùy độ dài audio. Nên set timeout hợp lý.

---

## 🐛 Troubleshooting

### Microphone không hoạt động:
- Check browser permissions
- Test với `navigator.mediaDevices.getUserMedia()`
- Check HTTPS (required for microphone access)

### Audio quá lớn:
- Giới hạn thời gian record (ví dụ: max 30 giây)
- Compress audio nếu cần

### API timeout:
- Tăng timeout lên 60s nếu cần
- Show progress indicator

### Base64 encoding lỗi:
- Đảm bảo remove data URL prefix
- Check blob type trước khi encode

---

## 📞 Support

Nếu có vấn đề, check:
1. Browser console logs
2. Network tab trong DevTools
3. Backend logs
4. Swagger UI: `/swagger-ui.html` để test API trực tiếp

---

**Chúc bạn tích hợp thành công! 🎉**

