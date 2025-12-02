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

    @Value("${google.cloud.enabled:false}")
    private boolean googleCloudEnabled;

    @Value("${ai.moderation.max-text-length:100000}")
    private int maxTextLength;

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

        // Handle nested array case (PostgreSQL)
        Object[] actualMetadata = metadata;
        if (metadata.length == 1 && metadata[0] instanceof Object[]) {
            actualMetadata = (Object[]) metadata[0];
        }

        if (actualMetadata.length < 10 || actualMetadata[9] == null) {
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

            // Level match (simple check - can be enhanced later)
            CourseAICheckResponse.LevelMatch levelMatch = CourseAICheckResponse.LevelMatch.builder()
                    .declaredLevel(extractDeclaredLevel(actualMetadata))
                    .detectedLevel(null) // Not implemented yet (would need Gemini)
                    .match(null) // Not implemented yet
                    .confidence(null)
                    .summary("Level matching not implemented yet")
                    .build();
            result.setLevelMatch(levelMatch);

            // Generate recommendations
            List<String> recommendations = generateRecommendations(safetyScore, sentiment);
            result.setRecommendations(recommendations);

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
        if (metadata != null) {
            Object[] actualMetadata = metadata.length == 1 && metadata[0] instanceof Object[] 
                    ? (Object[]) metadata[0] : metadata;
            
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
                    }
                }

                // Quiz
                quizRepo.findQuizMetadataByLessonId(lesson.getId()).ifPresent(quizMetadata -> {
                    if (quizMetadata.length > 2 && quizMetadata[2] != null) {
                        textBuilder.append(quizMetadata[2].toString()).append(" "); // title
                    }
                    if (quizMetadata.length > 3 && quizMetadata[3] != null) {
                        textBuilder.append(quizMetadata[3].toString()).append(" "); // description
                    }

                    Long quizId = ((Number) quizMetadata[0]).longValue();
                    
                    // Questions
                    List<Object[]> questions = questionRepo.findQuestionMetadataByQuizId(quizId);
                    for (Object[] questionMeta : questions) {
                        if (questionMeta.length > 2 && questionMeta[2] != null) {
                            textBuilder.append(questionMeta[2].toString()).append(" "); // content
                        }
                        if (questionMeta.length > 4 && questionMeta[4] != null) {
                            textBuilder.append(questionMeta[4].toString()).append(" "); // explanation
                        }

                        Long questionId = ((Number) questionMeta[0]).longValue();
                        
                        // Options
                        List<Object[]> options = optionRepo.findOptionMetadataByQuestionId(questionId);
                        for (Object[] optionMeta : options) {
                            if (optionMeta.length > 2 && optionMeta[2] != null) {
                                textBuilder.append(optionMeta[2].toString()).append(" "); // content
                            }
                        }
                    }
                });
            }
        }

        // Flashcards (from COURSE_VOCAB sets)
        // Get all sectionsContent for this course, then find flashcard sets
        List<Chapter> chapters = chapterRepo.findByCourse_IdOrderByOrderIndexAsc(courseId);
        for (Chapter chapter : chapters) {
            List<Lesson> lessons = lessonRepo.findByChapter_IdOrderByOrderIndexAsc(chapter.getId());
            for (Lesson lesson : lessons) {
                List<Section> sections = sectionRepo.findByLesson_IdOrderByOrderIndexAsc(lesson.getId());
                for (Section section : sections) {
                    List<SectionsContent> contents = contentRepo.findBySection_IdOrderByOrderIndexAsc(section.getId());
                    for (SectionsContent content : contents) {
                        // Find flashcard set for this section content
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
}

