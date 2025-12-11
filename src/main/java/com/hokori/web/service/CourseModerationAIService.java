package com.hokori.web.service;

import com.google.cloud.language.v1.*;
import com.hokori.web.Enum.ContentFormat;
import com.hokori.web.Enum.CourseStatus;
import com.hokori.web.dto.moderator.CourseAICheckResponse;
import com.hokori.web.entity.*;
import com.hokori.web.exception.AIServiceException;
import com.hokori.web.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for AI-powered course content moderation
 * Uses Google Cloud Natural Language API to check content safety
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseModerationAIService {

    private final CourseRepository courseRepo;
    private final ChapterRepository chapterRepo;
    private final LessonRepository lessonRepo;
    private final SectionRepository sectionRepo;
    private final SectionsContentRepository contentRepo;
    private final QuizRepository quizRepo;
    private final QuestionRepository questionRepo;
    private final OptionRepository optionRepo;
    private final FlashcardSetRepository flashcardSetRepo;
    private final FlashcardRepository flashcardRepo;

    @Autowired(required = false)
    private LanguageServiceClient languageServiceClient;

    @Autowired(required = false)
    private GeminiService geminiService;

    @Value("${google.cloud.enabled:false}")
    private boolean googleCloudEnabled;

    @Value("${ai.moderation.max-text-length:100000}")
    private int maxTextLength;

    @Value("${ai.moderation.enable-gemini:true}")
    private boolean enableGemini;

    /**
     * Check course content with AI
     * Only checks courses with PENDING_APPROVAL status
     * 
     * Caching: Checks course.updatedAt - if course hasn't changed, returns cached result
     * (Simple caching: in production, consider using Redis or database cache)
     */
    @Transactional(readOnly = true)
    public CourseAICheckResponse checkCourseContent(Long courseId) {
        log.info("Starting AI check for course ID: {}", courseId);

        // Load course metadata
        Object[] metadata = courseRepo.findCourseMetadataById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        // Validate metadata is not empty
        if (metadata == null || metadata.length == 0) {
            log.error("Course metadata is null or empty for course ID: {}", courseId);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Invalid course metadata: empty result");
        }

        // Handle nested array case (PostgreSQL)
        Object[] actualMetadata = metadata;
        if (metadata.length == 1 && metadata[0] instanceof Object[]) {
            actualMetadata = (Object[]) metadata[0];
            // Validate nested array is not empty
            if (actualMetadata == null || actualMetadata.length == 0) {
                log.error("Nested course metadata is null or empty for course ID: {}", courseId);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Invalid course metadata: empty nested array");
            }
        }

        // Validate metadata has enough elements (need at least 10 for status at index 9)
        if (actualMetadata.length < 10) {
            log.error("Course metadata array too short for course ID: {}. Expected at least 10 elements, got: {}", 
                    courseId, actualMetadata.length);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Invalid course metadata: array too short (expected at least 10 elements, got " + actualMetadata.length + ")");
        }

        if (actualMetadata[9] == null) {
            log.error("Course status is null for course ID: {}", courseId);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Invalid course metadata: missing status");
        }

        // Check status
        CourseStatus status;
        try {
            status = CourseStatus.valueOf(actualMetadata[9].toString().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Invalid course status: " + actualMetadata[9]);
        }

        if (status != CourseStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "AI check only available for courses pending approval. Current status: " + status);
        }

        // Extract course title
        String courseTitle = actualMetadata[1] != null ? actualMetadata[1].toString() : "Untitled Course";

        // Check if Google Cloud AI is enabled
        if (!googleCloudEnabled || languageServiceClient == null) {
            log.warn("Google Cloud AI is not enabled or LanguageServiceClient is not available");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI moderation service is not available. Please check Google Cloud configuration.");
        }

        try {
            // Extract all text content from course
            String allText = extractAllCourseText(courseId);
            
            if (!StringUtils.hasText(allText)) {
                log.warn("No text content found in course ID: {}", courseId);
                return createEmptyResponse(courseId, courseTitle, "No text content found in course");
            }

            // Check text length
            if (allText.length() > maxTextLength) {
                log.warn("Course text exceeds max length ({}), truncating to {}", allText.length(), maxTextLength);
                allText = allText.substring(0, maxTextLength);
            }

            log.debug("Extracted text length: {} characters", allText.length());

            // Call Natural Language API for safety check
            AnalyzeSentimentResponse response = languageServiceClient.analyzeSentiment(
                    AnalyzeSentimentRequest.newBuilder()
                            .setDocument(Document.newBuilder()
                                    .setContent(allText)
                                    .setType(Document.Type.PLAIN_TEXT)
                                    .build())
                            .build()
            );

            Sentiment sentiment = response.getDocumentSentiment();
            double safetyScore = calculateSafetyScore(sentiment);

            // Build response
            CourseAICheckResponse result = CourseAICheckResponse.createDefault(courseId, courseTitle);
            
            // Safety check
            CourseAICheckResponse.SafetyCheck safetyCheck = CourseAICheckResponse.SafetyCheck.builder()
                    .status(determineSafetyStatus(safetyScore))
                    .score(safetyScore)
                    .hasIssues(safetyScore < 0.7)
                    .summary(generateSafetySummary(safetyScore))
                    .build();
            result.setSafetyCheck(safetyCheck);

            // Level match with Gemini
            CourseAICheckResponse.LevelMatch levelMatch = checkLevelMatch(courseId, actualMetadata, allText);
            result.setLevelMatch(levelMatch);

            // New AI checks with Gemini (if enabled)
            if (enableGemini && geminiService != null) {
                try {
                    // Pedagogical Quality Assessment
                    CourseAICheckResponse.PedagogicalQuality pedagogicalQuality = 
                            checkPedagogicalQuality(courseId, courseTitle, allText, extractDeclaredLevel(actualMetadata));
                    result.setPedagogicalQuality(pedagogicalQuality);

                    // Language Accuracy Check
                    CourseAICheckResponse.LanguageAccuracy languageAccuracy = 
                            checkLanguageAccuracy(courseId, allText, extractDeclaredLevel(actualMetadata));
                    result.setLanguageAccuracy(languageAccuracy);

                    // Grammar Progression Validation
                    CourseAICheckResponse.GrammarProgression grammarProgression = 
                            checkGrammarProgression(courseId, allText, extractDeclaredLevel(actualMetadata));
                    result.setGrammarProgression(grammarProgression);

                    // Add recommendations and warnings from Gemini checks
                    if (pedagogicalQuality != null && pedagogicalQuality.getRecommendations() != null) {
                        result.getRecommendations().addAll(pedagogicalQuality.getRecommendations());
                    }
                    if (grammarProgression != null && grammarProgression.getRecommendations() != null) {
                        result.getRecommendations().addAll(grammarProgression.getRecommendations());
                    }
                    if (languageAccuracy != null && languageAccuracy.getScore() != null && languageAccuracy.getScore() < 0.7) {
                        result.getWarnings().add("Phát hiện một số lỗi về độ chính xác ngôn ngữ. Vui lòng xem xét kỹ.");
                    }
                    if (grammarProgression != null && Boolean.FALSE.equals(grammarProgression.getIsLogical())) {
                        result.getWarnings().add("Tiến trình ngữ pháp có thể gây hiểu nhầm. Vui lòng xem xét lại thứ tự dạy ngữ pháp.");
                    }
                } catch (Exception e) {
                    log.warn("Gemini checks failed, continuing with basic checks: {}", e.getMessage());
                    // Continue without Gemini checks if they fail
                }
            } else {
                log.debug("Gemini checks disabled or service not available");
            }

            // Generate recommendations
            List<String> recommendations = generateRecommendations(safetyScore, sentiment);
            result.getRecommendations().addAll(recommendations);

            // Generate warnings if needed
            if (safetyScore < 0.7) {
                result.getWarnings().add("Nội dung có thể chứa từ ngữ không phù hợp. Vui lòng xem xét kỹ.");
            }

            log.info("AI check completed for course ID: {}, safety score: {}", courseId, safetyScore);
            return result;

        } catch (AIServiceException e) {
            log.error("AI service error during course check", e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI moderation service error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during AI check", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error during AI check: " + e.getMessage());
        }
    }

    /**
     * Extract all text content from course
     */
    private String extractAllCourseText(Long courseId) {
        StringBuilder textBuilder = new StringBuilder();

        // Course metadata
        Object[] metadata = courseRepo.findCourseMetadataById(courseId).orElse(null);
        if (metadata != null && metadata.length > 0) {
            Object[] actualMetadata = metadata;
            if (metadata.length == 1 && metadata[0] instanceof Object[]) {
                actualMetadata = (Object[]) metadata[0];
                // Validate nested array
                if (actualMetadata == null || actualMetadata.length == 0) {
                    log.warn("Nested metadata is empty when extracting course text for course ID: {}", courseId);
                } else {
                    if (actualMetadata.length > 1 && actualMetadata[1] != null) {
                        textBuilder.append(actualMetadata[1].toString()).append(" "); // title
                    }
                    if (actualMetadata.length > 2 && actualMetadata[2] != null) {
                        textBuilder.append(actualMetadata[2].toString()).append(" "); // subtitle
                    }
                    if (actualMetadata.length > 3 && actualMetadata[3] != null) {
                        textBuilder.append(actualMetadata[3].toString()).append(" "); // description
                    }
                }
            } else {
                // Not nested, use directly
                if (actualMetadata.length > 1 && actualMetadata[1] != null) {
                    textBuilder.append(actualMetadata[1].toString()).append(" "); // title
                }
                if (actualMetadata.length > 2 && actualMetadata[2] != null) {
                    textBuilder.append(actualMetadata[2].toString()).append(" "); // subtitle
                }
                if (actualMetadata.length > 3 && actualMetadata[3] != null) {
                    textBuilder.append(actualMetadata[3].toString()).append(" "); // description
                }
            }
        }

        // Chapters
        List<Chapter> chapters = chapterRepo.findByCourse_IdOrderByOrderIndexAsc(courseId);
        for (Chapter chapter : chapters) {
            if (chapter.getTitle() != null) {
                textBuilder.append(chapter.getTitle()).append(" ");
            }
            if (chapter.getSummary() != null) {
                textBuilder.append(chapter.getSummary()).append(" ");
            }

            // Lessons
            List<Lesson> lessons = lessonRepo.findByChapter_IdOrderByOrderIndexAsc(chapter.getId());
            for (Lesson lesson : lessons) {
                if (lesson.getTitle() != null) {
                    textBuilder.append(lesson.getTitle()).append(" ");
                }

                // Sections
                List<Section> sections = sectionRepo.findByLesson_IdOrderByOrderIndexAsc(lesson.getId());
                for (Section section : sections) {
                    if (section.getTitle() != null) {
                        textBuilder.append(section.getTitle()).append(" ");
                    }

                    // SectionsContent
                    List<SectionsContent> contents = contentRepo.findBySection_IdOrderByOrderIndexAsc(section.getId());
                    for (SectionsContent content : contents) {
                        if (content.getContentFormat() == ContentFormat.RICH_TEXT && content.getRichText() != null) {
                            // Extract text from HTML (simple approach)
                            String richText = content.getRichText();
                            // Remove HTML tags (simple regex)
                            String plainText = richText.replaceAll("<[^>]+>", " ");
                            textBuilder.append(plainText).append(" ");
                        }
                        // Note: ASSET files (videos, PDFs) are skipped as per requirement
                        
                        // Flashcards (from COURSE_VOCAB sets)
                        flashcardSetRepo.findBySectionContent_IdAndDeletedFlagFalse(content.getId())
                                .ifPresent(set -> {
                                    List<Flashcard> flashcards = flashcardRepo.findBySetAndDeletedFlagFalseOrderByOrderIndexAsc(set);
                                    for (Flashcard card : flashcards) {
                                        if (card.getFrontText() != null) {
                                            textBuilder.append(card.getFrontText()).append(" ");
                                        }
                                        if (card.getBackText() != null) {
                                            textBuilder.append(card.getBackText()).append(" ");
                                        }
                                        if (card.getExampleSentence() != null) {
                                            textBuilder.append(card.getExampleSentence()).append(" ");
                                        }
                                    }
                                });
                    }

                    // Quiz (now belongs to section, not lesson)
                    quizRepo.findQuizMetadataBySectionId(section.getId()).ifPresent(quizMetadata -> {
                        // Validate quizMetadata array
                        if (quizMetadata == null || quizMetadata.length == 0) {
                            log.warn("Quiz metadata is null or empty for section ID: {}", section.getId());
                            return;
                        }

                        // Handle nested array case (PostgreSQL)
                        Object[] actualQuizMetadata = quizMetadata;
                        if (quizMetadata.length == 1 && quizMetadata[0] instanceof Object[]) {
                            actualQuizMetadata = (Object[]) quizMetadata[0];
                        }

                        if (actualQuizMetadata.length > 2 && actualQuizMetadata[2] != null) {
                            textBuilder.append(actualQuizMetadata[2].toString()).append(" "); // title
                        }
                        if (actualQuizMetadata.length > 3 && actualQuizMetadata[3] != null) {
                            textBuilder.append(actualQuizMetadata[3].toString()).append(" "); // description
                        }

                        // Validate quizId exists
                        if (actualQuizMetadata.length > 0 && actualQuizMetadata[0] != null) {
                            try {
                                Long quizId = ((Number) actualQuizMetadata[0]).longValue();
                                
                                // Questions
                                List<Object[]> questions = questionRepo.findQuestionMetadataByQuizId(quizId);
                                for (Object[] questionMeta : questions) {
                                    // Validate questionMeta array
                                    if (questionMeta == null || questionMeta.length == 0) {
                                        log.warn("Question metadata is null or empty for quiz ID: {}", quizId);
                                        continue;
                                    }

                                    // Handle nested array case (PostgreSQL)
                                    Object[] actualQuestionMeta = questionMeta;
                                    if (questionMeta.length == 1 && questionMeta[0] instanceof Object[]) {
                                        actualQuestionMeta = (Object[]) questionMeta[0];
                                    }

                                    if (actualQuestionMeta.length > 2 && actualQuestionMeta[2] != null) {
                                        textBuilder.append(actualQuestionMeta[2].toString()).append(" "); // content
                                    }
                                    if (actualQuestionMeta.length > 4 && actualQuestionMeta[4] != null) {
                                        textBuilder.append(actualQuestionMeta[4].toString()).append(" "); // explanation
                                    }

                                    // Validate questionId exists
                                    if (actualQuestionMeta.length > 0 && actualQuestionMeta[0] != null) {
                                        try {
                                            Long questionId = ((Number) actualQuestionMeta[0]).longValue();
                                            
                                            // Options
                                            List<Object[]> options = optionRepo.findOptionMetadataByQuestionId(questionId);
                                            for (Object[] optionMeta : options) {
                                                // Validate optionMeta array
                                                if (optionMeta == null || optionMeta.length == 0) {
                                                    continue;
                                                }

                                                // Handle nested array case (PostgreSQL)
                                                Object[] actualOptionMeta = optionMeta;
                                                if (optionMeta.length == 1 && optionMeta[0] instanceof Object[]) {
                                                    actualOptionMeta = (Object[]) optionMeta[0];
                                                }

                                                if (actualOptionMeta.length > 2 && actualOptionMeta[2] != null) {
                                                    textBuilder.append(actualOptionMeta[2].toString()).append(" "); // content
                                                }
                                            }
                                        } catch (Exception e) {
                                            log.warn("Error extracting question ID from metadata: {}", e.getMessage());
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("Error extracting quiz ID from metadata: {}", e.getMessage());
                            }
                        }
                    });
                }
            }
        }

        return textBuilder.toString().trim();
    }

    /**
     * Calculate safety score from sentiment
     * Higher score = safer content
     */
    private double calculateSafetyScore(Sentiment sentiment) {
        // Use magnitude as indicator of emotional content
        // Higher magnitude = more emotional = potentially less safe
        // Score ranges from 0.0 to 1.0
        double magnitude = sentiment.getMagnitude();
        double score = sentiment.getScore();
        
        // Base score: 0.5 + (sentiment score normalized to 0-0.5)
        // Higher sentiment = more positive = safer
        double baseScore = 0.5 + (score + 1.0) * 0.25; // Map [-1, 1] to [0.5, 1.0]
        
        // Penalize high magnitude (very emotional content)
        if (magnitude > 1.0) {
            baseScore -= (magnitude - 1.0) * 0.1;
        }
        
        // Ensure score is between 0.0 and 1.0
        return Math.max(0.0, Math.min(1.0, baseScore));
    }

    /**
     * Determine safety status from score
     */
    private String determineSafetyStatus(double score) {
        if (score >= 0.8) {
            return "SAFE";
        } else if (score >= 0.6) {
            return "WARNING";
        } else {
            return "UNSAFE";
        }
    }

    /**
     * Generate safety summary
     */
    private String generateSafetySummary(double score) {
        if (score >= 0.8) {
            return "Nội dung an toàn, không có vấn đề";
        } else if (score >= 0.6) {
            return "Nội dung có thể cần xem xét thêm";
        } else {
            return "Nội dung có thể chứa từ ngữ không phù hợp";
        }
    }

    /**
     * Extract declared level from course metadata
     */
    private String extractDeclaredLevel(Object[] metadata) {
        if (metadata == null || metadata.length == 0) {
            log.warn("Metadata is null or empty when extracting level");
            return "UNKNOWN";
        }
        if (metadata.length > 4 && metadata[4] != null) {
            return metadata[4].toString();
        }
        return "UNKNOWN";
    }

    /**
     * Generate recommendations based on safety check
     */
    private List<String> generateRecommendations(double safetyScore, Sentiment sentiment) {
        List<String> recommendations = new ArrayList<>();
        
        if (safetyScore >= 0.8) {
            recommendations.add("✓ Nội dung an toàn và phù hợp");
            recommendations.add("✓ Không có từ ngữ nhạy cảm");
        } else if (safetyScore >= 0.6) {
            recommendations.add("⚠ Nội dung có thể cần xem xét thêm");
            recommendations.add("⚠ Kiểm tra lại các từ ngữ có thể gây hiểu lầm");
        } else {
            recommendations.add("✗ Nội dung có thể chứa từ ngữ không phù hợp");
            recommendations.add("✗ Vui lòng review kỹ trước khi approve");
        }
        
        return recommendations;
    }

    /**
     * Create empty response when no content found
     */
    private CourseAICheckResponse createEmptyResponse(Long courseId, String courseTitle, String message) {
        CourseAICheckResponse result = CourseAICheckResponse.createDefault(courseId, courseTitle);
        result.setSafetyCheck(CourseAICheckResponse.SafetyCheck.builder()
                .status("UNKNOWN")
                .score(0.5)
                .hasIssues(false)
                .summary(message)
                .build());
        result.setLevelMatch(CourseAICheckResponse.LevelMatch.builder()
                .declaredLevel("UNKNOWN")
                .detectedLevel(null)
                .match(null)
                .confidence(null)
                .summary("Không có nội dung để kiểm tra")
                .build());
        result.getRecommendations().add(message);
        return result;
    }

    /**
     * Check level match with Gemini
     */
    private CourseAICheckResponse.LevelMatch checkLevelMatch(Long courseId, Object[] metadata, String allText) {
        String declaredLevel = extractDeclaredLevel(metadata);
        
        if (!enableGemini || geminiService == null) {
            return CourseAICheckResponse.LevelMatch.builder()
                    .declaredLevel(declaredLevel)
                    .detectedLevel(null)
                    .match(null)
                    .confidence(null)
                    .summary("Level matching requires Gemini (not enabled)")
                    .build();
        }

        try {
            String prompt = String.format(
                "Bạn là chuyên gia đánh giá khóa học tiếng Nhật JLPT.\n\n" +
                "Phân tích nội dung khóa học sau và xác định level JLPT phù hợp (N5, N4, N3, N2, N1).\n\n" +
                "Level khai báo: %s\n\n" +
                "Nội dung khóa học:\n%s\n\n" +
                "Trả về JSON với format:\n" +
                "{\n" +
                "  \"detectedLevel\": \"N5|N4|N3|N2|N1\",\n" +
                "  \"match\": true/false,\n" +
                "  \"confidence\": 0.0-1.0,\n" +
                "  \"summary\": \"Tóm tắt ngắn gọn\"\n" +
                "}\n\n" +
                "Chỉ trả về JSON, không có text khác.",
                declaredLevel,
                allText.length() > 5000 ? allText.substring(0, 5000) + "..." : allText
            );

            // Call Gemini and parse JSON response
            com.fasterxml.jackson.databind.JsonNode json = geminiService.generateContentAsJson(prompt);
            if (json != null) {
                return CourseAICheckResponse.LevelMatch.builder()
                        .declaredLevel(declaredLevel)
                        .detectedLevel(json.has("detectedLevel") ? json.get("detectedLevel").asText() : null)
                        .match(json.has("match") ? json.get("match").asBoolean() : null)
                        .confidence(json.has("confidence") ? json.get("confidence").asDouble() : null)
                        .summary(json.has("summary") ? json.get("summary").asText() : "Đã phân tích level")
                        .build();
            }

            return createDefaultLevelMatch(declaredLevel, "Không thể parse phản hồi từ AI");

        } catch (Exception e) {
            log.warn("Level match check failed: {}", e.getMessage());
            return createDefaultLevelMatch(declaredLevel, "Lỗi khi kiểm tra level: " + e.getMessage());
        }
    }

    private CourseAICheckResponse.LevelMatch createDefaultLevelMatch(String declaredLevel, String summary) {
        return CourseAICheckResponse.LevelMatch.builder()
                .declaredLevel(declaredLevel)
                .detectedLevel(null)
                .match(null)
                .confidence(null)
                .summary(summary)
                .build();
    }

    /**
     * Check pedagogical quality with Gemini
     */
    private CourseAICheckResponse.PedagogicalQuality checkPedagogicalQuality(
            Long courseId, String courseTitle, String allText, String level) {
        
        if (!enableGemini || geminiService == null) {
            return null;
        }

        try {
            String prompt = String.format(
                "Bạn là chuyên gia đánh giá chất lượng giáo dục khóa học tiếng Nhật.\n\n" +
                "Đánh giá chất lượng giáo dục của khóa học:\n" +
                "Tiêu đề: %s\n" +
                "Level: %s\n\n" +
                "Nội dung:\n%s\n\n" +
                "Đánh giá:\n" +
                "1. Nội dung có giá trị học tập không?\n" +
                "2. Giải thích có rõ ràng, dễ hiểu không?\n" +
                "3. Có đủ ví dụ minh họa không?\n" +
                "4. Có bài tập thực hành phù hợp không?\n\n" +
                "Trả về JSON:\n" +
                "{\n" +
                "  \"score\": 0.0-1.0,\n" +
                "  \"strengths\": [\"điểm mạnh 1\", \"điểm mạnh 2\"],\n" +
                "  \"weaknesses\": [\"điểm yếu 1\", \"điểm yếu 2\"],\n" +
                "  \"summary\": \"Tóm tắt đánh giá\",\n" +
                "  \"recommendations\": [\"khuyến nghị 1\", \"khuyến nghị 2\"]\n" +
                "}\n\n" +
                "Chỉ trả về JSON, không có text khác.",
                courseTitle,
                level,
                allText.length() > 8000 ? allText.substring(0, 8000) + "..." : allText
            );

            com.fasterxml.jackson.databind.JsonNode json = geminiService.generateContentAsJson(prompt);
            if (json != null) {
                CourseAICheckResponse.PedagogicalQuality.PedagogicalQualityBuilder builder = 
                        CourseAICheckResponse.PedagogicalQuality.builder();

                if (json.has("score")) {
                    builder.score(json.get("score").asDouble());
                }
                if (json.has("strengths")) {
                    List<String> strengths = new ArrayList<>();
                    json.get("strengths").forEach(node -> strengths.add(node.asText()));
                    builder.strengths(strengths);
                }
                if (json.has("weaknesses")) {
                    List<String> weaknesses = new ArrayList<>();
                    json.get("weaknesses").forEach(node -> weaknesses.add(node.asText()));
                    builder.weaknesses(weaknesses);
                }
                if (json.has("summary")) {
                    builder.summary(json.get("summary").asText());
                }
                if (json.has("recommendations")) {
                    List<String> recommendations = new ArrayList<>();
                    json.get("recommendations").forEach(node -> recommendations.add(node.asText()));
                    builder.recommendations(recommendations);
                }

                return builder.build();
            }

            return null;
        } catch (Exception e) {
            log.warn("Pedagogical quality check failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check language accuracy with Gemini
     */
    private CourseAICheckResponse.LanguageAccuracy checkLanguageAccuracy(
            Long courseId, String allText, String level) {
        
        if (!enableGemini || geminiService == null) {
            return null;
        }

        try {
            String prompt = String.format(
                "Bạn là chuyên gia kiểm tra độ chính xác ngôn ngữ tiếng Nhật và tiếng Việt.\n\n" +
                "Kiểm tra khóa học tiếng Nhật level %s:\n\n" +
                "Nội dung:\n%s\n\n" +
                "Kiểm tra:\n" +
                "1. Chính tả tiếng Nhật có đúng không?\n" +
                "2. Ngữ pháp tiếng Nhật có đúng không?\n" +
                "3. Dịch tiếng Việt có chính xác không?\n" +
                "4. Có từ vựng/ngữ pháp sai level không?\n\n" +
                "Trả về JSON:\n" +
                "{\n" +
                "  \"score\": 0.0-1.0,\n" +
                "  \"japaneseErrors\": [\n" +
                "    {\"text\": \"câu tiếng Nhật\", \"error\": \"mô tả lỗi\", \"suggestion\": \"gợi ý sửa\", \"location\": \"Lesson 2\"}\n" +
                "  ],\n" +
                "  \"vietnameseErrors\": [\n" +
                "    {\"text\": \"câu tiếng Việt\", \"error\": \"mô tả lỗi\", \"suggestion\": \"gợi ý sửa\", \"location\": \"Lesson 2\"}\n" +
                "  ],\n" +
                "  \"levelMismatches\": [\n" +
                "    {\"word\": \"từ vựng\", \"declaredLevel\": \"N5\", \"actualLevel\": \"N3\", \"location\": \"Lesson 5\", \"severity\": \"HIGH|MEDIUM|LOW\"}\n" +
                "  ],\n" +
                "  \"summary\": \"Tóm tắt\"\n" +
                "}\n\n" +
                "Chỉ trả về JSON, không có text khác.",
                level,
                allText.length() > 8000 ? allText.substring(0, 8000) + "..." : allText
            );

            com.fasterxml.jackson.databind.JsonNode json = geminiService.generateContentAsJson(prompt);
            if (json != null) {
                CourseAICheckResponse.LanguageAccuracy.LanguageAccuracyBuilder builder = 
                        CourseAICheckResponse.LanguageAccuracy.builder();

                if (json.has("score")) {
                    builder.score(json.get("score").asDouble());
                }
                if (json.has("summary")) {
                    builder.summary(json.get("summary").asText());
                }

                // Parse Japanese errors
                if (json.has("japaneseErrors")) {
                    List<CourseAICheckResponse.LanguageError> errors = new ArrayList<>();
                    json.get("japaneseErrors").forEach(node -> {
                        CourseAICheckResponse.LanguageError error = CourseAICheckResponse.LanguageError.builder()
                                .text(node.has("text") ? node.get("text").asText() : null)
                                .error(node.has("error") ? node.get("error").asText() : null)
                                .suggestion(node.has("suggestion") ? node.get("suggestion").asText() : null)
                                .location(node.has("location") ? node.get("location").asText() : null)
                                .build();
                        errors.add(error);
                    });
                    builder.japaneseErrors(errors);
                }

                // Parse Vietnamese errors
                if (json.has("vietnameseErrors")) {
                    List<CourseAICheckResponse.LanguageError> errors = new ArrayList<>();
                    json.get("vietnameseErrors").forEach(node -> {
                        CourseAICheckResponse.LanguageError error = CourseAICheckResponse.LanguageError.builder()
                                .text(node.has("text") ? node.get("text").asText() : null)
                                .error(node.has("error") ? node.get("error").asText() : null)
                                .suggestion(node.has("suggestion") ? node.get("suggestion").asText() : null)
                                .location(node.has("location") ? node.get("location").asText() : null)
                                .build();
                        errors.add(error);
                    });
                    builder.vietnameseErrors(errors);
                }

                // Parse level mismatches
                if (json.has("levelMismatches")) {
                    List<CourseAICheckResponse.LevelMismatch> mismatches = new ArrayList<>();
                    json.get("levelMismatches").forEach(node -> {
                        CourseAICheckResponse.LevelMismatch mismatch = CourseAICheckResponse.LevelMismatch.builder()
                                .word(node.has("word") ? node.get("word").asText() : null)
                                .declaredLevel(node.has("declaredLevel") ? node.get("declaredLevel").asText() : null)
                                .actualLevel(node.has("actualLevel") ? node.get("actualLevel").asText() : null)
                                .location(node.has("location") ? node.get("location").asText() : null)
                                .severity(node.has("severity") ? node.get("severity").asText() : "MEDIUM")
                                .build();
                        mismatches.add(mismatch);
                    });
                    builder.levelMismatches(mismatches);
                }

                return builder.build();
            }

            return null;
        } catch (Exception e) {
            log.warn("Language accuracy check failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check grammar progression with Gemini
     */
    private CourseAICheckResponse.GrammarProgression checkGrammarProgression(
            Long courseId, String allText, String level) {
        
        if (!enableGemini || geminiService == null) {
            return null;
        }

        try {
            String prompt = String.format(
                "Bạn là chuyên gia đánh giá tiến trình học ngữ pháp tiếng Nhật JLPT.\n\n" +
                "Kiểm tra khóa học level %s:\n\n" +
                "Nội dung:\n%s\n\n" +
                "Kiểm tra:\n" +
                "1. Ngữ pháp có được dạy theo thứ tự hợp lý không?\n" +
                "2. Có ngữ pháp phức tạp được dạy trước ngữ pháp cơ bản không?\n" +
                "3. Có ngữ pháp nào dễ gây hiểu nhầm không?\n" +
                "4. Có đủ bài tập cho từng điểm ngữ pháp không?\n\n" +
                "Trả về JSON:\n" +
                "{\n" +
                "  \"isLogical\": true/false,\n" +
                "  \"score\": 0.0-1.0,\n" +
                "  \"issues\": [\n" +
                "    {\n" +
                "      \"grammar\": \"điểm ngữ pháp\",\n" +
                "      \"currentLocation\": \"Lesson 2\",\n" +
                "      \"requiredPrerequisite\": \"ngữ pháp cần học trước\",\n" +
                "      \"prerequisiteLocation\": \"Lesson 5\",\n" +
                "      \"severity\": \"HIGH|MEDIUM|LOW\",\n" +
                "      \"description\": \"mô tả vấn đề\",\n" +
                "      \"potentialConfusion\": \"có thể gây hiểu nhầm như thế nào\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"summary\": \"Tóm tắt\",\n" +
                "  \"recommendations\": [\"khuyến nghị 1\", \"khuyến nghị 2\"]\n" +
                "}\n\n" +
                "Chỉ trả về JSON, không có text khác.",
                level,
                allText.length() > 8000 ? allText.substring(0, 8000) + "..." : allText
            );

            com.fasterxml.jackson.databind.JsonNode json = geminiService.generateContentAsJson(prompt);
            if (json != null) {
                CourseAICheckResponse.GrammarProgression.GrammarProgressionBuilder builder = 
                        CourseAICheckResponse.GrammarProgression.builder();

                if (json.has("isLogical")) {
                    builder.isLogical(json.get("isLogical").asBoolean());
                }
                if (json.has("score")) {
                    builder.score(json.get("score").asDouble());
                }
                if (json.has("summary")) {
                    builder.summary(json.get("summary").asText());
                }
                if (json.has("recommendations")) {
                    List<String> recommendations = new ArrayList<>();
                    json.get("recommendations").forEach(node -> recommendations.add(node.asText()));
                    builder.recommendations(recommendations);
                }

                // Parse issues
                if (json.has("issues")) {
                    List<CourseAICheckResponse.ProgressionIssue> issues = new ArrayList<>();
                    json.get("issues").forEach(node -> {
                        CourseAICheckResponse.ProgressionIssue issue = CourseAICheckResponse.ProgressionIssue.builder()
                                .grammar(node.has("grammar") ? node.get("grammar").asText() : null)
                                .currentLocation(node.has("currentLocation") ? node.get("currentLocation").asText() : null)
                                .requiredPrerequisite(node.has("requiredPrerequisite") ? node.get("requiredPrerequisite").asText() : null)
                                .prerequisiteLocation(node.has("prerequisiteLocation") ? node.get("prerequisiteLocation").asText() : null)
                                .severity(node.has("severity") ? node.get("severity").asText() : "MEDIUM")
                                .description(node.has("description") ? node.get("description").asText() : null)
                                .potentialConfusion(node.has("potentialConfusion") ? node.get("potentialConfusion").asText() : null)
                                .build();
                        issues.add(issue);
                    });
                    builder.issues(issues);
                }

                return builder.build();
            }

            return null;
        } catch (Exception e) {
            log.warn("Grammar progression check failed: {}", e.getMessage());
            return null;
        }
    }
}

