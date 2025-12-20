package com.hokori.web.repository;

import com.hokori.web.entity.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {

    /**
     * Tìm OTP chưa hết hạn và chưa sử dụng theo email
     * Trả về OTP mới nhất (theo createdAt DESC, sau đó theo id DESC để đảm bảo unique)
     */
    @Query(value = """
            SELECT o FROM PasswordResetOtp o
            WHERE o.email = :email
            AND o.isUsed = false
            AND o.expiresAt > :now
            ORDER BY o.createdAt DESC, o.id DESC
            """)
    java.util.List<PasswordResetOtp> findLatestValidByEmailList(@Param("email") String email, @Param("now") LocalDateTime now);
    
    /**
     * Tìm OTP chưa hết hạn và chưa sử dụng theo email (wrapper method)
     */
    default Optional<PasswordResetOtp> findLatestValidByEmail(String email, LocalDateTime now) {
        java.util.List<PasswordResetOtp> list = findLatestValidByEmailList(email, now);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
    
    /**
     * Tìm OTP theo email và OTP code (dùng để verify đúng OTP mà user nhập vào)
     * Tìm trong các OTP chưa hết hạn và chưa sử dụng
     */
    @Query(value = """
            SELECT o FROM PasswordResetOtp o
            WHERE o.email = :email
            AND o.otpCode = :otpCode
            AND o.isUsed = false
            AND o.expiresAt > :now
            ORDER BY o.createdAt DESC, o.id DESC
            """)
    java.util.List<PasswordResetOtp> findValidOtpByEmailAndCodeList(
            @Param("email") String email,
            @Param("otpCode") String otpCode,
            @Param("now") LocalDateTime now);
    
    /**
     * Tìm OTP theo email và OTP code (wrapper method)
     */
    default Optional<PasswordResetOtp> findValidOtpByEmailAndCode(String email, String otpCode, LocalDateTime now) {
        java.util.List<PasswordResetOtp> list = findValidOtpByEmailAndCodeList(email, otpCode, now);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * Tìm OTP đã verify (isUsed = true) theo email và OTP code
     * Dùng để reset password sau khi đã verify OTP
     * 
     * Lưu ý: Không check expiresAt vì OTP đã được verify, cho phép reset password
     * trong một khoảng thời gian hợp lý sau khi verify (ví dụ: 30 phút sau khi verify)
     * Trả về OTP mới nhất (theo createdAt DESC, sau đó theo id DESC để đảm bảo unique)
     */
    @Query(value = """
            SELECT o FROM PasswordResetOtp o
            WHERE o.email = :email
            AND o.otpCode = :otpCode
            AND o.isUsed = true
            ORDER BY o.createdAt DESC, o.id DESC
            """)
    java.util.List<PasswordResetOtp> findVerifiedOtpByEmailAndCodeList(
            @Param("email") String email, 
            @Param("otpCode") String otpCode);
    
    /**
     * Tìm OTP đã verify theo email và OTP code (wrapper method)
     */
    default Optional<PasswordResetOtp> findVerifiedOtpByEmailAndCode(String email, String otpCode) {
        java.util.List<PasswordResetOtp> list = findVerifiedOtpByEmailAndCodeList(email, otpCode);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * Đánh dấu OTP đã sử dụng
     */
    @Modifying
    @Query("UPDATE PasswordResetOtp o SET o.isUsed = true WHERE o.id = :id")
    void markAsUsed(@Param("id") Long id);

    /**
     * Tăng số lần verify sai
     * flushAutomatically = true để đảm bảo changes được flush ngay lập tức
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE PasswordResetOtp o SET o.failedAttempts = o.failedAttempts + 1 WHERE o.id = :id")
    void incrementFailedAttempts(@Param("id") Long id);

    /**
     * Xóa các OTP đã hết hạn (cleanup job)
     */
    @Modifying
    @Query("DELETE FROM PasswordResetOtp o WHERE o.expiresAt < :now")
    void deleteExpiredOtp(@Param("now") LocalDateTime now);
    
    /**
     * Đếm tổng số lần nhập sai OTP của một email trong khoảng thời gian gần đây (15 phút)
     * Dùng để track failed attempts theo email, không phải theo từng OTP record
     */
    @Query("""
            SELECT COALESCE(SUM(o.failedAttempts), 0) 
            FROM PasswordResetOtp o
            WHERE o.email = :email
            AND o.createdAt > :since
            """)
    Long countTotalFailedAttemptsByEmailSince(
            @Param("email") String email,
            @Param("since") LocalDateTime since);
    
    /**
     * Đếm số lần request OTP của một email trong khoảng thời gian gần đây
     * Dùng để rate limiting - tránh spam request OTP
     */
    @Query("""
            SELECT COUNT(o) 
            FROM PasswordResetOtp o
            WHERE o.email = :email
            AND o.createdAt > :since
            """)
    Long countOtpRequestsByEmailSince(
            @Param("email") String email,
            @Param("since") LocalDateTime since);
    
    /**
     * Invalidate các OTP cũ chưa sử dụng của email này khi request OTP mới
     * Đánh dấu isUsed = true để không thể verify OTP cũ nữa
     * Đảm bảo chỉ có 1 OTP active tại một thời điểm
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE PasswordResetOtp o 
            SET o.isUsed = true 
            WHERE o.email = :email
            AND o.isUsed = false
            AND o.expiresAt > :now
            """)
    void invalidateOldOtpsForEmail(
            @Param("email") String email,
            @Param("now") LocalDateTime now);
}

