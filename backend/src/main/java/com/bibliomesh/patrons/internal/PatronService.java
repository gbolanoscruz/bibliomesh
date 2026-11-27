package com.bibliomesh.patrons.internal;

import com.bibliomesh.patrons.PatronDtos.PatronResponse;
import com.bibliomesh.patrons.PatronDtos.PatronSummary;
import com.bibliomesh.patrons.PatronDtos.UpsertPatronRequest;
import com.bibliomesh.patrons.PatronType;
import com.bibliomesh.patrons.PatronsApi;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PatronService implements PatronsApi {

    private final PatronRepository patronRepository;

    public PatronService(PatronRepository patronRepository) {
        this.patronRepository = patronRepository;
    }

    @Transactional
    public PatronResponse upsert(UpsertPatronRequest req) {
        String idNumber = req.identificationNumber().trim();
        Patron patron = patronRepository.findByIdentificationNumberIgnoreCase(idNumber)
                .orElseGet(Patron::new);

        patron.setIdentificationNumber(idNumber);
        patron.setFirstName(req.firstName().trim());
        patron.setFirstSurname(trimOrNull(req.firstSurname()));
        patron.setSecondSurname(trimOrNull(req.secondSurname()));
        patron.setPatronType(req.patronType() != null ? req.patronType() : PatronType.STUDENT);
        patron.setGradeSection(trimOrNull(req.gradeSection()));
        patron.setPhone(trimOrNull(req.phone()));
        patron.setEmail(trimOrNull(req.email()));
        patron.setObservations(trimOrNull(req.observations()));
        patron.setActive(true);
        patron.setUpdatedAt(OffsetDateTime.now());

        return toResponse(patronRepository.save(patron));
    }

    @Transactional(readOnly = true)
    public List<PatronResponse> search(String query) {
        List<Patron> patrons = (query == null || query.isBlank())
                ? patronRepository.findByActiveTrueOrderByFirstNameAscFirstSurnameAsc()
                : patronRepository.searchActive(query.trim());
        return patrons.stream().map(p -> toResponse(p)).toList();
    }

    @Transactional(readOnly = true)
    public PatronResponse getByIdentification(String identificationNumber) {
        Patron patron = patronRepository.findByIdentificationNumberIgnoreCase(identificationNumber.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Patron not found: " + identificationNumber));
        return toResponse(patron);
    }

    @Transactional
    public void deactivate(String identificationNumber) {
        Patron patron = patronRepository.findByIdentificationNumberIgnoreCase(identificationNumber.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Patron not found: " + identificationNumber));
        patron.setActive(false);
        patron.setUpdatedAt(OffsetDateTime.now());
        patronRepository.save(patron);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PatronSummary> findSummaryByIdentification(String identificationNumber) {
        if (identificationNumber == null || identificationNumber.isBlank()) {
            return Optional.empty();
        }
        return patronRepository.findByIdentificationNumberIgnoreCase(identificationNumber.trim())
                .map(p -> toSummary(p));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PatronSummary> findSummaryById(Long id) {
        return patronRepository.findById(id).map(p -> toSummary(p));
    }

    @Override
    @Transactional(readOnly = true)
    public long countActivePatrons() {
        return patronRepository.countByActiveTrue();
    }

    private static PatronResponse toResponse(Patron p) {
        return new PatronResponse(
                p.getId(), p.getIdentificationNumber(), p.getFirstName(),
                p.getFirstSurname(), p.getSecondSurname(), p.getFullName(),
                p.getPatronType(), p.getGradeSection(), p.getPhone(), p.getEmail(),
                p.getObservations(), p.isActive(), p.getCreatedAt());
    }

    private static PatronSummary toSummary(Patron p) {
        return new PatronSummary(
                p.getId(), p.getIdentificationNumber(), p.getFullName(),
                p.getPatronType(), p.isActive());
    }

    private static String trimOrNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}