package com.bibliomesh.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public final class TechResourceDtos {

    private TechResourceDtos() {}

    public record UpsertTechResourceRequest(
            @NotBlank @Size(max = 64) String assetTag,
            @NotBlank @Size(max = 200) String name,
            @NotNull TechResourceType resourceType,
            @Size(max = 100) String brand,
            @Size(max = 100) String model,
            @Size(max = 120) String serialNumber,
            @Size(max = 120) String conditionNote,
            String observations,
            TechResourceStatus status
    ) {}

    public record TechResourceResponse(
            Long id,
            String assetTag,
            String name,
            TechResourceType resourceType,
            String brand,
            String model,
            String serialNumber,
            String conditionNote,
            String observations,
            TechResourceStatus status,
            OffsetDateTime createdAt
    ) {}
}