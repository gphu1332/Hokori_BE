package com.hokori.web.repository;

import com.hokori.web.Enum.WalletTransactionSource;
import com.hokori.web.Enum.WalletTransactionStatus;
import com.hokori.web.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    Page<WalletTransaction> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // ====== Load WalletTransaction with Course (JOIN FETCH) ======
    @Query("""
        select t from WalletTransaction t
        left join fetch t.course
        where t.user.id = :userId
        order by t.createdAt desc
        """)
    List<WalletTransaction> findByUser_IdWithCourseOrderByCreatedAtDesc(@Param("userId") Long userId);

    // ====== NEW: tổng amount_cents trong 1 khoảng thời gian ======
    @Query("""
        select coalesce(sum(t.amountCents), 0)
        from WalletTransaction t
        where t.user.id = :userId
          and t.status = :status
          and t.source = :source
          and t.createdAt between :from and :to
        """)
    Long sumIncomeForPeriod(
            @Param("userId") Long userId,
            @Param("status") WalletTransactionStatus status,
            @Param("source") WalletTransactionSource source,
            @Param("from") Instant from,
            @Param("to") Instant to
    );
}
