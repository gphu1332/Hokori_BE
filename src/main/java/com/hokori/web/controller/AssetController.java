package com.hokori.web.controller;

import com.hokori.web.Enum.AssetType;
import com.hokori.web.dto.asset.AssetCreateRequest;
import com.hokori.web.dto.asset.AssetResponse;
import com.hokori.web.service.AssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Assets", description = "Manage course/media assets (image, video, audio, pdf...)")
@SecurityRequirement(name = "bearerAuth") // tên security scheme trong openapi config
@CrossOrigin(origins = "*")               // tùy chỉnh CORS theo môi trường của bạn
@RestController
@RequestMapping(value = "/api/assets", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AssetController {

    private final AssetService service;

    @Operation(
            summary = "Create asset metadata",
            description = "Tạo metadata cho file đã upload (LOCAL/S3...). " +
                    "Yêu cầu ownerId là user hợp lệ."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created",
                    content = @Content(schema = @Schema(implementation = AssetResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Owner not found", content = @Content)
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AssetResponse> create(
            @Parameter(description = "Chủ sở hữu asset (teacher/admin)", required = true)
            @RequestParam Long ownerId,
            @Valid @RequestBody AssetCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(ownerId, req));
    }

    @Operation(
            summary = "List assets by owner (optionally filter by type)",
            description = "Liệt kê asset theo ownerId; có thể lọc theo type và phân trang."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    @GetMapping
    public Page<AssetResponse> list(
            @Parameter(description = "Chủ sở hữu asset", required = true)
            @RequestParam Long ownerId,
            @Parameter(description = "Lọc theo loại asset (VIDEO/AUDIO/IMAGE/PDF/SUBTITLE/CAPTION/OTHER)")
            @RequestParam(required = false) AssetType type,
            @ParameterObject
            @PageableDefault(size = 20) Pageable pageable) {
        return service.list(ownerId, type, pageable);
    }

    @Operation(
            summary = "Get asset detail",
            description = "Lấy thông tin chi tiết một asset theo id."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(schema = @Schema(implementation = AssetResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Asset not found", content = @Content)
    })
    @GetMapping("/{id}")
    public AssetResponse get(
            @Parameter(description = "ID của asset", required = true)
            @PathVariable Long id) {
        return service.get(id);
    }

    @Operation(
            summary = "Update asset metadata",
            description = "Cập nhật metadata (title/description/visibility/...) của asset."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(schema = @Schema(implementation = AssetResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Asset not found", content = @Content)
    })
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AssetResponse update(
            @Parameter(description = "ID của asset", required = true)
            @PathVariable Long id,
            @Valid @RequestBody AssetCreateRequest req) {
        return service.update(id, req);
    }

    @Operation(
            summary = "Delete asset (soft delete)",
            description = "Xoá mềm asset. Nếu Entity có @SQLDelete thì sẽ set deleted_flag = 1."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Asset not found", content = @Content)
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(description = "ID của asset", required = true)
            @PathVariable Long id) {
        service.delete(id);
    }
}
