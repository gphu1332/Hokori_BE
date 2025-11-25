package com.hokori.web.service;

import com.hokori.web.Enum.CourseStatus;
import com.hokori.web.Enum.WalletTransactionSource;
import com.hokori.web.Enum.WalletTransactionStatus;
import com.hokori.web.dto.dashboard.RecentCourseSummaryDto;
import com.hokori.web.dto.dashboard.TeacherDashboardSummaryRes;
import com.hokori.web.entity.Course;
import com.hokori.web.repository.CourseRepository;
import com.hokori.web.repository.EnrollmentRepository;
import com.hokori.web.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherDashboardService {

    private final CourseRepository courseRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final WalletTransactionRepository walletTxRepo;
    private final CurrentUserService currentUser;

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

        // chuyển từ cents -> tiền (VD dùng VND thì bạn có thể để nguyên cents hoặc chia 100)
        BigDecimal monthlyRevenue = BigDecimal.valueOf(revenueCents)
                .movePointLeft(2); // 100 cents = 1 đơn vị tiền

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
}
