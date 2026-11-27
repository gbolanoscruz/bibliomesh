package com.bibliomesh.catalog.internal;

import com.bibliomesh.catalog.CatalogApi;
import com.bibliomesh.catalog.CatalogDtos.LibrarySettingsResponse;
import com.bibliomesh.catalog.CatalogDtos.TitleResponse;
import com.bibliomesh.catalog.CopyStatus;
import com.bibliomesh.catalog.TechResourceDtos.TechResourceResponse;
import com.bibliomesh.catalog.TechResourceStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CatalogApiImpl implements CatalogApi {

    private final BookCopyRepository copyRepository;
    private final TechResourceRepository techResourceRepository;
    private final CatalogService catalogService;
    private final TechResourceService techResourceService;

    public CatalogApiImpl(
            BookCopyRepository copyRepository,
            TechResourceRepository techResourceRepository,
            CatalogService catalogService,
            TechResourceService techResourceService) {
        this.copyRepository = copyRepository;
        this.techResourceRepository = techResourceRepository;
        this.catalogService = catalogService;
        this.techResourceService = techResourceService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CopySummary> findCopyByInscription(String inscriptionNumber) {
        if (inscriptionNumber == null || inscriptionNumber.isBlank()) {
            return Optional.empty();
        }
        return copyRepository.findByInscriptionNumberIgnoreCase(inscriptionNumber.trim())
                .map(c -> toCopySummary(c));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CopySummary> findCopyById(Long copyId) {
        return copyRepository.findById(copyId).map(c -> toCopySummary(c));
    }

    @Override
    @Transactional
    public void updateCopyStatus(Long copyId, CopyStatus status) {
        BookCopy copy = copyRepository.findById(copyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book copy not found: " + copyId));
        copy.setStatus(status);
        copy.setUpdatedAt(OffsetDateTime.now());
        copyRepository.save(copy);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TechResourceSummary> findTechResourceByAssetTag(String assetTag) {
        if (assetTag == null || assetTag.isBlank()) {
            return Optional.empty();
        }
        return techResourceRepository.findByAssetTagIgnoreCase(assetTag.trim())
                .map(r -> toTechSummary(r));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TechResourceSummary> findTechResourceById(Long techResourceId) {
        return techResourceRepository.findById(techResourceId).map(r -> toTechSummary(r));
    }

    @Override
    @Transactional
    public void updateTechResourceStatus(Long techResourceId, TechResourceStatus status) {
        TechResource resource = techResourceRepository.findById(techResourceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Tech resource not found: " + techResourceId));
        resource.setStatus(status);
        resource.setUpdatedAt(OffsetDateTime.now());
        techResourceRepository.save(resource);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TitleResponse> searchCatalog(String query) {
        return catalogService.searchTitles(query);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TechResourceResponse> listTechResources() {
        return techResourceService.search(null);
    }

    @Override
    @Transactional(readOnly = true)
    public LibrarySettingsResponse getLibrarySettings() {
        return catalogService.getSettings();
    }

    @Override
    @Transactional(readOnly = true)
    public long countBookCopies() {
        return copyRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countTechResources() {
        return techResourceRepository.count();
    }

    private static CopySummary toCopySummary(BookCopy c) {
        BibliographicTitle t = c.getTitle();
        return new CopySummary(
                c.getId(), t.getId(), c.getInscriptionNumber(),
                t.getTitle(), t.getDeweyCode(), t.getCutterCode(), c.getStatus());
    }

    private static TechResourceSummary toTechSummary(TechResource r) {
        return new TechResourceSummary(r.getId(), r.getAssetTag(), r.getName(), r.getResourceType(), r.getStatus());
    }
}