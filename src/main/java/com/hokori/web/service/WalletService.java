// com.hokori.web.service.WalletService.java
package com.hokori.web.service;

import com.hokori.web.Enum.WalletTransactionSource;
import com.hokori.web.Enum.WalletTransactionStatus;
import com.hokori.web.entity.Course;
import com.hokori.web.entity.User;
import com.hokori.web.entity.WalletTransaction;
import com.hokori.web.repository.CourseRepository;
import com.hokori.web.repository.UserRepository;
import com.hokori.web.repository.WalletTransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletTransactionRepository walletTxRepo;
    private final UserRepository userRepo;
    private final CourseRepository courseRepo;

    /**
     * Giao dịch khi teacher nhận tiền từ việc bán 1 khóa học.
     * amountCents: số tiền teacher được nhận (đã trừ commission).
     */
    @Transactional
    public WalletTransaction createCourseSaleTransaction(
            Long teacherId,
            Long courseId,
            long amountCents,
            Long createdBy // có thể null hoặc id system
    ) {
        if (amountCents <= 0) {
            throw new IllegalArgumentException("amountCents must be > 0");
        }

        User teacher = userRepo.findById(teacherId)
                .orElseThrow(() -> new EntityNotFoundException("Teacher not found"));

        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));

        long oldBalance = teacher.getWalletBalance() == null ? 0L : teacher.getWalletBalance();
        long newBalance = oldBalance + amountCents;

        teacher.setWalletBalance(newBalance);

        WalletTransaction tx = WalletTransaction.builder()
                .user(teacher)
                .course(course)
                .status(WalletTransactionStatus.COMPLETED)
                .amountCents(amountCents)   // dương
                .balanceAfterCents(newBalance)
                .source(WalletTransactionSource.COURSE_SALE)
                .description("Revenue from course sale: " + course.getTitle())
                .createdBy(createdBy)
                .build();

        walletTxRepo.save(tx);
        userRepo.save(teacher);

        return tx;
    }

    /**
     * Giao dịch payout: trừ tiền trong ví teacher khi chuyển về ngân hàng.
     * payoutAmountCents: số tiền muốn trả cho teacher.
     */
    @Transactional
    public WalletTransaction createTeacherPayout(
            Long teacherId,
            long payoutAmountCents,
            Long adminId
    ) {
        if (payoutAmountCents <= 0) {
            throw new IllegalArgumentException("payoutAmountCents must be > 0");
        }

        User teacher = userRepo.findById(teacherId)
                .orElseThrow(() -> new EntityNotFoundException("Teacher not found"));

        long oldBalance = teacher.getWalletBalance() == null ? 0L : teacher.getWalletBalance();
        if (payoutAmountCents > oldBalance) {
            throw new IllegalArgumentException("Payout amount is greater than wallet balance");
        }

        long newBalance = oldBalance - payoutAmountCents;

        teacher.setWalletBalance(newBalance);
        teacher.setLastPayoutDate(LocalDate.now());

        WalletTransaction tx = WalletTransaction.builder()
                .user(teacher)
                .status(WalletTransactionStatus.COMPLETED)
                .amountCents(-payoutAmountCents) // âm
                .balanceAfterCents(newBalance)
                .source(WalletTransactionSource.TEACHER_PAYOUT)
                .description("Payout to teacher")
                .createdBy(adminId)
                .build();

        walletTxRepo.save(tx);
        userRepo.save(teacher);

        return tx;
    }

    /**
     * Admin chỉnh tay số dư (thưởng / phạt).
     * amountCents có thể dương (cộng) hoặc âm (trừ).
     */
    @Transactional
    public WalletTransaction adminAdjustBalance(
            Long userId,
            long amountCents,
            String description,
            Long adminId
    ) {
        if (amountCents == 0) {
            throw new IllegalArgumentException("amountCents must not be 0");
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        long oldBalance = user.getWalletBalance() == null ? 0L : user.getWalletBalance();
        long newBalance = oldBalance + amountCents;

        if (newBalance < 0) {
            throw new IllegalArgumentException("Wallet balance cannot be negative");
        }

        user.setWalletBalance(newBalance);

        WalletTransaction tx = WalletTransaction.builder()
                .user(user)
                .status(WalletTransactionStatus.COMPLETED)
                .amountCents(amountCents)
                .balanceAfterCents(newBalance)
                .source(WalletTransactionSource.ADMIN_ADJUST)
                .description(description)
                .createdBy(adminId)
                .build();

        walletTxRepo.save(tx);
        userRepo.save(user);

        return tx;
    }
}
