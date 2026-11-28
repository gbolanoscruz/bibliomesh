package com.bibliomesh.catalog.internal;

import com.bibliomesh.catalog.CatalogDtos.AddCopyRequest;
import com.bibliomesh.catalog.CatalogDtos.BookCopyResponse;
import com.bibliomesh.catalog.CatalogDtos.CreateTitleRequest;
import com.bibliomesh.catalog.CatalogDtos.LibrarySettingsResponse;
import com.bibliomesh.catalog.CatalogDtos.TitleResponse;
import com.bibliomesh.catalog.CatalogDtos.UpdateCopyStatusRequest;
import com.bibliomesh.catalog.CatalogDtos.UpdateSettingsRequest;
import com.bibliomesh.catalog.CopyStatus;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CatalogService {

    private final BibliographicTitleRepository titleRepository;
    private final BookCopyRepository copyRepository;
    private final LibrarySettingsRepository settingsRepository;
    private final IsbnLookupService isbnLookupService;

    public CatalogService(
            BibliographicTitleRepository titleRepository,
            BookCopyRepository copyRepository,
            LibrarySettingsRepository settingsRepository,
            IsbnLookupService isbnLookupService) {
        this.titleRepository = titleRepository;
        this.copyRepository = copyRepository;
        this.settingsRepository = settingsRepository;
        this.isbnLookupService = isbnLookupService;
    }

    @Transactional
    public TitleResponse createTitleWithFirstCopy(CreateTitleRequest req) {
        BibliographicTitle title = new BibliographicTitle();
        String normalizedIsbn = isbnLookupService.normalizeIsbn(req.isbn());
        title.setIsbn(normalizedIsbn.isEmpty() ? null : normalizedIsbn);
        title.setTitle(req.title().trim());
        title.setSubtitle(trimOrNull(req.subtitle()));
        title.setPreferredTitle(trimOrNull(req.preferredTitle()));
        title.setAuthor(trimOrNull(req.author()));
        title.setPublisher(trimOrNull(req.publisher()));
        title.setPublicationPlace(trimOrNull(req.publicationPlace()));
        title.setPublicationYear(trimOrNull(req.publicationYear()));
        title.setSeries(trimOrNull(req.series()));
        title.setVolume(trimOrNull(req.volume()));
        title.setDeweyCode(trimOrNull(req.deweyCode()));
        title.setCutterCode(trimOrNull(req.cutterCode()));
        title.setDescriptors(req.descriptors());
        title.setSynopsis(trimOrNull(req.synopsis()));
        title.setGeneralNote(trimOrNull(req.generalNote()));
        title.setCoverUrl(trimOrNull(req.coverUrl()));
        title.setMarcExtras(req.marcExtras());

        BibliographicTitle savedTitle = titleRepository.save(title);
        BookCopy firstCopy = createCopyEntity(savedTitle, req.inscriptionNumber(), req.shelfLocation());
        return toTitleResponse(savedTitle, List.of(firstCopy));
    }

    @Transactional
    public BookCopyResponse addCopy(Long titleId, AddCopyRequest req) {
        BibliographicTitle title = titleRepository.findById(titleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Title not found: " + titleId));
        String inscription = req != null ? req.inscriptionNumber() : null;
        String shelf = req != null ? req.shelfLocation() : null;
        BookCopy saved = createCopyEntity(title, inscription, shelf);
        return toCopyResponse(saved);
    }

    @Transactional(readOnly = true)
    public com.bibliomesh.catalog.CatalogDtos.PagedTitlesResponse searchTitles(
            String query, String schoolCode, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        String cleanQuery = trimOrNull(query);
        String cleanSchool = trimOrNull(schoolCode);

        var pageable = org.springframework.data.domain.PageRequest.of(safePage, safeSize);
        var titlePage = titleRepository.searchPaged(cleanQuery, cleanSchool, pageable);

        List<BibliographicTitle> titles = titlePage.getContent();
        if (titles.isEmpty()) {
            return new com.bibliomesh.catalog.CatalogDtos.PagedTitlesResponse(
                    List.of(), titlePage.getNumber(), titlePage.getSize(),
                    titlePage.getTotalElements(), titlePage.getTotalPages());
        }

        List<Long> titleIds = titles.stream().map(t -> t.getId()).toList();
                java.util.Map<Long, List<BookCopy>> copiesByTitleId = copyRepository
                .findByTitleIdInOrderByInscriptionNumberAsc(titleIds)
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(c -> c.getTitle().getId()));

        List<TitleResponse> items = titles.stream()
                .map(t -> toTitleResponse(t, copiesByTitleId.getOrDefault(t.getId(), List.of())))
                .toList();

        return new com.bibliomesh.catalog.CatalogDtos.PagedTitlesResponse(
                items,
                titlePage.getNumber(),
                titlePage.getSize(),
                titlePage.getTotalElements(),
                titlePage.getTotalPages());
    }

    @Transactional(readOnly = true)
    public TitleResponse getTitle(Long titleId) {
        BibliographicTitle title = titleRepository.findById(titleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Title not found: " + titleId));
        return toTitleResponse(title, copyRepository.findByTitleIdOrderByInscriptionNumberAsc(titleId));
    }

    @Transactional(readOnly = true)
    public LibrarySettingsResponse getSettings() {
        LibrarySettings settings = settingsRepository.findById((short) 1)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Library settings missing"));
        return toSettingsResponse(settings);
    }

    @Transactional
    public LibrarySettingsResponse updateSettings(UpdateSettingsRequest req) {
        LibrarySettings settings = settingsRepository.findSingletonForUpdate()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Library settings missing"));
        settings.setLibraryName(req.libraryName().trim());
        settings.setInscriptionPrefix(req.inscriptionPrefix().trim().toUpperCase());
        settings.setLogoUrl(trimOrNull(req.logoUrl()));
        settings.setDigitalLibraryDriveFolderId(trimOrNull(req.digitalLibraryDriveFolderId()));
        settings.setUpdatedAt(OffsetDateTime.now());
        return toSettingsResponse(settingsRepository.save(settings));
    }

    private BookCopy createCopyEntity(BibliographicTitle title, String requestedInscription, String shelfLocation) {
        String inscription = trimOrNull(requestedInscription);
        if (inscription == null) {
            LibrarySettings settings = settingsRepository.findSingletonForUpdate()
                    .orElseThrow(() -> new IllegalStateException("library_settings singleton row not initialized"));
            inscription = settings.nextInscriptionNumber();
            settingsRepository.save(settings);
        } else if (copyRepository.existsByInscriptionNumberIgnoreCase(inscription)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Inscription number already exists: " + inscription);
        }

        BookCopy copy = new BookCopy();
        copy.setTitle(title);
        copy.setInscriptionNumber(inscription);
        copy.setBarcode(inscription);
        copy.setStatus(CopyStatus.AVAILABLE);
        copy.setShelfLocation(trimOrNull(shelfLocation));
        return copyRepository.save(copy);
    }

    private static TitleResponse toTitleResponse(BibliographicTitle t, List<BookCopy> copies) {
        return new TitleResponse(
                t.getId(), t.getIsbn(), t.getTitle(), t.getSubtitle(), t.getPreferredTitle(),
                t.getAuthor(), t.getPublisher(), t.getPublicationPlace(), t.getPublicationYear(),
                t.getSeries(), t.getVolume(), t.getDeweyCode(), t.getCutterCode(),
                t.getDescriptors(), t.getSynopsis(), t.getGeneralNote(), t.getCoverUrl(),
                t.getMarcExtras(), copies.stream().map(CatalogService::toCopyResponse).toList(),
                t.getCreatedAt());
    }

    private static BookCopyResponse toCopyResponse(BookCopy c) {
        return new BookCopyResponse(
                c.getId(), c.getInscriptionNumber(), c.getBarcode(),
                c.getStatus(), c.getShelfLocation(), c.isQueuedForLabel());
    }

    private static LibrarySettingsResponse toSettingsResponse(LibrarySettings s) {
        return new LibrarySettingsResponse(
                s.getLibraryCode(), s.getLibraryName(), s.getInscriptionPrefix(),
                s.getInscriptionSequence(), s.getLogoUrl(), s.getDigitalLibraryDriveFolderId());
    }

    private static String trimOrNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Transactional
    public BookCopyResponse updateCopyStatus(
            Long copyId,
            UpdateCopyStatusRequest request,
            com.bibliomesh.auth.AuthPrincipal principal) {
        BookCopy copy = copyRepository.findById(copyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ejemplar no encontrado: " + copyId));

        if (!principal.canManageSchool(copy.getSchoolId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permisos sobre esta escuela");
        }

        copy.setStatus(request.status());
        if (request.status() == CopyStatus.AVAILABLE) {
            copy.setUnavailableReason(null);
            copy.setUnavailableNotes(null);
        } else {
            copy.setUnavailableReason(request.unavailableReason());
            copy.setUnavailableNotes(request.unavailableNotes());
        }

        if (request.physicalCondition() != null) {
            copy.setPhysicalCondition(request.physicalCondition());
        }
        if (request.inLibrary() != null) {
            copy.setInLibrary(request.inLibrary());
            copy.setLastInventoryCheckAt(java.time.OffsetDateTime.now());
        }

        copy.setUpdatedAt(java.time.OffsetDateTime.now());
        BookCopy saved = copyRepository.save(copy);
        return toCopyResponse(saved);
    }
}