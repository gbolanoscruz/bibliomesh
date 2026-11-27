package com.bibliomesh.patrons.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bibliomesh.patrons.PatronDtos.PatronResponse;
import com.bibliomesh.patrons.PatronDtos.UpsertPatronRequest;
import com.bibliomesh.patrons.PatronType;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PatronServiceTest {

    private PatronRepository repository;
    private PatronService service;

    @BeforeEach
    void setUp() {
        repository = mock(PatronRepository.class);
        service = new PatronService(repository);
    }

    @Test
    void buildsFullNameWithBothSurnamesOnUpsert() {
        when(repository.findByIdentificationNumberIgnoreCase("1-1234-5678")).thenReturn(Optional.empty());
        when(repository.save(any(Patron.class))).thenAnswer(inv -> inv.getArgument(0));

        PatronResponse res = service.upsert(new UpsertPatronRequest(
                " 1-1234-5678 ", "María", "Rojas", "Castro",
                null, "5-B", "8888-9999", "maria@escuela.cr", "Beca lectura"));

        assertThat(res.identificationNumber()).isEqualTo("1-1234-5678");
        assertThat(res.fullName()).isEqualTo("María Rojas Castro");
        assertThat(res.patronType()).isEqualTo(PatronType.STUDENT);
        assertThat(res.active()).isTrue();
    }
}