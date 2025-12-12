package com.hokori.web.service;

import com.hokori.web.entity.Payment;
import com.hokori.web.entity.TeacherRevenue;
import com.hokori.web.repository.CourseRepository;
import com.hokori.web.repository.EnrollmentRepository;
import com.hokori.web.repository.TeacherRevenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service để quản lý revenue và tính toán doanh thu cho teacher
 * 
 * Logic:
 * - Khi payment thành công → tạo TeacherRevenue records
 * - Admin commission: 20%
 * - Teacher revenue: 80%
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RevenueService {
    
    private static final double TEACHER_REVENUE_PERCENT = 0.80; // 80%
    private static final double ADMIN_COMMISSION_PERCENT = 0.20; // 20%
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    
    private final TeacherRevenueRepository revenueRepo;
    private final CourseRepository courseRepo;
    private final EnrollmentRepository enrollmentRepo;
    
    /**
     * Tạo revenue records khi payment thành công
     * Gọi từ PaymentService sau khi payment status = PAID
     */
    @Transactional
    public void createRevenueFromPayment(Payment payment) {
        if (payment.getStatus() != com.hokori.web.Enum.PaymentStatus.PAID) {
            log.warn("Cannot create revenue from payment {} with status {}", payment.getId(), payment.getStatus());
            return;
        }
        
        if (payment.getCourseIds() == null || payment.getCourseIds().trim().isEmpty()) {
            log.debug("Payment {} has no courses, skipping revenue creation", payment.getId());
            return;
        }
        
        try {
            // Parse course IDs from JSON
            List<Long> courseIds = parseCourseIds(payment.getCourseIds());
            if (courseIds.isEmpty()) {
                log.warn("No valid course IDs found in payment {}", payment.getId());
                return;
            }
            
            // Get year-month from payment paidAt (using Vietnam timezone)
            ZoneId vietnamZone = ZoneId.of("Asia/Ho_Chi_Minh");
            String yearMonth = payment.getPaidAt() != null 
                    ? YearMonth.from(payment.getPaidAt().atZone(vietnamZone))
                            .format(YEAR_MONTH_FORMATTER)
                    : YearMonth.now(vietnamZone).format(YEAR_MONTH_FORMATTER);
            
            // Calculate price per course
            long totalCoursePriceCents = 0L;
            List<com.hokori.web.entity.Course> courses = new java.util.ArrayList<>();
            for (Long courseId : courseIds) {
                com.hokori.web.entity.Course course = courseRepo.findById(courseId).orElse(null);
                if (course != null && course.getUserId() != null) {
                    courses.add(course);
                    long coursePriceCents = course.getDiscountedPriceCents() != null 
                            ? course.getDiscountedPriceCents() 
                            : (course.getPriceCents() != null ? course.getPriceCents() : 0L);
                    totalCoursePriceCents += coursePriceCents;
                }
            }
            
            if (courses.isEmpty()) {
                log.warn("No valid courses found for payment {}", payment.getId());
                return;
            }
            
            // Calculate price per course (proportional if multiple courses)
            // If single course, use its price; if multiple, distribute proportionally
            for (com.hokori.web.entity.Course course : courses) {
                long coursePriceCents = course.getDiscountedPriceCents() != null 
                        ? course.getDiscountedPriceCents() 
                        : (course.getPriceCents() != null ? course.getPriceCents() : 0L);
                
                // Calculate proportional amount if multiple courses
                long courseAmountCents = totalCoursePriceCents > 0
                        ? Math.round((double) coursePriceCents / totalCoursePriceCents * payment.getAmountCents())
                        : coursePriceCents;
                
                // Calculate teacher revenue (80%) and admin commission (20%)
                long teacherRevenueCents = Math.round(courseAmountCents * TEACHER_REVENUE_PERCENT);
                long adminCommissionCents = courseAmountCents - teacherRevenueCents;
                
                // Check if revenue already exists (idempotent)
                if (revenueRepo.findByPaymentIdAndCourseId(payment.getId(), course.getId()).isPresent()) {
                    log.debug("Revenue already exists for payment {} and course {}", payment.getId(), course.getId());
                    continue;
                }
                
                // Find enrollment for this course and payment
                Long enrollmentId = null;
                try {
                    com.hokori.web.entity.Enrollment enrollment = enrollmentRepo
                            .findByUserIdAndCourseId(payment.getUserId(), course.getId())
                            .orElse(null);
                    if (enrollment != null && enrollment.getCreatedAt() != null) {
                        // Check if enrollment was created around the same time as payment
                        long timeDiff = Math.abs(enrollment.getCreatedAt().toEpochMilli() - 
                                                payment.getPaidAt().toEpochMilli());
                        if (timeDiff < 60000) { // Within 1 minute
                            enrollmentId = enrollment.getId();
                        }
                    }
                } catch (Exception e) {
                    log.warn("Could not find enrollment for payment {} and course {}", payment.getId(), course.getId());
                }
                
                // Create revenue record
                TeacherRevenue revenue = TeacherRevenue.builder()
                        .teacherId(course.getUserId())
                        .courseId(course.getId())
                        .paymentId(payment.getId())
                        .enrollmentId(enrollmentId)
                        .totalAmountCents(payment.getAmountCents())
                        .coursePriceCents(coursePriceCents)
                        .teacherRevenueCents(teacherRevenueCents)
                        .adminCommissionCents(adminCommissionCents)
                        .yearMonth(yearMonth)
                        .paidAt(payment.getPaidAt() != null ? payment.getPaidAt() : Instant.now())
                        .isPaid(false) // Chưa chuyển tiền
                        .build();
                
                revenueRepo.save(revenue);
                log.info("Created revenue record: teacherId={}, courseId={}, paymentId={}, " +
                         "teacherRevenue={} cents, adminCommission={} cents",
                        course.getUserId(), course.getId(), payment.getId(),
                        teacherRevenueCents, adminCommissionCents);
            }
            
        } catch (Exception e) {
            log.error("Error creating revenue from payment {}", payment.getId(), e);
            // Don't throw - payment already succeeded, revenue can be created later
        }
    }
    
    /**
     * Parse course IDs from JSON string
     */
    private List<Long> parseCourseIds(String courseIdsJson) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(courseIdsJson, 
                    mapper.getTypeFactory().constructCollectionType(List.class, Long.class));
        } catch (Exception e) {
            log.error("Error parsing courseIds JSON: {}", courseIdsJson, e);
            return List.of();
        }
    }
    
    /**
     * Đánh dấu revenue đã được chuyển tiền (admin action)
     */
    @Transactional
    public void markRevenueAsPaid(List<Long> revenueIds, Long adminUserId, String note) {
        List<TeacherRevenue> revenues = revenueRepo.findAllById(revenueIds);
        
        if (revenues.isEmpty()) {
            throw new RuntimeException("No revenue records found");
        }
        
        Instant now = Instant.now();
        for (TeacherRevenue revenue : revenues) {
            if (Boolean.TRUE.equals(revenue.getIsPaid())) {
                log.warn("Revenue {} already marked as paid, skipping", revenue.getId());
                continue;
            }
            
            revenue.setIsPaid(true);
            revenue.setPayoutDate(now);
            revenue.setPayoutByUserId(adminUserId);
            revenue.setPayoutNote(note);
            revenueRepo.save(revenue);
            
            log.info("Marked revenue {} as paid by admin {}", revenue.getId(), adminUserId);
        }
    }
    
    /**
     * Đánh dấu tất cả revenue chưa được chuyển tiền của teacher trong tháng là đã chuyển
     */
    @Transactional
    public void markTeacherMonthRevenueAsPaid(Long teacherId, String yearMonth, Long adminUserId, String note) {
        List<TeacherRevenue> revenues = revenueRepo
                .findByTeacherIdAndYearMonthAndIsPaidFalseOrderByPaidAtDesc(teacherId, yearMonth);
        
        if (revenues.isEmpty()) {
            throw new RuntimeException("No unpaid revenue found for teacher " + teacherId + " in " + yearMonth);
        }
        
        markRevenueAsPaid(revenues.stream().map(TeacherRevenue::getId).toList(), adminUserId, note);
    }
}

