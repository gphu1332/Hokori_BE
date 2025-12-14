package com.hokori.web.service;

import com.hokori.web.Enum.ApprovalStatus;
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

import java.time.Instant;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service để teacher quản lý revenue và bank account
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TeacherRevenueService {
    
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    
    private final UserRepository userRepo;
    private final TeacherRevenueRepository revenueRepo;
    private final CourseRepository courseRepo;
    
    /**
     * Cập nhật thông tin tài khoản ngân hàng của teacher
     * Chỉ được phép khi teacher đã được APPROVED
     */
    @Transactional
    public void updateBankAccount(Long teacherId, TeacherBankAccountUpdateReq req) {
        User teacher = userRepo.findById(teacherId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));
        
        // Check if teacher is approved
        if (teacher.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "Only approved teachers can update bank account information");
        }
        
        // Update bank account info
        teacher.setBankAccountNumber(req.getBankAccountNumber());
        teacher.setBankAccountName(req.getBankAccountName());
        teacher.setBankName(req.getBankName());
        teacher.setBankBranchName(req.getBankBranchName());
        
        userRepo.save(teacher);
        log.info("Updated bank account for teacher {}", teacherId);
    }
    
    /**
     * Lấy trạng thái thanh toán của teacher trong tháng
     */
    public com.hokori.web.dto.revenue.PayoutStatusRes getPayoutStatus(Long teacherId, String yearMonth) {
        // Validate yearMonth format
        try {
            YearMonth.parse(yearMonth, YEAR_MONTH_FORMATTER);
        } catch (Exception e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, 
                    "Invalid yearMonth format. Expected format: YYYY-MM (e.g., 2025-01)");
        }
        
        User teacher = userRepo.findById(teacherId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Teacher not found"));
        
        List<TeacherRevenue> revenues = revenueRepo
                .findByTeacherIdAndYearMonthOrderByPaidAtDesc(teacherId, yearMonth);
        
        if (revenues.isEmpty()) {
            return PayoutStatusRes.builder()
                    .yearMonth(yearMonth)
                    .totalRevenueCents(0L)
                    .paidRevenueCents(0L)
                    .unpaidRevenueCents(0L)
                    .payoutStatus("PENDING")
                    .isFullyPaid(false)
                    .totalSales(0)
                    .paidSales(0)
                    .unpaidSales(0)
                    .bankAccountNumber(teacher.getBankAccountNumber())
                    .bankAccountName(teacher.getBankAccountName())
                    .bankName(teacher.getBankName())
                    .bankBranchName(teacher.getBankBranchName())
                    .build();
        }
        
        // Calculate totals
        long totalRevenueCents = revenues.stream()
                .mapToLong(TeacherRevenue::getTeacherRevenueCents)
                .sum();
        
        List<TeacherRevenue> paidRevenues = revenues.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsPaid()))
                .toList();
        
        long paidRevenueCents = paidRevenues.stream()
                .mapToLong(TeacherRevenue::getTeacherRevenueCents)
                .sum();
        
        long unpaidRevenueCents = totalRevenueCents - paidRevenueCents;
        
        // Determine payout status
        String payoutStatus;
        boolean isFullyPaid = unpaidRevenueCents == 0;
        if (isFullyPaid && !revenues.isEmpty()) {
            payoutStatus = "FULLY_PAID";
        } else if (paidRevenueCents > 0) {
            payoutStatus = "PARTIALLY_PAID";
        } else {
            payoutStatus = "PENDING";
        }
        
        // Get last payout date and note
        Instant lastPayoutDate = paidRevenues.stream()
                .filter(r -> r.getPayoutDate() != null)
                .map(TeacherRevenue::getPayoutDate)
                .max(Comparator.naturalOrder())
                .orElse(null);
        
        String lastPayoutNote = paidRevenues.stream()
                .filter(r -> r.getPayoutDate() != null && r.getPayoutNote() != null)
                .max(Comparator.comparing(TeacherRevenue::getPayoutDate))
                .map(TeacherRevenue::getPayoutNote)
                .orElse(null);
        
        return PayoutStatusRes.builder()
                .yearMonth(yearMonth)
                .totalRevenueCents(totalRevenueCents)
                .paidRevenueCents(paidRevenueCents)
                .unpaidRevenueCents(unpaidRevenueCents)
                .payoutStatus(payoutStatus)
                .isFullyPaid(isFullyPaid)
                .lastPayoutDate(lastPayoutDate)
                .lastPayoutNote(lastPayoutNote)
                .totalSales(revenues.size())
                .paidSales(paidRevenues.size())
                .unpaidSales(revenues.size() - paidRevenues.size())
                .bankAccountNumber(teacher.getBankAccountNumber())
                .bankAccountName(teacher.getBankAccountName())
                .bankName(teacher.getBankName())
                .bankBranchName(teacher.getBankBranchName())
                .build();
    }
    
    /**
     * Lấy tổng hợp revenue của teacher theo tháng với filter
     */
    public TeacherRevenueSummaryRes getRevenueSummary(Long teacherId, TeacherRevenueFilterReq filter) {
        String yearMonth = filter.getYearMonth();
        if (yearMonth == null || yearMonth.trim().isEmpty()) {
            yearMonth = YearMonth.now().format(YEAR_MONTH_FORMATTER);
        } else {
            // Handle both formats: "2025" (year only) or "2025-12" (year-month)
            yearMonth = yearMonth.trim();
            if (yearMonth.matches("^\\d{4}$")) {
                // Only year provided (e.g., "2025"), use current month
                int year = Integer.parseInt(yearMonth);
                YearMonth currentYearMonth = YearMonth.now();
                if (year == currentYearMonth.getYear()) {
                    // Same year, use current month
                    yearMonth = currentYearMonth.format(YEAR_MONTH_FORMATTER);
                } else {
                    // Different year, use December of that year
                    yearMonth = YearMonth.of(year, 12).format(YEAR_MONTH_FORMATTER);
                }
            } else {
                // Validate yearMonth format (YYYY-MM)
                try {
                    YearMonth.parse(yearMonth, YEAR_MONTH_FORMATTER);
                } catch (Exception e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                            "Invalid yearMonth format. Expected format: YYYY (e.g., 2025) or YYYY-MM (e.g., 2025-12)");
                }
            }
        }
        
        List<TeacherRevenue> revenues = revenueRepo.findByTeacherIdAndYearMonthOrderByPaidAtDesc(
                teacherId, yearMonth);
        
        if (revenues.isEmpty()) {
            return TeacherRevenueSummaryRes.builder()
                    .yearMonth(yearMonth)
                    .totalRevenueCents(0L)
                    .unpaidRevenueCents(0L)
                    .paidRevenueCents(0L)
                    .totalSales(0)
                    .courses(List.of())
                    .build();
        }
        
        // Apply filter
        List<TeacherRevenue> filteredRevenues = revenues;
        if (filter.getIsPaid() != null) {
            filteredRevenues = revenues.stream()
                    .filter(r -> filter.getIsPaid().equals(r.getIsPaid()))
                    .collect(Collectors.toList());
        }
        if (filter.getCourseId() != null) {
            // Verify course belongs to teacher (security check)
            Course course = courseRepo.findById(filter.getCourseId()).orElse(null);
            if (course == null) {
                log.warn("Course {} not found when filtering revenue for teacherId={}", 
                        filter.getCourseId(), teacherId);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                        "Course not found");
            }
            if (!course.getUserId().equals(teacherId)) {
                log.warn("Course {} (owned by userId={}) does not belong to teacherId={}", 
                        filter.getCourseId(), course.getUserId(), teacherId);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                        "You are not the owner of this course");
            }
            filteredRevenues = filteredRevenues.stream()
                    .filter(r -> r.getCourseId().equals(filter.getCourseId()))
                    .collect(Collectors.toList());
        }
        
        // Calculate totals
        long totalRevenueCents = filteredRevenues.stream()
                .mapToLong(TeacherRevenue::getTeacherRevenueCents)
                .sum();
        
        List<TeacherRevenue> paidRevenues = filteredRevenues.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsPaid()))
                .collect(Collectors.toList());
        
        List<TeacherRevenue> unpaidRevenues = filteredRevenues.stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsPaid()))
                .collect(Collectors.toList());
        
        long unpaidRevenueCents = unpaidRevenues.stream()
                .mapToLong(TeacherRevenue::getTeacherRevenueCents)
                .sum();
        
        long paidRevenueCents = paidRevenues.stream()
                .mapToLong(TeacherRevenue::getTeacherRevenueCents)
                .sum();
        
        // Group by course
        Map<Long, List<TeacherRevenue>> revenuesByCourse = filteredRevenues.stream()
                .collect(Collectors.groupingBy(TeacherRevenue::getCourseId));
        
        List<CourseRevenueRes> courses = new ArrayList<>();
        for (Map.Entry<Long, List<TeacherRevenue>> entry : revenuesByCourse.entrySet()) {
            Long courseId = entry.getKey();
            List<TeacherRevenue> courseRevenues = entry.getValue();
            
            Course course = courseRepo.findById(courseId).orElse(null);
            String courseTitle = course != null ? course.getTitle() : "Unknown Course";
            
            long courseRevenueCents = courseRevenues.stream()
                    .mapToLong(TeacherRevenue::getTeacherRevenueCents)
                    .sum();
            
            List<TeacherRevenue> coursePaidRevenues = courseRevenues.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getIsPaid()))
                    .collect(Collectors.toList());
            
            long coursePaidRevenueCents = coursePaidRevenues.stream()
                    .mapToLong(TeacherRevenue::getTeacherRevenueCents)
                    .sum();
            
            long courseUnpaidRevenueCents = courseRevenueCents - coursePaidRevenueCents;
            
            boolean isFullyPaid = courseUnpaidRevenueCents == 0 && !courseRevenues.isEmpty();
            
            String payoutStatus;
            if (isFullyPaid) {
                payoutStatus = "FULLY_PAID";
            } else if (coursePaidRevenueCents > 0) {
                payoutStatus = "PARTIALLY_PAID";
            } else {
                payoutStatus = "PENDING";
            }
            
            Instant lastPayoutDate = coursePaidRevenues.stream()
                    .filter(r -> r.getPayoutDate() != null)
                    .map(TeacherRevenue::getPayoutDate)
                    .max(Comparator.naturalOrder())
                    .orElse(null);
            
            courses.add(CourseRevenueRes.builder()
                    .courseId(courseId)
                    .courseTitle(courseTitle)
                    .revenueCents(courseRevenueCents)
                    .paidRevenueCents(coursePaidRevenueCents)
                    .unpaidRevenueCents(courseUnpaidRevenueCents)
                    .salesCount(courseRevenues.size())
                    .paidSalesCount(coursePaidRevenues.size())
                    .unpaidSalesCount(courseRevenues.size() - coursePaidRevenues.size())
                    .isFullyPaid(isFullyPaid)
                    .lastPayoutDate(lastPayoutDate)
                    .payoutStatus(payoutStatus)
                    .build());
        }
        
        // Determine overall payout status
        boolean isFullyPaid = unpaidRevenueCents == 0 && !filteredRevenues.isEmpty();
        String payoutStatus;
        if (isFullyPaid) {
            payoutStatus = "FULLY_PAID";
        } else if (paidRevenueCents > 0) {
            payoutStatus = "PARTIALLY_PAID";
        } else {
            payoutStatus = "PENDING";
        }
        
        Instant lastPayoutDate = paidRevenues.stream()
                .filter(r -> r.getPayoutDate() != null)
                .map(TeacherRevenue::getPayoutDate)
                .max(Comparator.naturalOrder())
                .orElse(null);
        
        return TeacherRevenueSummaryRes.builder()
                .yearMonth(yearMonth)
                .totalRevenueCents(totalRevenueCents)
                .unpaidRevenueCents(unpaidRevenueCents)
                .paidRevenueCents(paidRevenueCents)
                .totalSales(filteredRevenues.size())
                .paidSales(paidRevenues.size())
                .unpaidSales(unpaidRevenues.size())
                .isFullyPaid(isFullyPaid)
                .lastPayoutDate(lastPayoutDate)
                .payoutStatus(payoutStatus)
                .courses(courses)
                .build();
    }
    
    /**
     * Lấy danh sách revenue của teacher (tất cả các tháng)
     */
    public List<TeacherRevenueSummaryRes> getAllRevenueSummaries(Long teacherId) {
        List<TeacherRevenue> allRevenues = revenueRepo.findByTeacherIdOrderByYearMonthDescPaidAtDesc(teacherId);
        
        // Group by yearMonth
        Map<String, List<TeacherRevenue>> revenuesByMonth = allRevenues.stream()
                .collect(Collectors.groupingBy(TeacherRevenue::getYearMonth));
        
        List<TeacherRevenueSummaryRes> summaries = new ArrayList<>();
        for (Map.Entry<String, List<TeacherRevenue>> entry : revenuesByMonth.entrySet()) {
            String yearMonth = entry.getKey();
            List<TeacherRevenue> revenues = entry.getValue();
            
            long totalRevenueCents = revenues.stream()
                    .mapToLong(TeacherRevenue::getTeacherRevenueCents)
                    .sum();
            
            long unpaidRevenueCents = revenues.stream()
                    .filter(r -> !r.getIsPaid())
                    .mapToLong(TeacherRevenue::getTeacherRevenueCents)
                    .sum();
            
            long paidRevenueCents = totalRevenueCents - unpaidRevenueCents;
            
            // Group by course
            Map<Long, List<TeacherRevenue>> revenuesByCourse = revenues.stream()
                    .collect(Collectors.groupingBy(TeacherRevenue::getCourseId));
            
            List<CourseRevenueRes> courses = new ArrayList<>();
            for (Map.Entry<Long, List<TeacherRevenue>> courseEntry : revenuesByCourse.entrySet()) {
                Long courseId = courseEntry.getKey();
                List<TeacherRevenue> courseRevenues = courseEntry.getValue();
                
                Course course = courseRepo.findById(courseId).orElse(null);
                String courseTitle = course != null ? course.getTitle() : "Unknown Course";
                
                long courseRevenueCents = courseRevenues.stream()
                        .mapToLong(TeacherRevenue::getTeacherRevenueCents)
                        .sum();
                
                boolean isPaid = courseRevenues.stream().allMatch(TeacherRevenue::getIsPaid);
                
                courses.add(CourseRevenueRes.builder()
                        .courseId(courseId)
                        .courseTitle(courseTitle)
                        .revenueCents(courseRevenueCents)
                        .salesCount(courseRevenues.size())
                        .isPaid(isPaid) // For backward compatibility
                        .isFullyPaid(isPaid)
                        .payoutStatus(isPaid ? "FULLY_PAID" : "PENDING")
                        .build());
            }
            
            summaries.add(TeacherRevenueSummaryRes.builder()
                    .yearMonth(yearMonth)
                    .totalRevenueCents(totalRevenueCents)
                    .unpaidRevenueCents(unpaidRevenueCents)
                    .paidRevenueCents(paidRevenueCents)
                    .totalSales(revenues.size())
                    .courses(courses)
                    .build());
        }
        
        return summaries;
    }
    
    /**
     * Lấy chi tiết revenue của một course trong tháng
     */
    public List<TeacherRevenueRes> getCourseRevenueDetails(Long teacherId, Long courseId, String yearMonth) {
        List<TeacherRevenue> revenues = revenueRepo.findByCourseIdAndYearMonthOrderByPaidAtDesc(courseId, yearMonth);
        
        // Filter by teacher (security check)
        revenues = revenues.stream()
                .filter(r -> r.getTeacherId().equals(teacherId))
                .collect(Collectors.toList());
        
        Course course = courseRepo.findById(courseId).orElse(null);
        String courseTitle = course != null ? course.getTitle() : "Unknown Course";
        
        return revenues.stream()
                .map(r -> TeacherRevenueRes.builder()
                        .id(r.getId())
                        .courseId(r.getCourseId())
                        .courseTitle(courseTitle)
                        .paymentId(r.getPaymentId())
                        .enrollmentId(r.getEnrollmentId())
                        .totalAmountCents(r.getTotalAmountCents())
                        .coursePriceCents(r.getCoursePriceCents())
                        .teacherRevenueCents(r.getTeacherRevenueCents())
                        .adminCommissionCents(r.getAdminCommissionCents())
                        .yearMonth(r.getYearMonth())
                        .paidAt(r.getPaidAt())
                        .isPaid(r.getIsPaid())
                        .payoutDate(r.getPayoutDate())
                        .payoutByUserId(r.getPayoutByUserId())
                        .payoutNote(r.getPayoutNote())
                        .createdAt(r.getCreatedAt())
                        .updatedAt(r.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}

