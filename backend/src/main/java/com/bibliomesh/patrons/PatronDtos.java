package com.bibliomesh.patrons;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public final class PatronDtos {

    private PatronDtos() {}

    public record UpsertPatronRequest(
            @NotBlank @Size(max = 64) String identificationNumber,
            @NotBlank @Size(max = 120) String firstName,
            @Size(max = 120) String firstSurname,
            @Size(max = 120) String secondSurname,
            PatronType patronType,
            @Size(max = 64) String gradeSection,
            @Size(max = 40) String phone,
            @Email @Size(max = 180) String email,
            String observations
    ) {}

    public record PatronResponse(
            Long id,
            String identificationNumber,
            String firstName,
            String firstSurname,
            String secondSurname,
            String fullName,
            PatronType patronType,
            String gradeSection,
            String phone,
            String email,
            String observations,
            boolean active,
            OffsetDateTime createdAt
    ) {}

    public record PatronSummary(
            Long id,
            String identificationNumber,
            String fullName,
            PatronType patronType,
            boolean active
    ) {}
}