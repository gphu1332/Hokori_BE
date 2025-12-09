package com.hokori.web.service;

import com.hokori.web.Enum.CourseStatus;
import com.hokori.web.Enum.WalletTransactionSource;
import com.hokori.web.Enum.WalletTransactionStatus;
import com.hokori.web.dto.dashboard.RecentCourseSummaryDto;
import com.hokori.web.dto.dashboard.TeacherDashboardSummaryRes;
import com.hokori.web.dto.revenue.CourseRevenueRes;
import com.hokori.web.dto.revenue.TeacherRevenueRes;
import com.hokori.web.entity.Course;
import com.hokori.web.entity.WalletTransaction;
import com.hokori.web.repository.CourseRepository;
import com.hokori.web.repository.EnrollmentRepository;
import com.hokori.web.repository.UserRepository;
import com.hokori.web.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherDashboardService {

    private final CourseRepository courseRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final WalletTransactionRepository walletTxRepo;
    private final CurrentUserService currentUser;
    private final UserRepository userRepo;

    public TeacherDashboardSummaryRes getOverview() {
        Long teacherId = currentUser.getUserIdOrThrow();

        // ===== 1. Active students =====
        long activeStudents = enrollmentRepo.countActiveStudentsByTeacher(
                teacherId,
                CourseStatus.PUBLISHED
        );

        // TODO: nếu sau này có bảng thống kê tháng trước thì mới tính % change
        long activeStudentsChangePercent = 0;

        // ===== 2. Published courses & drafts =====
        long publishedCourses = courseRepo.countByUserIdAndStatusAndDeletedFlagFalse(
                teacherId, CourseStatus.PUBLISHED);

        long draftsWaitingReview = courseRepo.countByUserIdAndStatusAndDeletedFlagFalse(
                teacherId, CourseStatus.PENDING_APPROVAL);

        // ===== 3. Monthly revenue (tháng hiện tại) =====
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        YearMonth currentMonth = YearMonth.now(zone);

        ZonedDateTime fromZdt = currentMonth.atDay(1).atStartOfDay(zone);
        ZonedDateTime toZdt   = currentMonth.plusMonths(1).atDay(1).atStartOfDay(zone);

        Long revenueCents = walletTxRepo.sumIncomeForPeriod(
                teacherId,
                WalletTransactionStatus.COMPLETED,
                WalletTransactionSource.COURSE_SALE,
                fromZdt.toInstant(),
                toZdt.toInstant()
        );
        if (revenueCents == null) revenueCents = 0L;

        // VND trực tiếp, không chia 100
        BigDecimal monthlyRevenue = BigDecimal.valueOf(revenueCents);

        // ===== 4. Ngày payout tiếp theo =====
        LocalDate nextPayoutDate = calculateNextPayoutDate(zone);

        // ===== 5. Recent courses =====
        var pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "updatedAt"));

        List<RecentCourseSummaryDto> recentCourses = courseRepo
                .findByUserIdAndDeletedFlagFalse(teacherId, pageable)
                .map(this::toRecentCourseDto)
                .getContent();

        // ===== 6. New comments (chưa triển khai) =====
        long newComments = 0; // để 0 tạm, sau này mình có thể nối với CourseComment

        return new TeacherDashboardSummaryRes(
                activeStudents,
                activeStudentsChangePercent,
                publishedCourses,
                draftsWaitingReview,
                monthlyRevenue,
                nextPayoutDate,
                newComments,
                recentCourses
        );
    }

    private RecentCourseSummaryDto toRecentCourseDto(Course c) {
        long students = c.getEnrollCount() != null ? c.getEnrollCount() : 0L;
        Double rating = c.getRatingAvg();

        // dùng slug làm "code" hiển thị nếu bạn không có courseCode riêng
        String code = c.getSlug();

        return new RecentCourseSummaryDto(
                c.getId(),
                c.getTitle(),
                code,
                students,
                rating,
                c.getStatus(),
                c.getUpdatedAt()
        );
    }

    /**
     * Rule payout: ví dụ 15 và 30 hàng tháng (bạn có thể chỉnh lại theo BA).
     * Handle months with 28/29/30/31 days correctly.
     */
    private LocalDate calculateNextPayoutDate(ZoneId zone) {
        LocalDate today = LocalDate.now(zone);

        int day = today.getDayOfMonth();
        int daysInMonth = today.lengthOfMonth(); // Get actual days in current month
        
        if (day < 15) {
            return LocalDate.of(today.getYear(), today.getMonth(), 15);
        }
        
        // Use last day of month if month has less than 30 days, otherwise use 30
        int payoutDay = Math.min(30, daysInMonth);
        if (day < payoutDay) {
            return LocalDate.of(today.getYear(), today.getMonth(), payoutDay);
        }
        
        // After last payout day, jump to 15th of next month
        LocalDate nextMonth = today.plusMonths(1);
        return LocalDate.of(nextMonth.getYear(), nextMonth.getMonth(), 15);
    }

    /**
     * Teacher xem revenue theo tháng cụ thể
     */
    public TeacherRevenueRes getRevenueByMonth(Integer year, Integer month) {
        Long teacherId = currentUser.getUserIdOrThrow();
        
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        YearMonth targetMonth;
        if (year != null && month != null) {
            targetMonth = YearMonth.of(year, month);
        } else {
            targetMonth = YearMonth.now(zone);
        }

        ZonedDateTime fromZdt = targetMonth.atDay(1).atStartOfDay(zone);
        ZonedDateTime toZdt = targetMonth.plusMonths(1).atDay(1).atStartOfDay(zone);

        // Get revenue for the period
        Long revenueCents = walletTxRepo.sumIncomeForPeriod(
                teacherId,
                WalletTransactionStatus.COMPLETED,
                WalletTransactionSource.COURSE_SALE,
                fromZdt.toInstant(),
                toZdt.toInstant()
        );
        if (revenueCents == null) revenueCents = 0L;

        // Get all transactions for the period (with Course loaded)
        List<WalletTransaction> transactions = walletTxRepo
                .findByUser_IdWithCourseOrderByCreatedAtDesc(teacherId)
                .stream()
                .filter(tx -> tx.getStatus() == WalletTransactionStatus.COMPLETED 
                        && tx.getSource() == WalletTransactionSource.COURSE_SALE
                        && tx.getCreatedAt().isAfter(fromZdt.toInstant())
                        && tx.getCreatedAt().isBefore(toZdt.toInstant()))
                .collect(Collectors.toList());

        List<TeacherRevenueRes.TransactionDetail> transactionDetails = transactions.stream().map(tx -> {
            return TeacherRevenueRes.TransactionDetail.builder()
                    .id(tx.getId())
                    .amountCents(tx.getAmountCents())
                    .amount(BigDecimal.valueOf(tx.getAmountCents())) // VND trực tiếp, không chia 100
                    .courseId(tx.getCourse() != null ? tx.getCourse().getId() : null)
                    .courseTitle(tx.getCourse() != null ? tx.getCourse().getTitle() : "N/A")
                    .description(tx.getDescription())
                    .createdAt(tx.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());

        // Get wallet balance
        Long walletBalance = userRepo.findById(teacherId)
                .map(user -> user.getWalletBalance() != null ? user.getWalletBalance() : 0L)
                .orElse(0L);

        return TeacherRevenueRes.builder()
                .period(targetMonth.toString())
                .revenueCents(revenueCents)
                .revenue(BigDecimal.valueOf(revenueCents)) // VND trực tiếp, không chia 100
                .transactionCount(transactions.size())
                .walletBalance(walletBalance)
                .transactions(transactionDetails)
                .build();
    }

    /**
     * Teacher xem revenue từ một course cụ thể trong tháng
     */
    public CourseRevenueRes getCourseRevenue(Long courseId, Integer year, Integer month) {
        Long teacherId = currentUser.getUserIdOrThrow();
        
        // Verify course belongs to teacher
        Course course = courseRepo.findByIdAndDeletedFlagFalse(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        
        if (!course.getUserId().equals(teacherId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of this course");
        }

        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        YearMonth targetMonth;
        if (year != null && month != null) {
            targetMonth = YearMonth.of(year, month);
        } else {
            targetMonth = YearMonth.now(zone);
        }

        ZonedDateTime fromZdt = targetMonth.atDay(1).atStartOfDay(zone);
        ZonedDateTime toZdt = targetMonth.plusMonths(1).atDay(1).atStartOfDay(zone);

        // Get all transactions for the period, filtered by course (with Course loaded)
        List<WalletTransaction> transactions = walletTxRepo
                .findByUser_IdWithCourseOrderByCreatedAtDesc(teacherId)
                .stream()
                .filter(tx -> tx.getStatus() == WalletTransactionStatus.COMPLETED 
                        && tx.getSource() == WalletTransactionSource.COURSE_SALE
                        && tx.getCourse() != null
                        && tx.getCourse().getId().equals(courseId)
                        && tx.getCreatedAt().isAfter(fromZdt.toInstant())
                        && tx.getCreatedAt().isBefore(toZdt.toInstant()))
                .collect(Collectors.toList());

        Long revenueCents = transactions.stream()
                .mapToLong(WalletTransaction::getAmountCents)
                .sum();

        List<CourseRevenueRes.TransactionDetail> transactionDetails = transactions.stream().map(tx -> {
            return CourseRevenueRes.TransactionDetail.builder()
                    .id(tx.getId())
                    .amountCents(tx.getAmountCents())
                    .amount(BigDecimal.valueOf(tx.getAmountCents())) // VND trực tiếp, không chia 100
                    .description(tx.getDescription())
                    .createdAt(tx.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());

        // Get teacher name
        String teacherName = userRepo.findById(teacherId)
                .map(user -> user.getDisplayName() != null && !user.getDisplayName().isEmpty()
                        ? user.getDisplayName()
                        : user.getUsername() != null ? user.getUsername() : user.getEmail())
                .orElse("N/A");

        return CourseRevenueRes.builder()
                .teacherId(teacherId)
                .teacherName(teacherName)
                .courseId(courseId)
                .courseTitle(course.getTitle())
                .period(targetMonth.toString())
                .revenueCents(revenueCents)
                .revenue(BigDecimal.valueOf(revenueCents)) // VND trực tiếp, không chia 100
                .transactionCount(transactions.size())
                .transactions(transactionDetails)
                .build();
    }
}
