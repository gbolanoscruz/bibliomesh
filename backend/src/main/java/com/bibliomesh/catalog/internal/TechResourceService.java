package com.bibliomesh.catalog.internal;

import com.bibliomesh.catalog.TechResourceDtos.TechResourceResponse;
import com.bibliomesh.catalog.TechResourceDtos.UpsertTechResourceRequest;
import com.bibliomesh.catalog.TechResourceStatus;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TechResourceService {

    private final TechResourceRepository repository;

    public TechResourceService(TechResourceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TechResourceResponse upsert(UpsertTechResourceRequest req) {
        String tag = req.assetTag().trim().toUpperCase();
        TechResource resource = repository.findByAssetTagIgnoreCase(tag)
                .orElseGet(TechResource::new);

        resource.setAssetTag(tag);
        resource.setName(req.name().trim());
        resource.setResourceType(req.resourceType());
        resource.setBrand(trimOrNull(req.brand()));
        resource.setModel(trimOrNull(req.model()));
        resource.setSerialNumber(trimOrNull(req.serialNumber()));
        resource.setConditionNote(trimOrNull(req.conditionNote()));
        resource.setObservations(trimOrNull(req.observations()));
        if (req.status() != null) {
            resource.setStatus(req.status());
        } else if (resource.getId() == null) {
            resource.setStatus(TechResourceStatus.AVAILABLE);
        }
        resource.setUpdatedAt(OffsetDateTime.now());

        return toResponse(repository.save(resource));
    }

    @Transactional(readOnly = true)
    public List<TechResourceResponse> search(String query) {
        List<TechResource> items = (query == null || query.isBlank())
                ? repository.findAllByOrderByNameAsc()
                : repository.search(query.trim());
        return items.stream().map(r -> toResponse(r)).toList();
    }

    @Transactional(readOnly = true)
    public TechResourceResponse getByAssetTag(String assetTag) {
        TechResource resource = repository.findByAssetTagIgnoreCase(assetTag.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Tech resource not found: " + assetTag));
        return toResponse(resource);
    }

    static TechResourceResponse toResponse(TechResource r) {
        return new TechResourceResponse(
                r.getId(), r.getAssetTag(), r.getName(), r.getResourceType(),
                r.getBrand(), r.getModel(), r.getSerialNumber(),
                r.getConditionNote(), r.getObservations(), r.getStatus(), r.getCreatedAt());
    }

    private static String trimOrNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}