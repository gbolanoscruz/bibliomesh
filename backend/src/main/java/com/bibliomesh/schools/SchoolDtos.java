package com.bibliomesh.schools;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public final class SchoolDtos {

    private SchoolDtos() {}

    public record CreateSchoolRequest(
        @NotBlank @Size(max = 24) String code,
        @NotBlank @Size(max = 150) String name,
        @Min(1) @Max(12) Integer seqPadding
    ) {}

    public record UpdateSchoolSettingsRequest(
        @NotBlank @Size(max = 24) String code,
        @NotBlank @Size(max = 150) String name,
        @Min(1) @Max(12) Integer seqPadding
    ) {}

    public record SchoolResponse(
        Long id,
        String code,
        String name,
        Integer seqPadding,
        Long lastInscriptionSeq,
        Boolean active,
        OffsetDateTime updatedAt
    ) {
        public static SchoolResponse from(School school) {
            return new SchoolResponse(
                school.getId(),
                school.getCode(),
                school.getName(),
                school.getSeqPadding(),
                school.getLastInscriptionSeq(),
                school.getActive(),
                school.getUpdatedAt()
            );
        }
    }

    public record InscriptionCodeResponse(
        Long schoolId,
        String schoolCode,
        Long sequenceNumber,
        String formattedInscriptionNumber
    ) {}
}