package com.bibliomesh.catalog;

import com.bibliomesh.catalog.CatalogDtos.LibrarySettingsResponse;
import com.bibliomesh.catalog.CatalogDtos.TitleResponse;
import com.bibliomesh.catalog.TechResourceDtos.TechResourceResponse;
import java.util.List;
import java.util.Optional;

public interface CatalogApi {

    record CopySummary(
            Long copyId,
            Long titleId,
            String inscriptionNumber,
            String title,
            String deweyCode,
            String cutterCode,
            CopyStatus status
    ) {}

    record TechResourceSummary(
            Long id,
            String assetTag,
            String name,
            TechResourceType resourceType,
            TechResourceStatus status
    ) {}

    Optional<CopySummary> findCopyByInscription(String inscriptionNumber);

    Optional<CopySummary> findCopyById(Long copyId);

    void updateCopyStatus(Long copyId, CopyStatus status);

    Optional<TechResourceSummary> findTechResourceByAssetTag(String assetTag);

    Optional<TechResourceSummary> findTechResourceById(Long techResourceId);

    void updateTechResourceStatus(Long techResourceId, TechResourceStatus status);

    List<TitleResponse> searchCatalog(String query);

    List<TechResourceResponse> listTechResources();

    LibrarySettingsResponse getLibrarySettings();

    long countBookCopies();

    long countTechResources();
}