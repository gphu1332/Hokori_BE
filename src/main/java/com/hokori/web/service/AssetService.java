package com.hokori.web.service;

import com.hokori.web.Enum.AssetStatus;
import com.hokori.web.Enum.AssetType;
import com.hokori.web.Enum.AssetVisibility;
import com.hokori.web.dto.asset.AssetCreateRequest;
import com.hokori.web.dto.asset.AssetResponse;
import com.hokori.web.entity.Asset;
import com.hokori.web.entity.User;
import com.hokori.web.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AssetService {
    private final AssetRepository repo;
    private final UserService userService;

    public AssetResponse create(Long ownerId, AssetCreateRequest req) {
        User owner = userService.getUserById(ownerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found: " + ownerId));

        Asset a = Asset.builder()
                .type(req.getType())
                .owner(owner)
                .title(req.getTitle())
                .description(req.getDescription())
                .fileName(req.getFileName())
                .extension(req.getExtension())
                .mimeType(req.getMimeType())
                .sizeBytes(req.getSizeBytes())
                .checksumSha256(req.getChecksumSha256())
                .relativePath(req.getRelativePath())
                .publicUrl(req.getPublicUrl())
                .visibility(Objects.requireNonNullElse(req.getVisibility(), AssetVisibility.PRIVATE))
                .durationSec(req.getDurationSec())
                .width(req.getWidth())
                .height(req.getHeight())
                .status(AssetStatus.READY)
                .build();

        return toResp(repo.save(a));
    }

    public Page<AssetResponse> list(Long ownerId, AssetType type, Pageable pageable) {
        return (type == null
                ? repo.findByOwner_Id(ownerId, pageable)
                : repo.findByOwnerAndType(ownerId, type, pageable))
                .map(this::toResp);
    }

    public AssetResponse get(Long id) {
        Asset a = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Asset not found: " + id));
        return toResp(a);
    }

    public AssetResponse update(Long id, AssetCreateRequest req) {
        Asset a = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Asset not found: " + id));

        a.setTitle(req.getTitle());
        a.setDescription(req.getDescription());
        a.setVisibility(Objects.requireNonNullElse(req.getVisibility(), a.getVisibility()));
        a.setPublicUrl(req.getPublicUrl());
        a.setDurationSec(req.getDurationSec());
        a.setWidth(req.getWidth());
        a.setHeight(req.getHeight());
        a.setMimeType(req.getMimeType());
        a.setExtension(req.getExtension());
        a.setSizeBytes(req.getSizeBytes());

        return toResp(repo.save(a));
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found: " + id);
        }
        repo.deleteById(id);
    }

    private AssetResponse toResp(Asset a) {
        AssetResponse r = new AssetResponse();
        r.setId(a.getId());
        r.setType(a.getType());
        r.setTitle(a.getTitle());
        r.setDescription(a.getDescription());
        r.setFileName(a.getFileName());
        r.setMimeType(a.getMimeType());
        r.setSizeBytes(a.getSizeBytes());
        r.setPublicUrl(a.getPublicUrl());
        r.setVisibility(a.getVisibility());
        r.setDurationSec(a.getDurationSec());
        r.setWidth(a.getWidth());
        r.setHeight(a.getHeight());
        r.setStatus(a.getStatus());
        r.setCreatedAt(a.getCreatedAt());
        return r;
    }
}
