package com.bibliomesh.opac.internal;

import com.bibliomesh.catalog.CatalogApi;
import com.bibliomesh.catalog.CatalogApi.CopySummary;
import com.bibliomesh.catalog.CatalogDtos.LibrarySettingsResponse;
import com.bibliomesh.catalog.CatalogDtos.TitleResponse;
import com.bibliomesh.catalog.TechResourceDtos.TechResourceResponse;
import com.bibliomesh.opac.OpacDtos.CreateDigitalResourceRequest;
import com.bibliomesh.opac.OpacDtos.DigitalLibraryResponse;
import com.bibliomesh.opac.OpacDtos.DigitalResourceResponse;
import com.bibliomesh.opac.OpacDtos.LabelDataResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OpacService {

    private final CatalogApi catalogApi;
    private final DigitalResourceRepository digitalResourceRepository;

    public OpacService(CatalogApi catalogApi, DigitalResourceRepository digitalResourceRepository) {
        this.catalogApi = catalogApi;
        this.digitalResourceRepository = digitalResourceRepository;
    }

    @Transactional(readOnly = true)
    public List<TitleResponse> searchBooks(String query) {
        return catalogApi.searchCatalog(query);
    }

    @Transactional(readOnly = true)
    public List<TechResourceResponse> listTechResources() {
        return catalogApi.listTechResources();
    }

    @Transactional(readOnly = true)
    public DigitalLibraryResponse getDigitalLibrary() {
        LibrarySettingsResponse settings = catalogApi.getLibrarySettings();
        List<DigitalResourceResponse> items = digitalResourceRepository.findAllByOrderByTitleAsc().stream()
                .map(d -> toResponse(d))
                .toList();
        return new DigitalLibraryResponse(
                settings.libraryName(),
                settings.digitalLibraryDriveFolderId(),
                items);
    }

    @Transactional
    public DigitalResourceResponse addDigitalResource(CreateDigitalResourceRequest req) {
        DigitalResource entity = new DigitalResource();
        entity.setTitle(req.title().trim());
        entity.setAuthor(trimOrNull(req.author()));
        entity.setCategory(trimOrNull(req.category()));
        entity.setResourceUrl(req.resourceUrl().trim());
        entity.setDescription(trimOrNull(req.description()));
        return toResponse(digitalResourceRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public LabelDataResponse getLabelData(String inscriptionNumber) {
        CopySummary copy = catalogApi.findCopyByInscription(inscriptionNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Book copy not found for label: " + inscriptionNumber));
        return new LabelDataResponse(
                copy.inscriptionNumber(),
                copy.title(),
                copy.deweyCode(),
                copy.cutterCode());
    }

    private static DigitalResourceResponse toResponse(DigitalResource d) {
        return new DigitalResourceResponse(
                d.getId(), d.getTitle(), d.getAuthor(),
                d.getCategory(), d.getResourceUrl(), d.getDescription(), d.getCreatedAt());
    }

    private static String trimOrNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}