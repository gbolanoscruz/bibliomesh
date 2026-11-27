package com.bibliomesh.opac;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;

public final class OpacDtos {

    private OpacDtos() {}

    public record CreateDigitalResourceRequest(
            @NotBlank @Size(max = 300) String title,
            @Size(max = 250) String author,
            @Size(max = 120) String category,
            @NotBlank String resourceUrl,
            String description
    ) {}

    public record DigitalResourceResponse(
            Long id,
            String title,
            String author,
            String category,
            String resourceUrl,
            String description,
            OffsetDateTime createdAt
    ) {}

    public record DigitalLibraryResponse(
            String libraryName,
            String driveFolderId,
            List<DigitalResourceResponse> resources
    ) {}

    public record LabelDataResponse(
            String inscriptionNumber,
            String title,
            String deweyCode,
            String cutterCode
    ) {}
}