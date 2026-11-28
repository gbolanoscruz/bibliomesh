package com.bibliomesh.catalog;

import com.bibliomesh.catalog.CatalogDtos.AddCopyRequest;
import com.bibliomesh.catalog.CatalogDtos.BookCopyResponse;
import com.bibliomesh.catalog.CatalogDtos.CreateTitleRequest;
import com.bibliomesh.catalog.CatalogDtos.IsbnLookupResponse;
import com.bibliomesh.catalog.CatalogDtos.LibrarySettingsResponse;
import com.bibliomesh.catalog.CatalogDtos.TitleResponse;
import com.bibliomesh.catalog.CatalogDtos.UpdateSettingsRequest;
import com.bibliomesh.catalog.internal.CatalogService;
import com.bibliomesh.catalog.internal.IsbnLookupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalogService;
    private final IsbnLookupService isbnLookupService;
    private final com.bibliomesh.auth.AuthTokenVerifier authTokenVerifier;

    public CatalogController(
            CatalogService catalogService,
            IsbnLookupService isbnLookupService,
            com.bibliomesh.auth.AuthTokenVerifier authTokenVerifier) {
        this.catalogService = catalogService;
        this.isbnLookupService = isbnLookupService;
        this.authTokenVerifier = authTokenVerifier;
    }

    @org.springframework.web.bind.annotation.PatchMapping("/copies/{copyId}/status")
    public BookCopyResponse updateCopyStatus(
            @PathVariable Long copyId,
            @org.springframework.web.bind.annotation.RequestHeader(name = "Authorization", required = false) String authorization,
            @Valid @RequestBody com.bibliomesh.catalog.CatalogDtos.UpdateCopyStatusRequest request) {
        var principal = authTokenVerifier.requireAdmin(authorization);
        return catalogService.updateCopyStatus(copyId, request, principal);
    }

    @GetMapping("/isbn/{isbn}")
    public IsbnLookupResponse lookupIsbn(@PathVariable String isbn) {
        try {
            return isbnLookupService.lookup(isbn);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @GetMapping("/titles")
    public com.bibliomesh.catalog.CatalogDtos.PagedTitlesResponse searchTitles(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "school", required = false) String school,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "24") int size) {
        return catalogService.searchTitles(query, school, page, size);
    }

    @GetMapping("/titles/{id}")
    public TitleResponse getTitle(@PathVariable Long id) {
        return catalogService.getTitle(id);
    }

    @PostMapping("/titles")
    @ResponseStatus(HttpStatus.CREATED)
    public TitleResponse createTitle(@Valid @RequestBody CreateTitleRequest request) {
        return catalogService.createTitleWithFirstCopy(request);
    }

    @PostMapping("/titles/{id}/copies")
    @ResponseStatus(HttpStatus.CREATED)
    public BookCopyResponse addCopy(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) AddCopyRequest request) {
        return catalogService.addCopy(id, request);
    }

    @GetMapping("/settings")
    public LibrarySettingsResponse getSettings() {
        return catalogService.getSettings();
    }

    @PutMapping("/settings")
    public LibrarySettingsResponse updateSettings(@Valid @RequestBody UpdateSettingsRequest request) {
        return catalogService.updateSettings(request);
    }
}