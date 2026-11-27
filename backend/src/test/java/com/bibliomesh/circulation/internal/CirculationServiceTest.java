package com.bibliomesh.circulation.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bibliomesh.catalog.CatalogApi;
import com.bibliomesh.catalog.CatalogApi.CopySummary;
import com.bibliomesh.catalog.CopyStatus;
import com.bibliomesh.circulation.CirculationDtos.CheckoutRequest;
import com.bibliomesh.circulation.CirculationDtos.LoanResponse;
import com.bibliomesh.circulation.LoanStatus;
import com.bibliomesh.patrons.PatronDtos.PatronSummary;
import com.bibliomesh.patrons.PatronType;
import com.bibliomesh.patrons.PatronsApi;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class CirculationServiceTest {

    private LoanRepository loanRepository;
    private RoomConsultationRepository roomConsultationRepository;
    private CatalogApi catalogApi;
    private PatronsApi patronsApi;
    private CirculationService service;

    @BeforeEach
    void setUp() {
        loanRepository = mock(LoanRepository.class);
        roomConsultationRepository = mock(RoomConsultationRepository.class);
        catalogApi = mock(CatalogApi.class);
        patronsApi = mock(PatronsApi.class);
        service = new CirculationService(loanRepository, roomConsultationRepository, catalogApi, patronsApi);
    }

    @Test
    void checksOutAvailableBookAndMarksCopyAsLoaned() {
        PatronSummary patron = new PatronSummary(10L, "1-1111-2222", "Ana Mora", PatronType.STUDENT, true);
        CopySummary copy = new CopySummary(5L, 2L, "LIB00001", "El Principito", "843", "S237p", CopyStatus.AVAILABLE);

        when(patronsApi.findSummaryByIdentification("1-1111-2222")).thenReturn(Optional.of(patron));
        when(catalogApi.findCopyByInscription("LIB00001")).thenReturn(Optional.of(copy));
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        LoanResponse res = service.checkout(
                new CheckoutRequest("1-1111-2222", "LIB00001", LocalDate.of(2026, 12, 10), null));

        assertThat(res.itemType()).isEqualTo("BOOK");
        assertThat(res.itemCode()).isEqualTo("LIB00001");
        assertThat(res.status()).isEqualTo(LoanStatus.ACTIVE);
        verify(catalogApi).updateCopyStatus(5L, CopyStatus.LOANED);
    }

    @Test
    void rejectsCheckoutWhenCopyIsAlreadyLoaned() {
        PatronSummary patron = new PatronSummary(10L, "1-1111-2222", "Ana Mora", PatronType.STUDENT, true);
        CopySummary copy = new CopySummary(5L, 2L, "LIB00001", "El Principito", "843", "S237p", CopyStatus.LOANED);

        when(patronsApi.findSummaryByIdentification("1-1111-2222")).thenReturn(Optional.of(patron));
        when(catalogApi.findCopyByInscription("LIB00001")).thenReturn(Optional.of(copy));

        assertThatThrownBy(() -> service.checkout(new CheckoutRequest("1-1111-2222", "LIB00001", null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not available");
    }
}