package com.hokori.web.repository;

import com.hokori.web.Enum.CourseStatus;
import com.hokori.web.Enum.JLPTLevel;
import com.hokori.web.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    Optional<Course> findBySlugAndDeletedFlagFalse(String slug);

    // Check if slug exists (including deleted) - for unique constraint validation
    boolean existsBySlug(String slug);

    // ⚠️ KHÔNG dùng @EntityGraph ở đây để tránh multiple bag fetch
    Optional<Course> findByIdAndDeletedFlagFalse(Long id);

    // (không bắt buộc cho service hiện tại, nhưng giữ cũng không sao)
    Page<Course> findAllByDeletedFlagFalse(Pageable pageable);

    Page<Course> findAllByUserIdAndDeletedFlagFalse(Long userId, Pageable pageable);

    @Query("""
       select c
       from Course c
       where c.deletedFlag = false
         and c.status = 'PUBLISHED'
         and (:lvl is null or c.level = :lvl)
    """)
    Page<Course> findPublishedByLevel(JLPTLevel lvl, Pageable pageable);

    boolean existsByIdAndUserIdAndDeletedFlagFalse(Long id, Long userId);

    // =======================
    // 1) Course metadata by id
    // =======================

    /**
     * INTERNAL: thực thi native query với parameter boolean deleted.
     */
    @Query(value = """
        SELECT c.id, c.title, c.slug, c.subtitle, c.level, c.price_cents, c.discounted_price_cents, 
               c.currency, c.cover_image_path, c.status, c.published_at, c.user_id, c.deleted_flag,
               COALESCE(u.display_name, u.username) as teacher_name,
               c.rejection_reason, c.rejected_at, c.rejected_by_user_id,
               c.flagged_reason, c.flagged_at, c.flagged_by_user_id
        FROM course c
        LEFT JOIN users u ON c.user_id = u.id
        WHERE c.id = :id AND c.deleted_flag = :deleted
        """, nativeQuery = true)
    Optional<Object[]> findCourseMetadataByIdInternal(@Param("id") Long id,
                                                      @Param("deleted") boolean deleted);

    /**
     * API chính dùng trong service – luôn lấy deleted_flag = false.
     */
    default Optional<Object[]> findCourseMetadataById(Long id) {
        return findCourseMetadataByIdInternal(id, false);
    }

    // =====================================
    // 2) Course metadata list theo userId
    // =====================================

    /**
     * INTERNAL: thực thi native query với parameter boolean deleted.
     *
     * Returns: [id, title, slug, subtitle, level, priceCents, discountedPriceCents,
     *           currency, coverImagePath, status, publishedAt, userId, deletedFlag, teacherName]
     */
    @Query(value = """
        SELECT c.id, c.title, c.slug, c.subtitle, c.level, c.price_cents, c.discounted_price_cents, 
               c.currency, c.cover_image_path, c.status, c.published_at, c.user_id, c.deleted_flag,
               COALESCE(u.display_name, u.username) as teacher_name,
               c.rejection_reason, c.rejected_at, c.rejected_by_user_id,
               c.flagged_reason, c.flagged_at, c.flagged_by_user_id
        FROM course c
        LEFT JOIN users u ON c.user_id = u.id
        WHERE c.deleted_flag = :deleted
          AND c.user_id = :userId
          AND (:status IS NULL OR c.status = :status)
          AND (:q IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(c.slug)  LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY c.updated_at DESC
        """, nativeQuery = true)
    List<Object[]> findCourseMetadataByUserIdInternal(@Param("userId") Long userId,
                                                      @Param("status") String status,
                                                      @Param("q") String q,
                                                      @Param("deleted") boolean deleted);

    /**
     * API chính – luôn lấy deleted_flag = false.
     */
    default List<Object[]> findCourseMetadataByUserId(Long userId, String status, String q) {
        return findCourseMetadataByUserIdInternal(userId, status, q, false);
    }

    // =========================================
    // 3) Published courses metadata (Marketplace)
    // =========================================

    /**
     * INTERNAL: list published courses metadata without LOB fields.
     * Note: PUBLISHED courses never have rejection fields, so we don't include them here.
     *
     * Returns: [id, title, slug, subtitle, level, priceCents, discountedPriceCents,
     *           currency, coverImagePath, status, publishedAt, userId, deletedFlag, teacherName]
     */
    @Query(value = """
        SELECT c.id, c.title, c.slug, c.subtitle, c.level, c.price_cents, c.discounted_price_cents, 
               c.currency, c.cover_image_path, c.status, c.published_at, c.user_id, c.deleted_flag,
               COALESCE(u.display_name, u.username) as teacher_name
        FROM course c
        LEFT JOIN users u ON c.user_id = u.id
        WHERE c.deleted_flag = :deleted
          AND c.status = 'PUBLISHED'
          AND (:level IS NULL OR c.level = :level)
        ORDER BY c.published_at DESC
        """, nativeQuery = true)
    List<Object[]> findPublishedCourseMetadataInternal(@Param("level") String level,
                                                       @Param("deleted") boolean deleted);

    /**
     * API chính – luôn lấy deleted_flag = false.
     */
    default List<Object[]> findPublishedCourseMetadata(String level) {
        return findPublishedCourseMetadataInternal(level, false);
    }

    // =============================
    // 4) Course price (Cart usage)
    // =============================

    /**
     * INTERNAL: get course price without loading LOB fields (for cart operations).
     * Returns: [id, priceCents, deletedFlag, status]
     */
    @Query(value = """
        SELECT c.id, c.price_cents, c.deleted_flag, c.status
        FROM course c
        WHERE c.id = :id AND c.deleted_flag = :deleted
        """, nativeQuery = true)
    Optional<Object[]> findCoursePriceByIdInternal(@Param("id") Long id,
                                                   @Param("deleted") boolean deleted);

    /**
     * API chính – luôn lấy deleted_flag = false.
     */
    default Optional<Object[]> findCoursePriceById(Long id) {
        return findCoursePriceByIdInternal(id, false);
    }

    // ========================================
    // 5) Courses pending approval (Moderator)
    // ========================================

    /**
     * INTERNAL: list courses pending approval (for moderator).
     *
     * Returns: [id, title, slug, subtitle, level, priceCents, discountedPriceCents,
     *           currency, coverImagePath, status, publishedAt, userId, deletedFlag, teacherName,
     *           rejectionReason, rejectedAt, rejectedByUserId]
     */
    @Query(value = """
        SELECT c.id, c.title, c.slug, c.subtitle, c.level, c.price_cents, c.discounted_price_cents, 
               c.currency, c.cover_image_path, c.status, c.published_at, c.user_id, c.deleted_flag,
               COALESCE(u.display_name, u.username) as teacher_name,
               c.rejection_reason, c.rejected_at, c.rejected_by_user_id
        FROM course c
        LEFT JOIN users u ON c.user_id = u.id
        WHERE c.deleted_flag = :deleted
          AND c.status = 'PENDING_APPROVAL'
        ORDER BY c.updated_at DESC
        """, nativeQuery = true)
    List<Object[]> findPendingApprovalCoursesInternal(@Param("deleted") boolean deleted);

    /**
     * API chính – luôn lấy deleted_flag = false.
     */
    default List<Object[]> findPendingApprovalCourses() {
        return findPendingApprovalCoursesInternal(false);
    }

    // ===================================
    // Các method derived còn lại
    // ===================================

    long countByUserIdAndStatusAndDeletedFlagFalse(Long userId, CourseStatus status);

    Page<Course> findByUserIdAndDeletedFlagFalse(Long userId, Pageable pageable);

    List<Course> findAllByDeletedFlagFalse();
}
