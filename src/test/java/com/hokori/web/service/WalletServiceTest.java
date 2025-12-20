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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletTransactionRepository walletTxRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private CourseRepository courseRepo;

    @InjectMocks
    private WalletService walletService;

    /**
     * TC-WALLET-01
     * Teacher nhận tiền từ bán khóa học
     */
    @Test
    void createCourseSaleTransaction_success() {
        Long teacherId = 1L;
        Long courseId = 10L;
        long amount = 100_000L;

        User teacher = new User();
        teacher.setId(teacherId);
        teacher.setWalletBalance(200_000L);

        Course course = new Course();
        course.setId(courseId);
        course.setTitle("JLPT N4");

        when(userRepo.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(courseRepo.findById(courseId)).thenReturn(Optional.of(course));

        WalletTransaction tx = walletService.createCourseSaleTransaction(
                teacherId, courseId, amount, null
        );

        assertNotNull(tx);
        assertEquals(WalletTransactionSource.COURSE_SALE, tx.getSource());
        assertEquals(WalletTransactionStatus.COMPLETED, tx.getStatus());
        assertEquals(amount, tx.getAmountCents());
        assertEquals(300_000L, tx.getBalanceAfterCents());

        verify(walletTxRepo).save(any(WalletTransaction.class));
        verify(userRepo).save(teacher);
    }

    /**
     * TC-WALLET-02
     * Teacher payout hợp lệ
     */
    @Test
    void createTeacherPayout_success() {
        Long teacherId = 2L;
        long payout = 150_000L;

        User teacher = new User();
        teacher.setId(teacherId);
        teacher.setWalletBalance(300_000L);

        when(userRepo.findById(teacherId)).thenReturn(Optional.of(teacher));

        WalletTransaction tx = walletService.createTeacherPayout(
                teacherId, payout, 99L
        );

        assertNotNull(tx);
        assertEquals(WalletTransactionSource.TEACHER_PAYOUT, tx.getSource());
        assertEquals(-payout, tx.getAmountCents());
        assertEquals(150_000L, tx.getBalanceAfterCents());
        assertEquals(LocalDate.now(), teacher.getLastPayoutDate());

        verify(walletTxRepo).save(any(WalletTransaction.class));
        verify(userRepo).save(teacher);
    }

    /**
     * TC-WALLET-03
     * Admin điều chỉnh số dư
     */
    @Test
    void adminAdjustBalance_success() {
        Long userId = 3L;
        long adjustAmount = -50_000L;

        User user = new User();
        user.setId(userId);
        user.setWalletBalance(200_000L);

        when(userRepo.findById(userId)).thenReturn(Optional.of(user));

        WalletTransaction tx = walletService.adminAdjustBalance(
                userId, adjustAmount, "Penalty", 1L
        );

        assertNotNull(tx);
        assertEquals(WalletTransactionSource.ADMIN_ADJUST, tx.getSource());
        assertEquals(adjustAmount, tx.getAmountCents());
        assertEquals(150_000L, tx.getBalanceAfterCents());

        verify(walletTxRepo).save(any(WalletTransaction.class));
        verify(userRepo).save(user);
    }

    /**
     * TC-WALLET-04
     * Payout vượt số dư → lỗi
     */
    @Test
    void createTeacherPayout_exceedBalance_throwException() {
        Long teacherId = 4L;

        User teacher = new User();
        teacher.setId(teacherId);
        teacher.setWalletBalance(50_000L);

        when(userRepo.findById(teacherId)).thenReturn(Optional.of(teacher));

        assertThrows(
                IllegalArgumentException.class,
                () -> walletService.createTeacherPayout(teacherId, 100_000L, 1L)
        );
    }
}
