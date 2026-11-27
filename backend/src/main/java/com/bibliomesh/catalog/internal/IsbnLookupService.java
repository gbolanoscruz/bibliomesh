package com.bibliomesh.catalog.internal;

import com.bibliomesh.catalog.CatalogDtos.IsbnLookupResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class IsbnLookupService {

    private static final Logger log = LoggerFactory.getLogger(IsbnLookupService.class);
    private static final String GOOGLE_BOOKS_BASE = "https://www.googleapis.com/books/v1";
    private static final String OPEN_LIBRARY_BASE = "https://openlibrary.org";

    private final BibliographicTitleRepository titleRepository;
    private final BookCopyRepository copyRepository;
    private final RestClient googleBooksClient;
    private final RestClient openLibraryClient;

    @Autowired
    public IsbnLookupService(
            BibliographicTitleRepository titleRepository,
            BookCopyRepository copyRepository) {
        this(
                titleRepository,
                copyRepository,
                RestClient.create(GOOGLE_BOOKS_BASE),
                RestClient.create(OPEN_LIBRARY_BASE));
    }

    IsbnLookupService(
            BibliographicTitleRepository titleRepository,
            BookCopyRepository copyRepository,
            RestClient googleBooksClient,
            RestClient openLibraryClient) {
        this.titleRepository = titleRepository;
        this.copyRepository = copyRepository;
        this.googleBooksClient = googleBooksClient;
        this.openLibraryClient = openLibraryClient;
    }

    public String normalizeIsbn(String rawIsbn) {
        if (rawIsbn == null) {
            return "";
        }
        return rawIsbn.replaceAll("[^0-9Xx]", "").toUpperCase();
    }

    public IsbnLookupResponse lookup(String rawIsbn) {
        String isbn = normalizeIsbn(rawIsbn);
        if (isbn.length() != 10 && isbn.length() != 13) {
            throw new IllegalArgumentException("ISBN must contain 10 or 13 characters: " + rawIsbn);
        }

        Optional<BibliographicTitle> existing = titleRepository.findFirstByIsbn(isbn);
        if (existing.isPresent()) {
            BibliographicTitle title = existing.get();
            long copies = copyRepository.countByTitleId(title.getId());
            return new IsbnLookupResponse(
                    isbn, true, "LOCAL_CATALOG", true, title.getId(), copies,
                    title.getTitle(), title.getSubtitle(), title.getAuthor(),
                    title.getPublisher(), title.getPublicationYear(),
                    title.getDescriptors(), title.getSynopsis(), title.getCoverUrl());
        }

        Optional<ExternalMetadata> google = fetchFromGoogleBooks(isbn);
        if (google.isPresent()) {
            ExternalMetadata meta = google.get();
            String cover = meta.coverUrl();
            if (cover == null || cover.isBlank()) {
                cover = fetchFromOpenLibrary(isbn).map(meta2 -> meta2.coverUrl()).orElse(null);            }
            return new IsbnLookupResponse(
                    isbn, true, "GOOGLE_BOOKS", false, null, 0,
                    meta.title(), meta.subtitle(), meta.author(),
                    meta.publisher(), meta.publicationYear(),
                    meta.descriptors(), meta.synopsis(), cover);
        }

        Optional<ExternalMetadata> openLib = fetchFromOpenLibrary(isbn);
        if (openLib.isPresent()) {
            ExternalMetadata meta = openLib.get();
            return new IsbnLookupResponse(
                    isbn, true, "OPEN_LIBRARY", false, null, 0,
                    meta.title(), meta.subtitle(), meta.author(),
                    meta.publisher(), meta.publicationYear(),
                    meta.descriptors(), meta.synopsis(), meta.coverUrl());
        }

        return new IsbnLookupResponse(
                isbn, false, "NONE", false, null, 0,
                null, null, null, null, null, List.of(), null, null);
    }

    private Optional<ExternalMetadata> fetchFromGoogleBooks(String isbn) {
        try {
            GoogleBooksResponse body = googleBooksClient.get()
                    .uri("/volumes?q=isbn:{isbn}", isbn)
                    .retrieve()
                    .body(GoogleBooksResponse.class);

            if (body == null || body.items() == null || body.items().isEmpty()) {
                return Optional.empty();
            }
            GoogleVolumeInfo info = body.items().getFirst().volumeInfo();
            if (info == null || info.title() == null) {
                return Optional.empty();
            }
            String author = info.authors() != null ? String.join(", ", info.authors()) : null;
            String year = extractYear(info.publishedDate());
            String cover = info.imageLinks() != null ? toHttps(info.imageLinks().thumbnail()) : null;
            List<String> categories = info.categories() != null ? info.categories() : List.of();

            return Optional.of(new ExternalMetadata(
                    info.title(), info.subtitle(), author,
                    info.publisher(), year, categories, info.description(), cover));
        } catch (Exception ex) {
            log.warn("Google Books lookup failed for ISBN {}: {}", isbn, ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<ExternalMetadata> fetchFromOpenLibrary(String isbn) {
        try {
            String key = "ISBN:" + isbn;
            Map<String, OpenLibraryBook> response = openLibraryClient.get()
                    .uri("/api/books?bibkeys={key}&format=json&jscmd=data", key)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || !response.containsKey(key)) {
                return Optional.empty();
            }
            OpenLibraryBook book = response.get(key);
            if (book == null || book.title() == null) {
                return Optional.empty();
            }
            String author = book.authors() != null
                ? String.join(", ", book.authors().stream().map(item -> item.name()).toList())                    : null;
            String publisher = (book.publishers() != null && !book.publishers().isEmpty())
                    ? book.publishers().getFirst().name()
                    : null;
            List<String> subjects = book.subjects() != null
                    ? book.subjects().stream().map(item -> item.name()).limit(3).toList()                    : List.of();
            String cover = book.cover() != null
                    ? (book.cover().large() != null ? book.cover().large() : book.cover().medium())
                    : null;

            return Optional.of(new ExternalMetadata(
                    book.title(), book.subtitle(), author,
                    publisher, extractYear(book.publish_date()), subjects, null, cover));
        } catch (Exception ex) {
            log.warn("Open Library lookup failed for ISBN {}: {}", isbn, ex.getMessage());
            return Optional.empty();
        }
    }

    private static String extractYear(String date) {
        if (date == null || date.length() < 4) {
            return date;
        }
        return date.replaceAll(".*?(\\d{4}).*", "$1");
    }

    private static String toHttps(String url) {
        return url != null ? url.replace("http://", "https://") : null;
    }

    private record ExternalMetadata(
            String title, String subtitle, String author, String publisher,
            String publicationYear, List<String> descriptors, String synopsis, String coverUrl) {}

    private record GoogleBooksResponse(List<GoogleItem> items) {}
    private record GoogleItem(GoogleVolumeInfo volumeInfo) {}
    private record GoogleVolumeInfo(
            String title, String subtitle, List<String> authors, String publisher,
            String publishedDate, String description, List<String> categories,
            GoogleImageLinks imageLinks) {}
    private record GoogleImageLinks(String thumbnail) {}

    private record OpenLibraryBook(
            String title, String subtitle, List<NamedItem> authors,
            List<NamedItem> publishers, String publish_date,
            List<NamedItem> subjects, OpenLibraryCover cover) {}
    private record NamedItem(String name) {}
    private record OpenLibraryCover(String medium, String large) {}
}