package com.hokori.web.repository;

import com.hokori.web.Enum.JLPTLevel;
import com.hokori.web.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    Optional<Course> findBySlugAndDeletedFlagFalse(String slug);

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
    
    /**
     * Get course metadata without LOB fields (avoids LOB stream error).
     * Returns: [id, title, slug, subtitle, level, priceCents, discountedPriceCents, currency, coverImagePath, status, publishedAt, userId, deletedFlag]
     */
    @Query(value = """
        SELECT c.id, c.title, c.slug, c.subtitle, c.level, c.price_cents, c.discounted_price_cents, 
               c.currency, c.cover_image_path, c.status, c.published_at, c.user_id, c.deleted_flag
        FROM course c
        WHERE c.id = :id AND c.deleted_flag = false
        """, nativeQuery = true)
    Optional<Object[]> findCourseMetadataById(@Param("id") Long id);
    
    /**
     * List courses metadata without LOB fields for pagination.
     * Returns: [id, title, slug, subtitle, level, priceCents, discountedPriceCents, currency, coverImagePath, status, publishedAt, userId, deletedFlag]
     */
    @Query(value = """
        SELECT c.id, c.title, c.slug, c.subtitle, c.level, c.price_cents, c.discounted_price_cents, 
               c.currency, c.cover_image_path, c.status, c.published_at, c.user_id, c.deleted_flag
        FROM course c
        WHERE c.deleted_flag = false 
          AND c.user_id = :userId
          AND (:status IS NULL OR c.status = :status)
          AND (:q IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(c.slug) LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY c.updated_at DESC
        """, nativeQuery = true)
    List<Object[]> findCourseMetadataByUserId(@Param("userId") Long userId, 
                                               @Param("status") String status, 
                                               @Param("q") String q);
    
    /**
     * List published courses metadata without LOB fields.
     * Returns: [id, title, slug, subtitle, level, priceCents, discountedPriceCents, currency, coverImagePath, status, publishedAt, userId, deletedFlag]
     */
    @Query(value = """
        SELECT c.id, c.title, c.slug, c.subtitle, c.level, c.price_cents, c.discounted_price_cents, 
               c.currency, c.cover_image_path, c.status, c.published_at, c.user_id, c.deleted_flag
        FROM course c
        WHERE c.deleted_flag = false 
          AND c.status = 'PUBLISHED'
          AND (:level IS NULL OR c.level = :level)
        ORDER BY c.published_at DESC
        """, nativeQuery = true)
    List<Object[]> findPublishedCourseMetadata(@Param("level") String level);
    
    /**
     * Get course price without loading LOB fields (for cart operations).
     * Returns: [id, priceCents, deletedFlag]
     */
    @Query(value = """
        SELECT c.id, c.price_cents, c.deleted_flag
        FROM course c
        WHERE c.id = :id AND c.deleted_flag = false
        """, nativeQuery = true)
    Optional<Object[]> findCoursePriceById(@Param("id") Long id);
}
