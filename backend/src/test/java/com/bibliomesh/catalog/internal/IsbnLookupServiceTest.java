package com.bibliomesh.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bibliomesh.catalog.CatalogDtos.IsbnLookupResponse;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class IsbnLookupServiceTest {

    private BibliographicTitleRepository titleRepository;
    private BookCopyRepository copyRepository;
    private IsbnLookupService service;

    @BeforeEach
    void setUp() {
        titleRepository = mock(BibliographicTitleRepository.class);
        copyRepository = mock(BookCopyRepository.class);
        service = new IsbnLookupService(
                titleRepository,
                copyRepository,
                mock(RestClient.class),
                mock(RestClient.class));
    }

    @Test
    void normalizesHyphensAndSpacesFromScannerInput() {
        assertThat(service.normalizeIsbn("978-0-13-468599-1")).isEqualTo("9780134685991");
        assertThat(service.normalizeIsbn(" 0-306-40615-x ")).isEqualTo("030640615X");
    }

    @Test
    void rejectsInvalidIsbnLength() {
        assertThatThrownBy(() -> service.lookup("12345"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10 or 13");
    }

    @Test
    void returnsLocalCatalogMatchBeforeCallingExternalApis() {
        BibliographicTitle existing = new BibliographicTitle();
        existing.setIsbn("9780134685991");
        existing.setTitle("Effective Java");
        existing.setAuthor("Joshua Bloch");

        when(titleRepository.findFirstByIsbn("9780134685991")).thenReturn(Optional.of(existing));
        when(copyRepository.countByTitleId(null)).thenReturn(3L);

        IsbnLookupResponse response = service.lookup("978-0-13-468599-1");

        assertThat(response.found()).isTrue();
        assertThat(response.alreadyInCatalog()).isTrue();
        assertThat(response.source()).isEqualTo("LOCAL_CATALOG");
        assertThat(response.existingCopyCount()).isEqualTo(3L);
        assertThat(response.title()).isEqualTo("Effective Java");
    }
}