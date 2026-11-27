package com.bibliomesh.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public final class CatalogDtos {

    private CatalogDtos() {}

    public record IsbnLookupResponse(
            String isbn,
            boolean found,
            String source,
            boolean alreadyInCatalog,
            Long existingTitleId,
            long existingCopyCount,
            String title,
            String subtitle,
            String author,
            String publisher,
            String publicationYear,
            List<String> descriptors,
            String synopsis,
            String coverUrl
    ) {}

    public record CreateTitleRequest(
            @Size(max = 20) String isbn,
            @NotBlank @Size(max = 300) String title,
            @Size(max = 300) String subtitle,
            @Size(max = 300) String preferredTitle,
            @Size(max = 250) String author,
            @Size(max = 200) String publisher,
            @Size(max = 150) String publicationPlace,
            @Size(max = 32) String publicationYear,
            @Size(max = 200) String series,
            @Size(max = 64) String volume,
            @Size(max = 64) String deweyCode,
            @Size(max = 64) String cutterCode,
            List<String> descriptors,
            String synopsis,
            String generalNote,
            String coverUrl,
            Map<String, String> marcExtras,
            @Size(max = 64) String inscriptionNumber,
            @Size(max = 100) String shelfLocation
    ) {}

    public record AddCopyRequest(
            @Size(max = 64) String inscriptionNumber,
            @Size(max = 100) String shelfLocation
    ) {}

    public record BookCopyResponse(
            Long id,
            String inscriptionNumber,
            String barcode,
            CopyStatus status,
            String shelfLocation,
            boolean queuedForLabel
    ) {}

    public record TitleResponse(
            Long id,
            String isbn,
            String title,
            String subtitle,
            String preferredTitle,
            String author,
            String publisher,
            String publicationPlace,
            String publicationYear,
            String series,
            String volume,
            String deweyCode,
            String cutterCode,
            List<String> descriptors,
            String synopsis,
            String generalNote,
            String coverUrl,
            Map<String, String> marcExtras,
            List<BookCopyResponse> copies,
            OffsetDateTime createdAt
    ) {}

    public record LibrarySettingsResponse(
            String libraryCode,
            String libraryName,
            String inscriptionPrefix,
            int inscriptionSequence,
            String logoUrl,
            String digitalLibraryDriveFolderId
    ) {}

    public record UpdateSettingsRequest(
            @NotBlank @Size(max = 150) String libraryName,
            @NotBlank @Size(max = 16) String inscriptionPrefix,
            String logoUrl,
            @Size(max = 128) String digitalLibraryDriveFolderId
    ) {}
}