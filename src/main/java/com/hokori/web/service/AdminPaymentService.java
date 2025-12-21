package com.hokori.web.service;

import com.hokori.web.dto.revenue.*;
import com.hokori.web.entity.Course;
import com.hokori.web.entity.TeacherRevenue;
import com.hokori.web.entity.User;
import com.hokori.web.repository.CourseRepository;
import com.hokori.web.repository.TeacherRevenueRepository;
import com.hokori.web.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service để admin quản lý thanh toán cho teachers
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminPaymentService {
    
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    
    private final TeacherRevenueRepository revenueRepo;
    private final UserRepository userRepo;
    private final CourseRepository courseRepo;
    private final RevenueService revenueService;
    
    /**
     * Lấy danh sách tất cả teachers có revenue chưa được chuyển tiền
     * Group by teacher và yearMonth
     */
    public List<AdminPendingPayoutRes> getPendingPayouts(String yearMonth) {
        // If yearMonth is null, get current month
        if (yearMonth == null || yearMonth.trim().isEmpty()) {
            yearMonth = YearMonth.now().format(YEAR_MONTH_FORMATTER);
        }
        
        // Validate yearMonth format
        try {
            YearMonth.parse(yearMonth, YEAR_MONTH_FORMATTER);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Invalid yearMonth format. Expected format: YYYY-MM (e.g., 2025-01)");
        }
        
        // Optimized: Filter by yearMonth ngay trong SQL query thay vì filter trong code
        List<Object[]> groupedRevenues = revenueRepo.findUnpaidRevenueGroupedByTeacherAndMonthForYearMonth(yearMonth);
        
        log.info("🔍 Querying pending payouts for yearMonth: {}", yearMonth);
        log.info("📊 Found {} teachers with unpaid revenue in month {}", groupedRevenues.size(), yearMonth);
        
        // Debug: Log all found teachers
        if (groupedRevenues.isEmpty()) {
            // Check if there are any unpaid revenues at all (for debugging)
            List<Object[]> allUnpaid = revenueRepo.findUnpaidRevenueGroupedByTeacherAndMonth();
            log.warn("⚠️ No unpaid revenue found for month {}. Total unpaid revenues across all months: {}", 
                    yearMonth, allUnpaid.size());
            if (!allUnpaid.isEmpty()) {
                log.info("📋 Available months with unpaid revenue:");
                for (Object[] row : allUnpaid) {
                    String availableMonth = (String) row[1];
                    Long teacherId = ((Number) row[0]).longValue();
                    log.info("  - Month: {}, TeacherId: {}", availableMonth, teacherId);
                }
            }
        } else {
            for (Object[] row : groupedRevenues) {
                Long teacherId = ((Number) row[0]).longValue();
                String revenueYearMonth = (String) row[1];
                log.debug("  TeacherId: {}, YearMonth: {}", teacherId, revenueYearMonth);
            }
        }
        
        Map<Long, AdminPendingPayoutRes> teacherMap = new LinkedHashMap<>();
        
        for (Object[] row : groupedRevenues) {
            Long teacherId = ((Number) row[0]).longValue();
            String revenueYearMonth = (String) row[1];
            
            // Verify yearMonth matches (should always match since we filter in SQL, but double-check)
            if (!yearMonth.equals(revenueYearMonth)) {
                log.warn("YearMonth mismatch: expected {}, got {} for teacher {}", yearMonth, revenueYearMonth, teacherId);
                continue;
            }
            
            User teacher = userRepo.findById(teacherId).orElse(null);
            if (teacher == null) {
                continue;
            }
            
            // Get unpaid revenues for this teacher and month
            List<TeacherRevenue> unpaidRevenues = revenueRepo
                    .findByTeacher_IdAndYearMonthAndIsPaidFalseOrderByPaidAtDesc(teacherId, yearMonth);
            
            log.debug("  Teacher {}: Found {} unpaid revenue records", teacherId, unpaidRevenues.size());
            
            if (unpaidRevenues.isEmpty()) {
                log.warn("  ⚠️ Teacher {} has no unpaid revenues in month {} (but was in grouped query)", teacherId, yearMonth);
                continue;
            }
            
            // Group by course
            Map<Long, List<TeacherRevenue>> revenuesByCourse = unpaidRevenues.stream()
                    .collect(Collectors.groupingBy(TeacherRevenue::getCourseId));
            
            List<CourseRevenueRes> courses = new ArrayList<>();
            long totalPendingRevenueCents = 0L;
            int totalPendingSales = 0;
            
            for (Map.Entry<Long, List<TeacherRevenue>> entry : revenuesByCourse.entrySet()) {
                Long courseId = entry.getKey();
                List<TeacherRevenue> courseRevenues = entry.getValue();
                
                Course course = courseRepo.findById(courseId).orElse(null);
                String courseTitle = course != null ? course.getTitle() : "Unknown Course";
                
                long courseRevenueCents = courseRevenues.stream()
                        .mapToLong(TeacherRevenue::getTeacherRevenueCents)
                        .sum();
                
                // Skip free courses (revenue = 0) - không cần hiển thị vì không có tiền để trả
                if (courseRevenueCents == 0) {
                    continue;
                }
                
                totalPendingRevenueCents += courseRevenueCents;
                totalPendingSales += courseRevenues.size();
                
                // Tính tổng admin commission và original course price từ TeacherRevenue
                long totalAdminCommissionCents = courseRevenues.stream()
                        .mapToLong(TeacherRevenue::getAdminCommissionCents)
                        .sum();
                
                long totalOriginalCoursePriceCents = courseRevenues.stream()
                        .mapToLong(TeacherRevenue::getCoursePriceCents)
                        .sum();
                
                courses.add(CourseRevenueRes.builder()
                        .courseId(courseId)
                        .courseTitle(courseTitle)
                        .originalCoursePriceCents(totalOriginalCoursePriceCents)
                        .adminCommissionCents(totalAdminCommissionCents)
                        .revenueCents(courseRevenueCents)
                        .paidRevenueCents(0L)
                        .unpaidRevenueCents(courseRevenueCents)
                        .salesCount(courseRevenues.size())
                        .paidSalesCount(0)
                        .unpaidSalesCount(courseRevenues.size())
                        .isFullyPaid(false)
                        // Không set payoutStatus ở level course - admin chỉ cần xem tổng và trạng thái teacher
                        .build());
            }
            
            // Skip teachers who only have free courses (no revenue to pay)
            if (totalPendingRevenueCents == 0 || courses.isEmpty()) {
                log.debug("  ⚠️ Skipping teacher {}: totalPendingRevenueCents={}, courses.size()={}", 
                        teacherId, totalPendingRevenueCents, courses.size());
                continue;
            }
            
            log.info("  ✅ Adding teacher {} to result: totalPendingRevenueCents={}, courses={}", 
                    teacherId, totalPendingRevenueCents, courses.size());
            
            teacherMap.put(teacherId, AdminPendingPayoutRes.builder()
                    .teacherId(teacherId)
                    .teacherName(teacher.getDisplayName() != null ? teacher.getDisplayName() : 
                            ((teacher.getFirstName() != null ? teacher.getFirstName() : "") + " " + 
                             (teacher.getLastName() != null ? teacher.getLastName() : "")).trim())
                    .teacherEmail(teacher.getEmail())
                    .bankAccountNumber(teacher.getBankAccountNumber())
                    .bankAccountName(teacher.getBankAccountName())
                    .bankName(teacher.getBankName())
                    .bankBranchName(teacher.getBankBranchName())
                    .yearMonth(yearMonth)
                    .totalPendingRevenueCents(totalPendingRevenueCents)
                    .totalPendingSales(totalPendingSales)
                    .courseCount(courses.size()) // Số lượng courses có revenue chưa trả
                    .payoutStatus("PENDING") // Luôn là PENDING vì đây là pending payouts
                    .courses(courses)
                    .build());
        }
        
        log.info("📤 Returning {} teachers with pending payouts for month {}", teacherMap.size(), yearMonth);
        return new ArrayList<>(teacherMap.values());
    }
    
    /**
     * Lấy chi tiết revenue chưa được chuyển tiền của một teacher trong tháng
     */
    public AdminPendingPayoutRes getTeacherPendingPayoutDetails(Long teacherId, String yearMonth) {
        // Validate yearMonth format
        try {
            YearMonth.parse(yearMonth, YEAR_MONTH_FORMATTER);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Invalid yearMonth format. Expected format: YYYY-MM (e.g., 2025-01)");
        }
        
        User teacher = userRepo.findById(teacherId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));
        
        List<TeacherRevenue> unpaidRevenues = revenueRepo
                .findByTeacher_IdAndYearMonthAndIsPaidFalseOrderByPaidAtDesc(teacherId, yearMonth);
        
        if (unpaidRevenues.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                    "No pending payout found for teacher " + teacherId + " in " + yearMonth);
        }
        
        // Group by course first
        Map<Long, List<TeacherRevenue>> revenuesByCourse = unpaidRevenues.stream()
                .collect(Collectors.groupingBy(TeacherRevenue::getCourseId));
        
        List<CourseRevenueRes> courses = new ArrayList<>();
        long totalPendingRevenueCents = 0L;
        
        for (Map.Entry<Long, List<TeacherRevenue>> entry : revenuesByCourse.entrySet()) {
            Long courseId = entry.getKey();
            List<TeacherRevenue> courseRevenues = entry.getValue();
            
            Course course = courseRepo.findById(courseId).orElse(null);
            String courseTitle = course != null ? course.getTitle() : "Unknown Course";
            
            long courseRevenueCents = courseRevenues.stream()
                    .mapToLong(TeacherRevenue::getTeacherRevenueCents)
                    .sum();
            
            // Skip free courses (revenue = 0) - không cần hiển thị vì không có tiền để trả
            if (courseRevenueCents == 0) {
                continue;
            }
            
            totalPendingRevenueCents += courseRevenueCents;
            
            // Tính tổng admin commission và original course price từ TeacherRevenue
            long totalAdminCommissionCents = courseRevenues.stream()
                    .mapToLong(TeacherRevenue::getAdminCommissionCents)
                    .sum();
            
            long totalOriginalCoursePriceCents = courseRevenues.stream()
                    .mapToLong(TeacherRevenue::getCoursePriceCents)
                    .sum();
            
            courses.add(CourseRevenueRes.builder()
                    .courseId(courseId)
                    .courseTitle(courseTitle)
                    .originalCoursePriceCents(totalOriginalCoursePriceCents)
                    .adminCommissionCents(totalAdminCommissionCents)
                    .revenueCents(courseRevenueCents)
                    .paidRevenueCents(0L)
                    .unpaidRevenueCents(courseRevenueCents)
                    .salesCount(courseRevenues.size())
                    .paidSalesCount(0)
                    .unpaidSalesCount(courseRevenues.size())
                    .isFullyPaid(false)
                    // Không set payoutStatus ở level course - admin chỉ cần xem tổng và trạng thái teacher
                    .build());
        }
        
        // If after filtering free courses, there's no revenue to pay, return 404
        if (totalPendingRevenueCents == 0 || courses.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                    "No pending payout found for teacher " + teacherId + " in " + yearMonth + 
                    " (only free courses found)");
        }
        
        String teacherName = teacher.getDisplayName() != null ? teacher.getDisplayName() : 
                ((teacher.getFirstName() != null ? teacher.getFirstName() : "") + " " + 
                 (teacher.getLastName() != null ? teacher.getLastName() : "")).trim();
        if (teacherName.isEmpty()) {
            teacherName = teacher.getEmail();
        }
        
        return AdminPendingPayoutRes.builder()
                .teacherId(teacherId)
                .teacherName(teacherName)
                .teacherEmail(teacher.getEmail())
                .bankAccountNumber(teacher.getBankAccountNumber())
                .bankAccountName(teacher.getBankAccountName())
                .bankName(teacher.getBankName())
                .bankBranchName(teacher.getBankBranchName())
                .yearMonth(yearMonth)
                .totalPendingRevenueCents(totalPendingRevenueCents)
                .totalPendingSales(unpaidRevenues.size())
                .courseCount(courses.size()) // Số lượng courses có revenue chưa trả
                .payoutStatus("PENDING") // Luôn là PENDING vì đây là pending payouts
                .courses(courses)
                .build();
    }
    
    /**
     * Đánh dấu revenue đã được chuyển tiền
     * 
     * Business Logic:
     * - Admin chỉ trả tiền một lần vào cuối tháng cho tất cả revenue của teacher trong tháng đó
     * - Khi admin bấm "xác nhận chuyển" ở tổng (teacher level) → tất cả revenue của teacher trong tháng đó → FULLY_PAID
     * - Sang tháng mới thì tính tiếp revenue mới
     * 
     * @param req Request chứa teacherId + yearMonth (recommended) hoặc revenueIds (edge cases)
     * @param adminUserId ID của admin thực hiện đánh dấu
     */
    @Transactional
    public void markPayoutAsPaid(MarkPayoutPaidReq req, Long adminUserId) {
        if (req.getRevenueIds() != null && !req.getRevenueIds().isEmpty()) {
            // Option 1: Đánh dấu theo danh sách revenue IDs cụ thể (chỉ dùng cho edge cases/debugging)
            // ⚠️ Có thể gây PARTIALLY_PAID nếu chỉ đánh dấu một phần revenue
            revenueService.markRevenueAsPaid(req.getRevenueIds(), adminUserId, req.getNote());
        } else if (req.getTeacherId() != null && req.getYearMonth() != null) {
            // Option 2: Đánh dấu TẤT CẢ revenue chưa được chuyển tiền của teacher trong tháng (RECOMMENDED)
            // ✅ Luôn FULLY_PAID vì đánh dấu tất cả revenue của teacher trong tháng đó
            // Đây là cách admin thường dùng: bấm "xác nhận chuyển" ở tổng → trả một lần vào cuối tháng
            revenueService.markTeacherMonthRevenueAsPaid(
                    req.getTeacherId(), req.getYearMonth(), adminUserId, req.getNote());
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Either revenueIds or (teacherId + yearMonth) must be provided");
        }
    }
    
    /**
     * Tính tổng admin commission trong tháng (optimized với JPQL query)
     */
    public Long getAdminCommissionForMonth(String yearMonth) {
        // Validate yearMonth format
        try {
            YearMonth.parse(yearMonth, YEAR_MONTH_FORMATTER);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Invalid yearMonth format. Expected format: YYYY-MM (e.g., 2025-01)");
        }
        
        // Sử dụng JPQL query thay vì load tất cả rồi filter (hiệu quả hơn nhiều)
        return revenueRepo.sumAdminCommissionByYearMonth(yearMonth);
    }
    
    /**
     * Lấy chi tiết admin commission trong tháng
     * Bao gồm: doanh thu dự kiến (chưa trả tiền) và doanh thu đã chuyển tiền
     */
    public AdminCommissionRes getAdminCommissionDetails(String yearMonth) {
        // Validate yearMonth format
        try {
            YearMonth.parse(yearMonth, YEAR_MONTH_FORMATTER);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Invalid yearMonth format. Expected format: YYYY-MM (e.g., 2025-01)");
        }
        
        // Tính doanh thu dự kiến (20% từ revenue chưa được trả tiền)
        Long expectedRevenueCents = revenueRepo.sumUnpaidAdminCommissionByYearMonth(yearMonth);
        if (expectedRevenueCents == null) {
            expectedRevenueCents = 0L;
        }
        
        // Tính doanh thu đã chuyển tiền (20% từ revenue đã được trả tiền)
        Long paidRevenueCents = revenueRepo.sumPaidAdminCommissionByYearMonth(yearMonth);
        if (paidRevenueCents == null) {
            paidRevenueCents = 0L;
        }
        
        // Tổng doanh thu
        Long totalRevenueCents = expectedRevenueCents + paidRevenueCents;
        
        log.info("📊 Admin commission for {}: expected={}, paid={}, total={}", 
                yearMonth, expectedRevenueCents, paidRevenueCents, totalRevenueCents);
        
        return AdminCommissionRes.builder()
                .yearMonth(yearMonth)
                .expectedRevenueCents(expectedRevenueCents)
                .paidRevenueCents(paidRevenueCents)
                .totalRevenueCents(totalRevenueCents)
                .build();
    }
}

