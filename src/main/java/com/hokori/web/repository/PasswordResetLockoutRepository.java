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
    @Query(value = """
            SELECT l FROM PasswordResetLockout l
            WHERE l.email = :email
            AND l.isUnlocked = false
            AND l.unlockAt > :now
            ORDER BY l.lockedAt DESC, l.id DESC
            """)
    java.util.List<PasswordResetLockout> findActiveLockoutByEmailList(
            @Param("email") String email, 
            @Param("now") LocalDateTime now);
    
    /**
     * Tìm lockout đang active cho email (wrapper method)
     */
    default Optional<PasswordResetLockout> findActiveLockoutByEmail(String email, LocalDateTime now) {
        java.util.List<PasswordResetLockout> list = findActiveLockoutByEmailList(email, now);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * Tìm lockout đang active cho IP address
     */
    @Query(value = """
            SELECT l FROM PasswordResetLockout l
            WHERE l.ipAddress = :ipAddress
            AND l.isUnlocked = false
            AND l.unlockAt > :now
            ORDER BY l.lockedAt DESC, l.id DESC
            """)
    java.util.List<PasswordResetLockout> findActiveLockoutByIpAddressList(
            @Param("ipAddress") String ipAddress, 
            @Param("now") LocalDateTime now);
    
    /**
     * Tìm lockout đang active cho IP address (wrapper method)
     */
    default Optional<PasswordResetLockout> findActiveLockoutByIpAddress(String ipAddress, LocalDateTime now) {
        java.util.List<PasswordResetLockout> list = findActiveLockoutByIpAddressList(ipAddress, now);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * Tìm lockout đang active cho email HOẶC IP address
     * Dùng để check lockout khi request OTP hoặc verify OTP
     * Trả về lockout mới nhất (theo lockedAt DESC, sau đó theo id DESC để đảm bảo unique)
     */
    @Query(value = """
            SELECT l FROM PasswordResetLockout l
            WHERE (l.email = :email OR l.ipAddress = :ipAddress)
            AND l.isUnlocked = false
            AND l.unlockAt > :now
            ORDER BY l.lockedAt DESC, l.id DESC
            """)
    java.util.List<PasswordResetLockout> findActiveLockoutByEmailOrIpList(
            @Param("email") String email,
            @Param("ipAddress") String ipAddress,
            @Param("now") LocalDateTime now);
    
    /**
     * Tìm lockout đang active cho email HOẶC IP address (wrapper method)
     */
    default Optional<PasswordResetLockout> findActiveLockoutByEmailOrIp(String email, String ipAddress, LocalDateTime now) {
        java.util.List<PasswordResetLockout> list = findActiveLockoutByEmailOrIpList(email, ipAddress, now);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

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

