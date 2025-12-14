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
        
        // Get all unpaid revenues grouped by teacher and yearMonth
        List<Object[]> groupedRevenues = revenueRepo.findUnpaidRevenueGroupedByTeacherAndMonth();
        
        // Filter by yearMonth if specified
        Map<Long, AdminPendingPayoutRes> teacherMap = new LinkedHashMap<>();
        
        for (Object[] row : groupedRevenues) {
            Long teacherId = ((Number) row[0]).longValue();
            String revenueYearMonth = (String) row[1];
            
            // Filter by yearMonth
            if (!yearMonth.equals(revenueYearMonth)) {
                continue;
            }
            
            User teacher = userRepo.findById(teacherId).orElse(null);
            if (teacher == null) {
                continue;
            }
            
            // Get unpaid revenues for this teacher and month
            List<TeacherRevenue> unpaidRevenues = revenueRepo
                    .findByTeacherIdAndYearMonthAndIsPaidFalseOrderByPaidAtDesc(teacherId, yearMonth);
            
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
                
                courses.add(CourseRevenueRes.builder()
                        .courseId(courseId)
                        .courseTitle(courseTitle)
                        .revenueCents(courseRevenueCents)
                        .paidRevenueCents(0L)
                        .unpaidRevenueCents(courseRevenueCents)
                        .salesCount(courseRevenues.size())
                        .paidSalesCount(0)
                        .unpaidSalesCount(courseRevenues.size())
                        .isFullyPaid(false)
                        .payoutStatus("PENDING")
                        .build());
            }
            
            // Skip teachers who only have free courses (no revenue to pay)
            if (totalPendingRevenueCents == 0 || courses.isEmpty()) {
                continue;
            }
            
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
                    .courses(courses)
                    .build());
        }
        
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
                .findByTeacherIdAndYearMonthAndIsPaidFalseOrderByPaidAtDesc(teacherId, yearMonth);
        
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
            
            courses.add(CourseRevenueRes.builder()
                    .courseId(courseId)
                    .courseTitle(courseTitle)
                    .revenueCents(courseRevenueCents)
                    .paidRevenueCents(0L)
                    .unpaidRevenueCents(courseRevenueCents)
                    .salesCount(courseRevenues.size())
                    .paidSalesCount(0)
                    .unpaidSalesCount(courseRevenues.size())
                    .isFullyPaid(false)
                    .payoutStatus("PENDING")
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
                .courses(courses)
                .build();
    }
    
    /**
     * Đánh dấu revenue đã được chuyển tiền
     */
    @Transactional
    public void markPayoutAsPaid(MarkPayoutPaidReq req, Long adminUserId) {
        if (req.getRevenueIds() != null && !req.getRevenueIds().isEmpty()) {
            // Option 1: Đánh dấu theo danh sách revenue IDs
            revenueService.markRevenueAsPaid(req.getRevenueIds(), adminUserId, req.getNote());
        } else if (req.getTeacherId() != null && req.getYearMonth() != null) {
            // Option 2: Đánh dấu tất cả revenue chưa được chuyển tiền của teacher trong tháng
            revenueService.markTeacherMonthRevenueAsPaid(
                    req.getTeacherId(), req.getYearMonth(), adminUserId, req.getNote());
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Either revenueIds or (teacherId + yearMonth) must be provided");
        }
    }
    
    /**
     * Tính tổng admin commission trong tháng
     */
    public Long getAdminCommissionForMonth(String yearMonth) {
        // Validate yearMonth format
        try {
            YearMonth.parse(yearMonth, YEAR_MONTH_FORMATTER);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Invalid yearMonth format. Expected format: YYYY-MM (e.g., 2025-01)");
        }
        
        List<TeacherRevenue> revenues = revenueRepo.findAll().stream()
                .filter(r -> r.getYearMonth().equals(yearMonth))
                .collect(Collectors.toList());
        
        return revenues.stream()
                .mapToLong(TeacherRevenue::getAdminCommissionCents)
                .sum();
    }
}

