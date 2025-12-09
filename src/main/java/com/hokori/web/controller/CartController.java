package com.hokori.web.controller;

import com.hokori.web.dto.ApiResponse;
import com.hokori.web.dto.cart.AddItemRequest;
import com.hokori.web.dto.cart.CartCheckoutInfo;
import com.hokori.web.dto.cart.CartResponse;
import com.hokori.web.dto.cart.UpdateItemRequest;
import com.hokori.web.service.CartService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Cart APIs – thao tác giỏ hàng của user đang đăng nhập (JWT).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/cart", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Tag(name = "Cart", description = "Quản lý giỏ hàng cho người dùng hiện tại (Bearer JWT)")
@SecurityRequirement(name = "Bearer Authentication")
public class CartController {

    private final CartService service;

    // ------------------------------------------------------------------------
    // GET /api/cart
    // ------------------------------------------------------------------------
    @Operation(
            summary = "Xem giỏ hàng hiện tại",
            description = "Trả về giỏ hàng của **user đang đăng nhập**; nếu chưa có sẽ tạo giỏ trống."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "OK",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                {"status":"success","message":"OK","data":{"cartId":5,"items":[],"selectedSubtotal":0}}
                """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> view() {
        return ResponseEntity.ok(ApiResponse.success("OK", service.view()));
    }

    // ------------------------------------------------------------------------
    // POST /api/cart/items
    // ------------------------------------------------------------------------
    @Operation(
            summary = "Thêm khóa học vào giỏ",
            description = """
            Thêm 1 khóa học vào giỏ của user hiện tại.
            - Mỗi khóa học chỉ có thể thêm vào cart 1 lần (quantity luôn = 1).
            - Nếu course đã có trong cart: trả về lỗi COURSE_ALREADY_IN_CART (409).
            - Chặn nếu user đã sở hữu khóa học (đã enroll): trả về lỗi COURSE_OWNED (409).
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Item added",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                {"status":"success","message":"Item added","data":{"cartId":5,"items":[{"itemId":12,"courseId":101,"quantity":1,"totalPrice":1990000,"selected":true}],"selectedSubtotal":1990000}}
                """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "COURSE_NOT_FOUND / BAD_REQUEST"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "COURSE_OWNED / COURSE_ALREADY_IN_CART")
    })
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AddItemRequest.class),
                    examples = @ExampleObject(
                            value = "{ \"courseId\": 101, \"quantity\": 1 }"
                    )
            )
    )
    @PostMapping(value = "/items", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CartResponse>> add(
            @org.springframework.web.bind.annotation.RequestBody @Valid AddItemRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Item added", service.add(req)));
    }

    // ------------------------------------------------------------------------
    // PATCH /api/cart/items/{itemId}
    // ------------------------------------------------------------------------
    @Operation(
            summary = "Cập nhật 1 dòng trong giỏ",
            description = """
            Cập nhật cart item (chỉ có thể thay đổi trạng thái `selected`).
            - Với khóa học: `quantity` phải luôn = 1 (không thể thay đổi).
            - Nếu gửi `quantity` khác 1: trả về lỗi BAD_QUANTITY (400).
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Item updated",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                {"status":"success","message":"Item updated","data":{"cartId":5,"items":[{"itemId":12,"courseId":101,"quantity":1,"totalPrice":1990000,"selected":false}],"selectedSubtotal":0}}
                """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "BAD_QUANTITY (quantity must be 1) / COURSE_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "ITEM_NOT_FOUND")
    })
    @PatchMapping(value = "/items/{itemId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CartResponse>> update(
            @Parameter(name = "itemId", in = ParameterIn.PATH, required = true, description = "ID dòng giỏ hàng", example = "12")
            @PathVariable Long itemId,
            @org.springframework.web.bind.annotation.RequestBody @Valid UpdateItemRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Item updated", service.update(itemId, req)));
    }

    // ------------------------------------------------------------------------
    // DELETE /api/cart/items/{itemId}
    // ------------------------------------------------------------------------
    @Operation(summary = "Xóa 1 dòng khỏi giỏ", description = "Chỉ xóa item thuộc giỏ của user hiện tại.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Item removed",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                {"status":"success","message":"Item removed","data":{"cartId":5,"items":[],"selectedSubtotal":0}}
                """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "ITEM_NOT_FOUND")
    })
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> remove(
            @Parameter(name = "itemId", in = ParameterIn.PATH, required = true, description = "ID dòng giỏ hàng", example = "12")
            @PathVariable Long itemId) {
        return ResponseEntity.ok(ApiResponse.success("Item removed", service.remove(itemId)));
    }

    // ------------------------------------------------------------------------
    // PATCH /api/cart/items/select-all
    // ------------------------------------------------------------------------
    @Operation(summary = "Chọn/Bỏ chọn tất cả item", description = "Đặt `selected` cho toàn bộ item trong giỏ.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Selection changed",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                {"status":"success","message":"Selection changed","data":{"cartId":5,"items":[{"itemId":12,"courseId":101,"quantity":2,"totalPrice":3980000,"selected":false}],"selectedSubtotal":0}}
                """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PatchMapping("/items/select-all")
    public ResponseEntity<ApiResponse<CartResponse>> selectAll(
            @Parameter(name = "selected", in = ParameterIn.QUERY, required = true, description = "true: chọn hết; false: bỏ chọn hết", example = "true")
            @RequestParam boolean selected) {
        return ResponseEntity.ok(ApiResponse.success("Selection changed", service.selectAll(selected)));
    }

    // ------------------------------------------------------------------------
    // DELETE /api/cart/items
    // ------------------------------------------------------------------------
    @Operation(summary = "Xóa toàn bộ giỏ", description = "Xóa tất cả item (orphanRemoval).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Cart cleared",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                {"status":"success","message":"Cart cleared","data":{"cartId":5,"items":[],"selectedSubtotal":0}}
                """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> clear() {
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", service.clear()));
    }

    // ------------------------------------------------------------------------
    // GET /api/cart/checkout-info
    // ------------------------------------------------------------------------
    @Operation(
            summary = "Lấy thông tin checkout đơn giản",
            description = "Trả về cartId và danh sách courseIds đã được chọn (selected). Dùng để đơn giản hóa request khi thanh toán."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "OK",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                {"status":"success","message":"OK","data":{"cartId":5,"courseIds":[101,102]}}
                """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/checkout-info")
    public ResponseEntity<ApiResponse<CartCheckoutInfo>> getCheckoutInfo() {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getCheckoutInfo()));
    }
}
