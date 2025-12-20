package com.hokori.web.repository;

import com.hokori.web.entity.PasswordResetLockout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetLockoutRepository extends JpaRepository<PasswordResetLockout, Long> {

    /**
     * Tìm lockout đang active cho email
     * Lockout được coi là active nếu:
     * - isUnlocked = false
     * - unlockAt > now (chưa hết hạn)
     */
    @Query("""
            SELECT l FROM PasswordResetLockout l
            WHERE l.email = :email
            AND l.isUnlocked = false
            AND l.unlockAt > :now
            ORDER BY l.lockedAt DESC
            """)
    Optional<PasswordResetLockout> findActiveLockoutByEmail(
            @Param("email") String email, 
            @Param("now") LocalDateTime now);

    /**
     * Tìm lockout đang active cho IP address
     */
    @Query("""
            SELECT l FROM PasswordResetLockout l
            WHERE l.ipAddress = :ipAddress
            AND l.isUnlocked = false
            AND l.unlockAt > :now
            ORDER BY l.lockedAt DESC
            """)
    Optional<PasswordResetLockout> findActiveLockoutByIpAddress(
            @Param("ipAddress") String ipAddress, 
            @Param("now") LocalDateTime now);

    /**
     * Tìm lockout đang active cho email HOẶC IP address
     * Dùng để check lockout khi request OTP hoặc verify OTP
     */
    @Query("""
            SELECT l FROM PasswordResetLockout l
            WHERE (l.email = :email OR l.ipAddress = :ipAddress)
            AND l.isUnlocked = false
            AND l.unlockAt > :now
            ORDER BY l.lockedAt DESC
            """)
    Optional<PasswordResetLockout> findActiveLockoutByEmailOrIp(
            @Param("email") String email,
            @Param("ipAddress") String ipAddress,
            @Param("now") LocalDateTime now);

    /**
     * Mở khóa thủ công (admin có thể dùng)
     */
    @Modifying
    @Query("UPDATE PasswordResetLockout l SET l.isUnlocked = true WHERE l.id = :id")
    void unlock(@Param("id") Long id);

    /**
     * Xóa các lockout đã hết hạn và đã unlock (cleanup job)
     */
    @Modifying
    @Query("DELETE FROM PasswordResetLockout l WHERE l.unlockAt < :now AND l.isUnlocked = true")
    void deleteExpiredAndUnlockedLockouts(@Param("now") LocalDateTime now);
}

