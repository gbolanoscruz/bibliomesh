package com.bibliomesh.schools.internal;

import com.bibliomesh.schools.School;
import com.bibliomesh.schools.SchoolDtos.CreateSchoolRequest;
import com.bibliomesh.schools.SchoolDtos.InscriptionCodeResponse;
import com.bibliomesh.schools.SchoolDtos.SchoolResponse;
import com.bibliomesh.schools.SchoolDtos.UpdateSchoolSettingsRequest;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SchoolService {

    private final SchoolRepository schoolRepository;

    public SchoolService(SchoolRepository schoolRepository) {
        this.schoolRepository = schoolRepository;
    }

    @Transactional(readOnly = true)
    public List<SchoolResponse> listActiveSchools() {
        return schoolRepository.findAll().stream()
            .filter(s -> Boolean.TRUE.equals(s.getActive()))
            .map(SchoolResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public SchoolResponse getByCode(String code) {
        School school = schoolRepository.findByCodeIgnoreCaseAndActiveTrue(code.trim())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Escuela no encontrada o inactiva: " + code
            ));
        return SchoolResponse.from(school);
    }

    @Transactional
    public SchoolResponse createSchool(CreateSchoolRequest req) {
        String normalizedCode = req.code().trim().toUpperCase();
        if (schoolRepository.findByCodeIgnoreCaseAndActiveTrue(normalizedCode).isPresent()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Ya existe una escuela activa con el código: " + normalizedCode
            );
        }

        School school = new School();
        school.setCode(normalizedCode);
        school.setName(req.name().trim());
        school.setSeqPadding(req.seqPadding() != null ? req.seqPadding() : 6);
        school.setLastInscriptionSeq(0L);
        school.setActive(true);
        school.setCreatedAt(OffsetDateTime.now());
        school.setUpdatedAt(OffsetDateTime.now());

        return SchoolResponse.from(schoolRepository.save(school));
    }

    @Transactional
    public SchoolResponse updateSettings(Long schoolId, UpdateSchoolSettingsRequest req) {
        School school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Escuela no encontrada: " + schoolId
            ));

        String normalizedCode = req.code().trim().toUpperCase();
        school.setCode(normalizedCode);
        school.setName(req.name().trim());
        if (req.seqPadding() != null) {
            school.setSeqPadding(req.seqPadding());
        }
        school.setUpdatedAt(OffsetDateTime.now());

        return SchoolResponse.from(schoolRepository.save(school));
    }

    @Transactional
    public InscriptionCodeResponse generateNextInscription(Long schoolId) {
        List<Object[]> rows = schoolRepository.claimNextSequence(schoolId);
        if (rows == null || rows.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Escuela no encontrada para generar inscripción: " + schoolId
            );
        }

        Object[] row = rows.getFirst();
        String code = ((String) row[0]).trim().toUpperCase();
        long seq = ((Number) row[1]).longValue();
        int padding = ((Number) row[2]).intValue();
        String formatted = code + "-" + String.format("%0" + padding + "d", seq);

        return new InscriptionCodeResponse(schoolId, code, seq, formatted);
    }
}