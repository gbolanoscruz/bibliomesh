package com.bibliomesh.patrons;

import com.bibliomesh.patrons.PatronDtos.PatronSummary;
import java.util.Optional;

public interface PatronsApi {

    Optional<PatronSummary> findSummaryByIdentification(String identificationNumber);

    Optional<PatronSummary> findSummaryById(Long id);

    long countActivePatrons();
}