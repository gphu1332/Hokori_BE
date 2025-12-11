package com.hokori.web.repository;

import com.hokori.web.dto.wallet.WalletSummaryResponse;
import com.hokori.web.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByFirebaseUid(String firebaseUid);
    Optional<User> findByEmail(String email);
    
    Optional<User> findByPhoneNumber(String phoneNumber);
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.email = :email")
    Optional<User> findByEmailWithRole(@Param("email") String email);
    
    /**
     * Check if user exists and is active by email (for authentication).
     * Uses JPQL to select only non-LOB fields to avoid LOB stream issues.
     * This is simpler and more reliable than native queries.
     */
    @Query("SELECT u.id, u.isActive FROM User u WHERE u.email = :email")
    Optional<Object[]> findUserActiveStatusByEmail(@Param("email") String email);
    
    /**
     * Get role info by email (avoids LOB fields).
     * Returns: [role_id, role_name, role_description]
     */
    @Query(value = "SELECT r.id, r.role_name, r.description FROM users u JOIN roles r ON u.role_id = r.id WHERE u.email = :email", nativeQuery = true)
    Optional<Object[]> findRoleInfoByEmail(@Param("email") String email);
    
    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByFirebaseUid(String firebaseUid);

    // ------------ Common filters ------------
    List<User> findByIsActiveTrue();
    List<User> findByIsVerifiedTrue();
    List<User> findByIsActive(Boolean isActive);
    List<User> findByIsVerified(Boolean isVerified);

    @Query("SELECT u FROM User u WHERE u.createdAt >= :date")
    List<User> findUsersCreatedAfter(@Param("date") LocalDateTime date);

    @Query("SELECT u FROM User u WHERE u.lastLoginAt >= :date")
    List<User> findUsersWithRecentLogin(@Param("date") LocalDateTime date);

    // ------------ Fetch-join to avoid LazyInitialization on serialization ------------
    @Query("select distinct u from User u left join fetch u.role")
    List<User> findAllWithRole();

    @Query("SELECT u FROM User u JOIN FETCH u.role r WHERE r.id = :roleId")
    List<User> findByRoleId(@Param("roleId") Long roleId);

    @Query("SELECT u FROM User u JOIN FETCH u.role r WHERE r.roleName = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);

    @Query("SELECT COUNT(u) FROM User u JOIN u.role r WHERE r.id = :roleId")
    long countByRoleId(@Param("roleId") Long roleId);

    @Query("""
           SELECT u FROM User u
           LEFT JOIN FETCH u.role
           WHERE LOWER(u.displayName) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(u.username)    LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(u.email)       LIKE LOWER(CONCAT('%', :q, '%'))
           """)
    List<User> searchUsers(@Param("q") String searchTerm);

    @Query("select u from User u left join fetch u.role where u.id = :id")
    Optional<User> findByIdWithRole(@Param("id") Long id);

    // ===== Thêm mới cho wallet (JPQL, không đụng LOB) =====

    /**
     * Lấy thông tin ví đơn giản theo email.
     * Trả về mảng 3 phần tử: [id, walletBalance, lastPayoutDate]
     * => Không select các cột LOB nên tránh lỗi "Unable to access lob stream".
     */
    @Query("""
           SELECT u.id, u.walletBalance, u.lastPayoutDate
           FROM User u
           WHERE u.email = :email
           """)
    Optional<Object[]> findWalletInfoByEmail(@Param("email") String email);

    /**
     * Lấy id user theo email (dùng cho adminId, teacherId từ JWT).
     */
    @Query("SELECT u.id FROM User u WHERE u.email = :email")
    Optional<Long> findIdByEmail(@Param("email") String email);

    // ===== Cho wallet (JPQL, không đụng LOB) =====

    /**
     * Lấy thông tin ví đơn giản theo email.
     * Trả về luôn DTO WalletSummaryResponse (constructor expression),
     * tránh phải làm việc với Object[].
     */
    @Query("""
       SELECT new com.hokori.web.dto.wallet.WalletSummaryResponse(
            u.id,
            u.walletBalance,
            u.lastPayoutDate
       )
       FROM User u
       WHERE u.email = :email
       """)
    Optional<WalletSummaryResponse> findWalletSummaryByEmail(@Param("email") String email);
    
    /**
     * Get user profile metadata without loading LOB fields (avoids LOB stream error on PostgreSQL).
     * Compatible with both PostgreSQL (Railway) and SQL Server (SSMS).
     * Returns: [id, email, username, displayName, avatarUrl, phoneNumber, isActive, isVerified, 
     *          lastLoginAt, createdAt, approvalStatus, approvedAt, profileApprovalRequestId,
     *          yearsOfExperience, bio, websiteUrl, linkedin, bankAccountNumber, bankAccountName,
     *          bankName, bankBranchName, lastPayoutDate]
     */
    @Query(value = """
        SELECT u.id, u.email, u.username, u.display_name, u.avatar_url, u.phone_number, 
               u.is_active, u.is_verified, u.last_login_at, u.created_at,
               u.approval_status, u.approved_at, u.profile_approval_request_id,
               u.years_of_experience, u.bio, u.website_url, u.linkedin,
               u.bank_account_number, u.bank_account_name, u.bank_name, u.bank_branch_name,
               u.last_payout_date
        FROM users u
        WHERE u.email = :email
        """, nativeQuery = true)
    Optional<Object[]> findUserProfileMetadataByEmail(@Param("email") String email);
    
    /**
     * Get user profile metadata by ID (avoids LOB stream error).
     * Compatible with both PostgreSQL (Railway) and SQL Server (SSMS).
     * Returns: [id, email, username, displayName, avatarUrl, phoneNumber, isActive, isVerified, 
     *          lastLoginAt, createdAt, approvalStatus, approvedAt, profileApprovalRequestId,
     *          yearsOfExperience, bio, websiteUrl, linkedin, bankAccountNumber, bankAccountName,
     *          bankName, bankBranchName, lastPayoutDate]
     */
    @Query(value = """
        SELECT u.id, u.email, u.username, u.display_name, u.avatar_url, u.phone_number, 
               u.is_active, u.is_verified, u.last_login_at, u.created_at,
               u.approval_status, u.approved_at, u.profile_approval_request_id,
               u.years_of_experience, u.bio, u.website_url, u.linkedin,
               u.bank_account_number, u.bank_account_name, u.bank_name, u.bank_branch_name,
               u.last_payout_date
        FROM users u
        WHERE u.id = :id
        """, nativeQuery = true)
    Optional<Object[]> findUserProfileMetadataById(@Param("id") Long id);
}
