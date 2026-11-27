package com.bibliomesh.circulation.internal;

import com.bibliomesh.catalog.CatalogApi;
import com.bibliomesh.catalog.CatalogApi.CopySummary;
import com.bibliomesh.catalog.CatalogApi.TechResourceSummary;
import com.bibliomesh.catalog.CopyStatus;
import com.bibliomesh.catalog.TechResourceStatus;
import com.bibliomesh.circulation.CirculationDtos.CheckoutRequest;
import com.bibliomesh.circulation.CirculationDtos.DashboardStatsResponse;
import com.bibliomesh.circulation.CirculationDtos.LoanResponse;
import com.bibliomesh.circulation.CirculationDtos.RoomConsultationRequest;
import com.bibliomesh.circulation.CirculationDtos.RoomConsultationResponse;
import com.bibliomesh.circulation.LoanStatus;
import com.bibliomesh.patrons.PatronDtos.PatronSummary;
import com.bibliomesh.patrons.PatronsApi;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CirculationService {

    private static final Set<LoanStatus> OPEN_STATUSES = Set.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE);

    private final LoanRepository loanRepository;
    private final RoomConsultationRepository roomConsultationRepository;
    private final CatalogApi catalogApi;
    private final PatronsApi patronsApi;

    public CirculationService(
            LoanRepository loanRepository,
            RoomConsultationRepository roomConsultationRepository,
            CatalogApi catalogApi,
            PatronsApi patronsApi) {
        this.loanRepository = loanRepository;
        this.roomConsultationRepository = roomConsultationRepository;
        this.catalogApi = catalogApi;
        this.patronsApi = patronsApi;
    }

    @Transactional
    public LoanResponse checkout(CheckoutRequest req) {
        PatronSummary patron = patronsApi.findSummaryByIdentification(req.patronIdentification())
                .filter(p -> p.active())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Active patron not found: " + req.patronIdentification()));

        String code = req.itemCode().trim();
        Optional<CopySummary> copyOpt = catalogApi.findCopyByInscription(code);
        if (copyOpt.isPresent()) {
            CopySummary copy = copyOpt.get();
            if (copy.status() != CopyStatus.AVAILABLE) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Book copy is not available for loan: " + copy.inscriptionNumber());
            }

            Loan loan = new Loan();
            loan.setLoanCode("P-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            loan.setPatronId(patron.id());
            loan.setCopyId(copy.copyId());
            loan.setDueDate(req.dueDate());
            loan.setNotes(trimOrNull(req.notes()));
            loan.setStatus(LoanStatus.ACTIVE);

            catalogApi.updateCopyStatus(copy.copyId(), CopyStatus.LOANED);
            Loan saved = loanRepository.save(loan);
            return new LoanResponse(
                    saved.getId(), saved.getLoanCode(), "BOOK", copy.inscriptionNumber(), copy.title(),
                    patron.identificationNumber(), patron.fullName(), saved.getLoanedAt(),
                    saved.getDueDate(), saved.getReturnedAt(), saved.getStatus(), saved.getNotes());
        }

        Optional<TechResourceSummary> techOpt = catalogApi.findTechResourceByAssetTag(code);
        if (techOpt.isPresent()) {
            TechResourceSummary tech = techOpt.get();
            if (tech.status() != TechResourceStatus.AVAILABLE) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Tech resource is not available for loan: " + tech.assetTag());
            }

            Loan loan = new Loan();
            loan.setLoanCode("P-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            loan.setPatronId(patron.id());
            loan.setTechResourceId(tech.id());
            loan.setDueDate(req.dueDate());
            loan.setNotes(trimOrNull(req.notes()));
            loan.setStatus(LoanStatus.ACTIVE);

            catalogApi.updateTechResourceStatus(tech.id(), TechResourceStatus.LOANED);
            Loan saved = loanRepository.save(loan);
            return new LoanResponse(
                    saved.getId(), saved.getLoanCode(), "TECH", tech.assetTag(), tech.name(),
                    patron.identificationNumber(), patron.fullName(), saved.getLoanedAt(),
                    saved.getDueDate(), saved.getReturnedAt(), saved.getStatus(), saved.getNotes());
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item code not found in catalog: " + code);
    }

    @Transactional
    public LoanResponse returnItem(String rawItemCode) {
        String code = rawItemCode.trim();

        Optional<CopySummary> copyOpt = catalogApi.findCopyByInscription(code);
        if (copyOpt.isPresent()) {
            CopySummary copy = copyOpt.get();
            Loan loan = loanRepository.findFirstByCopyIdAndStatusIn(copy.copyId(), OPEN_STATUSES)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "No active loan found for book copy: " + copy.inscriptionNumber()));

            loan.setStatus(LoanStatus.RETURNED);
            loan.setReturnedAt(OffsetDateTime.now());
            catalogApi.updateCopyStatus(copy.copyId(), CopyStatus.AVAILABLE);
            return toLoanResponse(loanRepository.save(loan));
        }

        Optional<TechResourceSummary> techOpt = catalogApi.findTechResourceByAssetTag(code);
        if (techOpt.isPresent()) {
            TechResourceSummary tech = techOpt.get();
            Loan loan = loanRepository.findFirstByTechResourceIdAndStatusIn(tech.id(), OPEN_STATUSES)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "No active loan found for tech resource: " + tech.assetTag()));

            loan.setStatus(LoanStatus.RETURNED);
            loan.setReturnedAt(OffsetDateTime.now());
            catalogApi.updateTechResourceStatus(tech.id(), TechResourceStatus.AVAILABLE);
            return toLoanResponse(loanRepository.save(loan));
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item code not found in catalog: " + code);
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> listActiveLoans() {
        return loanRepository.findByStatusInOrderByLoanedAtDesc(OPEN_STATUSES).stream()
                .map(l -> toLoanResponse(l))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> listRecentReturnedLoans() {
        return loanRepository.findTop50ByStatusOrderByReturnedAtDesc(LoanStatus.RETURNED).stream()
                .map(l -> toLoanResponse(l))
                .toList();
    }

    @Transactional
    public RoomConsultationResponse registerRoomConsultation(RoomConsultationRequest req) {
        CopySummary copy = catalogApi.findCopyByInscription(req.inscriptionNumber())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Book copy not found: " + req.inscriptionNumber()));

        Long patronId = null;
        if (req.patronIdentification() != null && !req.patronIdentification().isBlank()) {
            patronId = patronsApi.findSummaryByIdentification(req.patronIdentification())
                    .map(p -> p.id())
                    .orElse(null);
        }

        RoomConsultation consultation = new RoomConsultation();
        consultation.setMovementCode("INT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        consultation.setCopyId(copy.copyId());
        consultation.setPatronId(patronId);

        RoomConsultation saved = roomConsultationRepository.save(consultation);
        return new RoomConsultationResponse(
                saved.getId(), saved.getMovementCode(), copy.inscriptionNumber(),
                copy.title(), saved.getConsultedAt());
    }

    @Transactional(readOnly = true)
    public List<RoomConsultationResponse> listRecentRoomConsultations() {
        return roomConsultationRepository.findTop20ByOrderByConsultedAtDesc().stream()
                .map(rc -> {
                    Optional<CopySummary> copy = catalogApi.findCopyById(rc.getCopyId());
                    return new RoomConsultationResponse(
                            rc.getId(),
                            rc.getMovementCode(),
                            copy.map(c -> c.inscriptionNumber()).orElse("UNKNOWN"),
                            copy.map(c -> c.title()).orElse("Unknown Title"),
                            rc.getConsultedAt());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        return new DashboardStatsResponse(
                catalogApi.countBookCopies(),
                catalogApi.countTechResources(),
                patronsApi.countActivePatrons(),
                loanRepository.countByStatusIn(OPEN_STATUSES),
                loanRepository.countByStatus(LoanStatus.RETURNED),
                roomConsultationRepository.count());
    }

    private LoanResponse toLoanResponse(Loan loan) {
        Optional<PatronSummary> patron = patronsApi.findSummaryById(loan.getPatronId());
        String patronIdNum = patron.map(p -> p.identificationNumber()).orElse("UNKNOWN");
        String patronName = patron.map(p -> p.fullName()).orElse("Unknown Patron");

        if (loan.getCopyId() != null) {
            Optional<CopySummary> copy = catalogApi.findCopyById(loan.getCopyId());
            return new LoanResponse(
                    loan.getId(), loan.getLoanCode(), "BOOK",
                    copy.map(c -> c.inscriptionNumber()).orElse("UNKNOWN"),
                    copy.map(c -> c.title()).orElse("Unknown Title"),
                    patronIdNum, patronName, loan.getLoanedAt(), loan.getDueDate(),
                    loan.getReturnedAt(), loan.getStatus(), loan.getNotes());
        }

        Optional<TechResourceSummary> tech = catalogApi.findTechResourceById(loan.getTechResourceId());
        return new LoanResponse(
                loan.getId(), loan.getLoanCode(), "TECH",
                tech.map(t -> t.assetTag()).orElse("UNKNOWN"),
                tech.map(t -> t.name()).orElse("Unknown Resource"),
                patronIdNum, patronName, loan.getLoanedAt(), loan.getDueDate(),
                loan.getReturnedAt(), loan.getStatus(), loan.getNotes());
    }

    private static String trimOrNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}