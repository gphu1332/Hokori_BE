package com.hokori.web.repository;

import com.hokori.web.entity.TeacherRevenue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherRevenueRepository extends JpaRepository<TeacherRevenue, Long> {
    
    /**
     * Tìm revenue theo teacher và tháng
     */
    List<TeacherRevenue> findByTeacher_IdAndYearMonthOrderByPaidAtDesc(Long teacherId, String yearMonth);
    
    /**
     * Tìm revenue theo course và tháng
     */
    List<TeacherRevenue> findByCourse_IdAndYearMonthOrderByPaidAtDesc(Long courseId, String yearMonth);
    
    /**
     * Tìm tất cả revenue của teacher (chưa phân trang)
     */
    List<TeacherRevenue> findByTeacher_IdOrderByYearMonthDescPaidAtDesc(Long teacherId);
    
    /**
     * Tìm revenue chưa được chuyển tiền (isPaid = false)
     */
    List<TeacherRevenue> findByTeacher_IdAndIsPaidFalseOrderByYearMonthDescPaidAtDesc(Long teacherId);
    
    /**
     * Tìm revenue theo payment và course (để check duplicate)
     */
    Optional<TeacherRevenue> findByPayment_IdAndCourse_Id(Long paymentId, Long courseId);
    
    /**
     * Tính tổng revenue của teacher trong tháng (chưa trừ commission)
     */
    @Query("SELECT COALESCE(SUM(tr.teacherRevenueCents), 0) FROM TeacherRevenue tr " +
           "WHERE tr.teacher.id = :teacherId AND tr.yearMonth = :yearMonth")
    Long sumTeacherRevenueByTeacherIdAndYearMonth(@Param("teacherId") Long teacherId, 
                                                  @Param("yearMonth") String yearMonth);
    
    /**
     * Tính tổng revenue của course trong tháng
     */
    @Query("SELECT COALESCE(SUM(tr.teacherRevenueCents), 0) FROM TeacherRevenue tr " +
           "WHERE tr.course.id = :courseId AND tr.yearMonth = :yearMonth")
    Long sumTeacherRevenueByCourseIdAndYearMonth(@Param("courseId") Long courseId, 
                                                @Param("yearMonth") String yearMonth);
    
    /**
     * Tìm tất cả revenue chưa được chuyển tiền, group by teacher và yearMonth
     */
    @Query("SELECT tr.teacher.id, tr.yearMonth, SUM(tr.teacherRevenueCents) as totalRevenue " +
           "FROM TeacherRevenue tr " +
           "WHERE tr.isPaid = false " +
           "GROUP BY tr.teacher.id, tr.yearMonth " +
           "ORDER BY tr.yearMonth DESC, tr.teacher.id")
    List<Object[]> findUnpaidRevenueGroupedByTeacherAndMonth();
    
    /**
     * Tìm revenue chưa được chuyển tiền theo yearMonth cụ thể, group by teacher
     * Optimized: filter ngay trong SQL thay vì filter trong code
     */
    @Query("SELECT tr.teacher.id, tr.yearMonth, SUM(tr.teacherRevenueCents) as totalRevenue " +
           "FROM TeacherRevenue tr " +
           "WHERE tr.isPaid = false AND tr.yearMonth = :yearMonth " +
           "GROUP BY tr.teacher.id, tr.yearMonth " +
           "ORDER BY tr.teacher.id")
    List<Object[]> findUnpaidRevenueGroupedByTeacherAndMonthForYearMonth(@Param("yearMonth") String yearMonth);
    
    /**
     * Tìm revenue chưa được chuyển tiền của teacher trong tháng cụ thể
     */
    List<TeacherRevenue> findByTeacher_IdAndYearMonthAndIsPaidFalseOrderByPaidAtDesc(
            Long teacherId, String yearMonth);
    
    /**
     * Đếm số revenue chưa được chuyển tiền của teacher
     */
    long countByTeacher_IdAndIsPaidFalse(Long teacherId);
    
    /**
     * Tính tổng admin commission trong tháng (optimized query)
     */
    @Query("SELECT COALESCE(SUM(tr.adminCommissionCents), 0) FROM TeacherRevenue tr " +
           "WHERE tr.yearMonth = :yearMonth")
    Long sumAdminCommissionByYearMonth(@Param("yearMonth") String yearMonth);
    
    /**
     * Tính tổng admin commission từ revenue chưa được trả tiền trong tháng (doanh thu dự kiến)
     */
    @Query("SELECT COALESCE(SUM(tr.adminCommissionCents), 0) FROM TeacherRevenue tr " +
           "WHERE tr.yearMonth = :yearMonth AND tr.isPaid = false")
    Long sumUnpaidAdminCommissionByYearMonth(@Param("yearMonth") String yearMonth);
    
    /**
     * Tính tổng admin commission từ revenue đã được trả tiền trong tháng (doanh thu đã chuyển tiền)
     */
    @Query("SELECT COALESCE(SUM(tr.adminCommissionCents), 0) FROM TeacherRevenue tr " +
           "WHERE tr.yearMonth = :yearMonth AND tr.isPaid = true")
    Long sumPaidAdminCommissionByYearMonth(@Param("yearMonth") String yearMonth);
}

