package com.hokori.web.controller;

import com.hokori.web.dto.wallet.AdminAdjustRequest;
import com.hokori.web.dto.wallet.TeacherPayoutRequest;
import com.hokori.web.dto.wallet.WalletSummaryResponse;
import com.hokori.web.dto.wallet.WalletTransactionResponse;
import com.hokori.web.entity.User;
import com.hokori.web.entity.WalletTransaction;
import com.hokori.web.repository.UserRepository;
import com.hokori.web.repository.WalletTransactionRepository;
import com.hokori.web.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(
        name = "Wallet APIs",
        description = """
                Các API liên quan đến ví (wallet) của user.
                - FE truyền Bearer token trong header Authorization.
                - Đối tượng chính dùng: Teacher (nhận tiền từ bán khoá), Admin (payout / điều chỉnh ví).
                """
)
public class WalletController {

    private final WalletService walletService;
    private final WalletTransactionRepository walletTxRepo;
    private final UserRepository userRepo;

    // ===== Helper: lấy user hiện tại từ JWT =====
    /**
     * Lấy user hiện tại đang đăng nhập từ SecurityContext.
     * JwtAuthenticationFilter đang set principal = email.
     */
    private String getCurrentEmailOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Unauthenticated");
        }
        // JwtAuthenticationFilter đang set principal = email
        return auth.getName();
    }

    private Long getCurrentUserIdOrThrow() {
        String email = getCurrentEmailOrThrow();
        return userRepo.findIdByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + email));
    }


    // ===== API: Lấy thông tin ví của chính mình =====

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Lấy thông tin ví của user hiện tại",
            description = """
                    Dùng để FE hiển thị:
                    - Số dư ví hiện tại (wallet_balance)
                    - Ngày payout gần nhất (last_payout_date)
                    
                    YÊU CẦU:
                    - Gửi Bearer token trong header Authorization.
                    
                    VD gọi:
                    GET /api/wallet/me
                    Authorization: Bearer <jwt>
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lấy thông tin ví thành công",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = WalletSummaryResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập hoặc token không hợp lệ")
    })
    public WalletSummaryResponse getMyWalletSummary() {
        String email = getCurrentEmailOrThrow();

        Object[] row = userRepo.findWalletInfoByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + email));

        Long userId = (Long) row[0];
        Long walletBalance = row[1] != null ? ((Number) row[1]).longValue() : 0L;
        // đổi kiểu cho đúng với entity của bạn: LocalDate / LocalDateTime / Instant
        java.time.LocalDate lastPayoutDate = (java.time.LocalDate) row[2];

        return WalletSummaryResponse.builder()
                .userId(userId)
                .walletBalance(walletBalance)
                .lastPayoutDate(lastPayoutDate)
                .build();
    }

    // ===== API: Lịch sử giao dịch ví của chính mình (paged) =====

    @GetMapping("/me/transactions")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Lấy danh sách giao dịch ví của user hiện tại (phân trang)",
            description = """
                    Trả về lịch sử biến động ví của chính user đang đăng nhập.
                    
                    FE dùng để vẽ màn "Lịch sử ví":
                    - amountCents: số tiền thay đổi (dương = cộng, âm = trừ).
                    - balanceAfterCents: số dư sau giao dịch.
                    - source: loại giao dịch (COURSE_SALE, TEACHER_PAYOUT, ADMIN_ADJUST).
                    - createdAt: thời gian giao dịch.
                    
                    PHÂN TRANG:
                    - Dùng query param chuẩn Spring:
                        ?page=0&size=10&sort=createdAt,desc
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lấy danh sách giao dịch thành công",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = WalletTransactionResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập hoặc token không hợp lệ")
    })
    public Page<WalletTransactionResponse> getMyTransactions(
            @ParameterObject
            @Parameter(
                    description = """
                            Tham số phân trang:
                            - page: trang hiện tại (bắt đầu từ 0)
                            - size: số item mỗi trang
                            - sort: field sort, ví dụ: sort=createdAt,desc
                            """
            )
            Pageable pageable
    ) {
        Long userId = getCurrentUserIdOrThrow();
        Page<WalletTransaction> page =
                walletTxRepo.findByUser_IdOrderByCreatedAtDesc(userId, pageable);
        return page.map(WalletTransactionResponse::fromEntity);
    }

    // ===== API: Admin xem lịch sử ví của 1 user bất kỳ =====

    @GetMapping("/users/{userId}/transactions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Admin: xem lịch sử giao dịch ví của 1 user bất kỳ",
            description = """
                    Dùng cho màn Admin để tra cứu ví của user (đặc biệt là teacher).
                    
                    - Chỉ ADMIN mới gọi được.
                    - Có thể dùng chung màn với /me/transactions nhưng cho phép chọn user khác.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lấy danh sách giao dịch thành công",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = WalletTransactionResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "403", description = "Không có quyền (không phải ADMIN)")
    })
    public Page<WalletTransactionResponse> getUserTransactions(
            @Parameter(description = "ID của user cần xem ví (thường là teacherId)")
            @PathVariable Long userId,
            @ParameterObject
            @Parameter(
                    description = """
                            Tham số phân trang:
                            - page: trang hiện tại (bắt đầu từ 0)
                            - size: số item mỗi trang
                            - sort: field sort, ví dụ: sort=createdAt,desc
                            """
            )
            Pageable pageable
    ) {
        Page<WalletTransaction> page =
                walletTxRepo.findByUser_IdOrderByCreatedAtDesc(userId, pageable);
        return page.map(WalletTransactionResponse::fromEntity);
    }

    // ===== API: Admin trả tiền cho teacher (payout) =====

    @PostMapping("/admin/payout")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Admin: tạo giao dịch payout cho teacher",
            description = """
                    Khi Admin thực hiện trả tiền (chuyển khoản) cho teacher,
                    FE gọi API này để:
                    - Trừ số dư ví của teacher.
                    - Ghi lại 1 WalletTransaction với source = TEACHER_PAYOUT.
                    
                    LƯU Ý:
                    - API này KHÔNG thực hiện chuyển khoản thực tế, chỉ lưu log + cập nhật số dư.
                    - AdminId được lấy từ token hiện tại.
                    
                    Request body (JSON):
                    {
                      "teacherId": 123,
                      "amountCents": 2000000
                    }
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Tạo giao dịch payout thành công",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = WalletTransactionResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ (VD: amount > balance)"),
            @ApiResponse(responseCode = "403", description = "Không có quyền (không phải ADMIN)")
    })
    public WalletTransactionResponse payoutToTeacher(
            @Valid @RequestBody TeacherPayoutRequest req
    ) {
        Long adminId = getCurrentUserIdOrThrow();

        WalletTransaction tx = walletService.createTeacherPayout(
                req.getTeacherId(),
                req.getAmountCents(),
                adminId
        );

        return WalletTransactionResponse.fromEntity(tx);
    }

    // ===== API: Admin điều chỉnh số dư ví (thưởng/phạt) =====

    @PostMapping("/admin/adjust")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Admin: điều chỉnh số dư ví (thưởng / phạt)",
            description = """
                    Dùng khi Admin muốn:
                    - Thưởng thêm tiền vào ví user (increase = true).
                    - Trừ tiền trong ví user (increase = false).
                    
                    amountCentsAbs: số tiền tuyệt đối (luôn > 0).
                    increase:
                    - true  => cộng tiền (bonus)
                    - false => trừ tiền (penalty)
                    
                    Ví dụ: Thưởng 100k cho teacher #10:
                    {
                      "userId": 10,
                      "amountCentsAbs": 100000,
                      "increase": true,
                      "description": "Bonus for good performance"
                    }
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Điều chỉnh ví thành công",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = WalletTransactionResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ (VD: trừ quá số dư)"),
            @ApiResponse(responseCode = "403", description = "Không có quyền (không phải ADMIN)")
    })
    public WalletTransactionResponse adminAdjustWallet(
            @Valid @RequestBody AdminAdjustRequest req
    ) {
        Long adminId = getCurrentUserIdOrThrow();

        long amount = req.getAmountCentsAbs();
        if (!req.getIncrease()) {
            amount = -amount;
        }

        WalletTransaction tx = walletService.adminAdjustBalance(
                req.getUserId(),
                amount,
                req.getDescription(),
                adminId
        );

        return WalletTransactionResponse.fromEntity(tx);
    }
}
