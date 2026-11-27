package com.bibliomesh.circulation;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public final class CirculationDtos {

    private CirculationDtos() {}

    public record CheckoutRequest(
            @NotBlank String patronIdentification,
            @NotBlank String itemCode,
            LocalDate dueDate,
            String notes
    ) {}

    public record ReturnRequest(
            @NotBlank String itemCode
    ) {}

    public record RoomConsultationRequest(
            @NotBlank String inscriptionNumber,
            String patronIdentification
    ) {}

    public record LoanResponse(
            Long id,
            String loanCode,
            String itemType,
            String itemCode,
            String itemTitle,
            String patronIdentification,
            String patronName,
            OffsetDateTime loanedAt,
            LocalDate dueDate,
            OffsetDateTime returnedAt,
            LoanStatus status,
            String notes
    ) {}

    public record RoomConsultationResponse(
            Long id,
            String movementCode,
            String inscriptionNumber,
            String bookTitle,
            OffsetDateTime consultedAt
    ) {}

    public record DashboardStatsResponse(
            long totalBookCopies,
            long totalTechResources,
            long activePatrons,
            long activeLoans,
            long returnedLoans,
            long roomConsultations
    ) {}
}