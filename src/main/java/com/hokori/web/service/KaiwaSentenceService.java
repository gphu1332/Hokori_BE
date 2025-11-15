package com.hokori.web.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for managing Kaiwa practice sentences by JLPT level
 * Provides suggested sentences and practice materials
 */
@Service
public class KaiwaSentenceService {
    
    private static final Logger logger = LoggerFactory.getLogger(KaiwaSentenceService.class);
    
    /**
     * Get suggested sentences for kaiwa practice by JLPT level
     * These are common conversational phrases appropriate for each level
     */
    public List<Map<String, Object>> getSuggestedSentences(String level) {
        String normalizedLevel = normalizeLevel(level);
        logger.debug("Getting suggested sentences for level: {}", normalizedLevel);
        
        List<Map<String, Object>> sentences = new ArrayList<>();
        
        switch (normalizedLevel) {
            case "N5":
                sentences.addAll(getN5Sentences());
                break;
            case "N4":
                sentences.addAll(getN4Sentences());
                break;
            case "N3":
                sentences.addAll(getN3Sentences());
                break;
            case "N2":
                sentences.addAll(getN2Sentences());
                break;
            case "N1":
                sentences.addAll(getN1Sentences());
                break;
            default:
                sentences.addAll(getN5Sentences());
        }
        
        return sentences;
    }
    
    /**
     * Get random sentence for practice by level
     */
    public Map<String, Object> getRandomSentence(String level) {
        List<Map<String, Object>> sentences = getSuggestedSentences(level);
        if (sentences.isEmpty()) {
            return null;
        }
        Random random = new Random();
        return sentences.get(random.nextInt(sentences.size()));
    }
    
    /**
     * Normalize JLPT level
     */
    private String normalizeLevel(String level) {
        if (level == null || level.isEmpty()) {
            return "N5";
        }
        String upperLevel = level.toUpperCase();
        if (upperLevel.equals("N5") || upperLevel.equals("N4") || 
            upperLevel.equals("N3") || upperLevel.equals("N2") || upperLevel.equals("N1")) {
            return upperLevel;
        }
        return "N5";
    }
    
    /**
     * N5 Level Sentences - Basic greetings and simple phrases
     */
    private List<Map<String, Object>> getN5Sentences() {
        List<Map<String, Object>> sentences = new ArrayList<>();
        
        String[] n5Texts = {
            "こんにちは", // Hello (afternoon)
            "おはようございます", // Good morning
            "こんばんは", // Good evening
            "ありがとうございます", // Thank you
            "すみません", // Excuse me / Sorry
            "さようなら", // Goodbye
            "お願いします", // Please
            "いただきます", // Before eating
            "ごちそうさまでした", // After eating
            "はじめまして", // Nice to meet you
            "よろしくお願いします", // Nice to meet you / Please
            "お元気ですか", // How are you?
            "元気です", // I'm fine
            "名前は何ですか", // What's your name?
            "私の名前は...です", // My name is...
            "どこから来ましたか", // Where are you from?
            "日本から来ました", // I'm from Japan
            "いくらですか", // How much?
            "これは何ですか", // What is this?
            "わかりました", // I understand
        };
        
        String[] n5Translations = {
            "Xin chào (buổi chiều)",
            "Chào buổi sáng",
            "Chào buổi tối",
            "Cảm ơn bạn",
            "Xin lỗi",
            "Tạm biệt",
            "Làm ơn",
            "Mời ăn (trước khi ăn)",
            "Cảm ơn vì bữa ăn (sau khi ăn)",
            "Rất vui được gặp bạn",
            "Rất mong được giúp đỡ",
            "Bạn khỏe không?",
            "Tôi khỏe",
            "Tên bạn là gì?",
            "Tên tôi là...",
            "Bạn đến từ đâu?",
            "Tôi đến từ Nhật Bản",
            "Bao nhiêu tiền?",
            "Đây là cái gì?",
            "Tôi hiểu rồi",
        };
        
        for (int i = 0; i < n5Texts.length; i++) {
            Map<String, Object> sentence = new HashMap<>();
            sentence.put("id", "N5-" + (i + 1));
            sentence.put("text", n5Texts[i]);
            sentence.put("translation", n5Translations[i]);
            sentence.put("level", "N5");
            sentence.put("category", getCategory(n5Texts[i]));
            sentence.put("difficulty", 1);
            sentences.add(sentence);
        }
        
        return sentences;
    }
    
    /**
     * N4 Level Sentences - Daily conversation
     */
    private List<Map<String, Object>> getN4Sentences() {
        List<Map<String, Object>> sentences = new ArrayList<>();
        
        String[] n4Texts = {
            "今日は何をしますか", // What will you do today?
            "映画を見に行きます", // I'm going to watch a movie
            "一緒に行きませんか", // Won't you go together?
            "何時に行きますか", // What time will you go?
            "駅はどこですか", // Where is the station?
            "右に曲がってください", // Please turn right
            "まっすぐ行ってください", // Please go straight
            "お腹が空きました", // I'm hungry
            "お腹がいっぱいです", // I'm full
            "美味しいですね", // It's delicious
            "お会計をお願いします", // Check please
            "いくらかかりますか", // How much does it cost?
            "予約をお願いします", // I'd like to make a reservation
            "電話番号を教えてください", // Please tell me your phone number
            "メールアドレスは何ですか", // What's your email address?
            "また今度お願いします", // See you next time
            "気をつけて", // Take care
            "お大事に", // Take care (when sick)
            "お疲れ様でした", // Good work / Thank you for your work
            "お先に失礼します", // Excuse me for leaving first
        };
        
        String[] n4Translations = {
            "Hôm nay bạn sẽ làm gì?",
            "Tôi sẽ đi xem phim",
            "Bạn có muốn đi cùng không?",
            "Bạn sẽ đi lúc mấy giờ?",
            "Nhà ga ở đâu?",
            "Hãy rẽ phải",
            "Hãy đi thẳng",
            "Tôi đói rồi",
            "Tôi no rồi",
            "Ngon quá nhỉ",
            "Làm ơn tính tiền",
            "Giá bao nhiêu?",
            "Tôi muốn đặt chỗ",
            "Làm ơn cho tôi số điện thoại",
            "Địa chỉ email của bạn là gì?",
            "Hẹn gặp lại lần sau",
            "Cẩn thận nhé",
            "Chúc bạn mau khỏe",
            "Bạn đã vất vả rồi",
            "Xin phép tôi về trước",
        };
        
        for (int i = 0; i < n4Texts.length; i++) {
            Map<String, Object> sentence = new HashMap<>();
            sentence.put("id", "N4-" + (i + 1));
            sentence.put("text", n4Texts[i]);
            sentence.put("translation", n4Translations[i]);
            sentence.put("level", "N4");
            sentence.put("category", getCategory(n4Texts[i]));
            sentence.put("difficulty", 2);
            sentences.add(sentence);
        }
        
        return sentences;
    }
    
    /**
     * N3 Level Sentences - Intermediate conversation
     */
    private List<Map<String, Object>> getN3Sentences() {
        List<Map<String, Object>> sentences = new ArrayList<>();
        
        String[] n3Texts = {
            "もしよろしければ、一緒に食事しませんか", // If you'd like, would you like to have a meal together?
            "申し訳ございませんが、今日は都合が悪いです", // I'm sorry, but today is not convenient
            "来週の予定を教えていただけますか", // Could you tell me next week's schedule?
            "この件について、もう少し詳しく説明していただけますか", // Could you explain this matter in more detail?
            "お時間があるときで構いません", // Anytime you have time is fine
            "お忙しいところ、すみません", // Sorry for bothering you while you're busy
            "おかげさまで、元気に過ごしています", // Thanks to you, I'm doing well
            "お久しぶりです", // Long time no see
            "お変わりありませんか", // How have you been?
            "お世話になっております", // Thank you for your continued support
        };
        
        String[] n3Translations = {
            "Nếu được, bạn có muốn ăn cùng không?",
            "Xin lỗi, nhưng hôm nay tôi không tiện",
            "Bạn có thể cho tôi biết lịch trình tuần sau không?",
            "Bạn có thể giải thích chi tiết hơn về vấn đề này không?",
            "Lúc nào bạn rảnh cũng được",
            "Xin lỗi vì làm phiền bạn lúc bận",
            "Nhờ bạn mà tôi sống khỏe mạnh",
            "Lâu rồi không gặp",
            "Bạn có khỏe không?",
            "Cảm ơn bạn đã luôn giúp đỡ",
        };
        
        for (int i = 0; i < n3Texts.length; i++) {
            Map<String, Object> sentence = new HashMap<>();
            sentence.put("id", "N3-" + (i + 1));
            sentence.put("text", n3Texts[i]);
            sentence.put("translation", n3Translations[i]);
            sentence.put("level", "N3");
            sentence.put("category", getCategory(n3Texts[i]));
            sentence.put("difficulty", 3);
            sentences.add(sentence);
        }
        
        return sentences;
    }
    
    /**
     * N2 Level Sentences - Upper-intermediate conversation
     */
    private List<Map<String, Object>> getN2Sentences() {
        List<Map<String, Object>> sentences = new ArrayList<>();
        
        String[] n2Texts = {
            "お忙しいところ恐縮ですが、お時間をいただけますでしょうか", // I'm sorry to bother you while you're busy, but could I have a moment?
            "本日はお忙しい中、お越しいただきありがとうございます", // Thank you for coming today despite being busy
            "ご都合がよろしいときに、お返事いただければと思います", // I'd appreciate a reply when it's convenient for you
            "お手数をおかけして申し訳ございません", // I'm sorry for the trouble
            "お心遣いありがとうございます", // Thank you for your consideration
            "お気になさらないでください", // Please don't worry about it
            "お役に立てて光栄です", // I'm honored to be of help
            "お力になれず申し訳ございません", // I'm sorry I couldn't be of help
            "ご理解いただきありがとうございます", // Thank you for your understanding
            "お待たせして申し訳ございませんでした", // I'm sorry to have kept you waiting
        };
        
        String[] n2Translations = {
            "Xin lỗi vì làm phiền bạn lúc bận, nhưng bạn có thể cho tôi một chút thời gian không?",
            "Cảm ơn bạn đã đến hôm nay dù bận rộn",
            "Tôi mong nhận được phản hồi khi bạn tiện",
            "Xin lỗi vì đã làm phiền bạn",
            "Cảm ơn bạn đã quan tâm",
            "Xin đừng lo lắng về điều đó",
            "Tôi rất vinh dự được giúp đỡ",
            "Xin lỗi vì không thể giúp được",
            "Cảm ơn bạn đã hiểu",
            "Xin lỗi vì đã để bạn chờ",
        };
        
        for (int i = 0; i < n2Texts.length; i++) {
            Map<String, Object> sentence = new HashMap<>();
            sentence.put("id", "N2-" + (i + 1));
            sentence.put("text", n2Texts[i]);
            sentence.put("translation", n2Translations[i]);
            sentence.put("level", "N2");
            sentence.put("category", getCategory(n2Texts[i]));
            sentence.put("difficulty", 4);
            sentences.add(sentence);
        }
        
        return sentences;
    }
    
    /**
     * N1 Level Sentences - Advanced conversation
     */
    private List<Map<String, Object>> getN1Sentences() {
        List<Map<String, Object>> sentences = new ArrayList<>();
        
        String[] n1Texts = {
            "お忙しいところ恐縮ですが、ご検討いただけますでしょうか", // I'm sorry to bother you while you're busy, but could you please consider it?
            "本日はお忙しい中、貴重なお時間をいただき誠にありがとうございます", // Thank you very much for your valuable time today despite being busy
            "ご多忙の折、恐縮ですが、ご返信いただければ幸いです", // I'm sorry to bother you during your busy schedule, but I'd appreciate a reply
            "お手数をおかけしてしまい、大変申し訳ございません", // I'm very sorry for the trouble I've caused
            "お心遣いをいただき、心より感謝申し上げます", // I sincerely thank you for your consideration
            "お気になさらないようお願い申し上げます", // I ask that you please don't worry about it
            "お役に立てることができ、光栄に存じます", // I consider it an honor to be of help
            "お力になれず、心苦しく思っております", // I feel sorry that I couldn't be of help
            "ご理解を賜り、厚く御礼申し上げます", // I express my deep gratitude for your understanding
            "お待たせいたしまして、誠に申し訳ございませんでした", // I sincerely apologize for having kept you waiting
        };
        
        String[] n1Translations = {
            "Xin lỗi vì làm phiền bạn lúc bận, nhưng bạn có thể xem xét không?",
            "Cảm ơn bạn rất nhiều vì đã dành thời gian quý báu hôm nay dù bận rộn",
            "Xin lỗi vì làm phiền bạn lúc bận, nhưng tôi rất mong nhận được phản hồi",
            "Tôi rất xin lỗi vì đã làm phiền bạn",
            "Tôi chân thành cảm ơn bạn đã quan tâm",
            "Xin đừng lo lắng về điều đó",
            "Tôi rất vinh dự được giúp đỡ",
            "Tôi cảm thấy rất tiếc vì không thể giúp được",
            "Tôi bày tỏ lòng biết ơn sâu sắc vì sự hiểu biết của bạn",
            "Tôi chân thành xin lỗi vì đã để bạn chờ",
        };
        
        for (int i = 0; i < n1Texts.length; i++) {
            Map<String, Object> sentence = new HashMap<>();
            sentence.put("id", "N1-" + (i + 1));
            sentence.put("text", n1Texts[i]);
            sentence.put("translation", n1Translations[i]);
            sentence.put("level", "N1");
            sentence.put("category", getCategory(n1Texts[i]));
            sentence.put("difficulty", 5);
            sentences.add(sentence);
        }
        
        return sentences;
    }
    
    /**
     * Categorize sentence by content
     */
    private String getCategory(String text) {
        if (text.contains("こんにちは") || text.contains("おはよう") || text.contains("こんばんは") || text.contains("さようなら")) {
            return "greeting";
        } else if (text.contains("ありがとう") || text.contains("すみません") || text.contains("申し訳")) {
            return "politeness";
        } else if (text.contains("何") || text.contains("どこ") || text.contains("いつ") || text.contains("いくら")) {
            return "question";
        } else if (text.contains("食べ") || text.contains("食事") || text.contains("美味しい")) {
            return "food";
        } else if (text.contains("行く") || text.contains("来る") || text.contains("駅") || text.contains("曲がる")) {
            return "direction";
        } else {
            return "general";
        }
    }
}

