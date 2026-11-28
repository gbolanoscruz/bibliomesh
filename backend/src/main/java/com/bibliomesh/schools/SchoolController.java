package com.bibliomesh.schools;

import com.bibliomesh.schools.SchoolDtos.CreateSchoolRequest;
import com.bibliomesh.schools.SchoolDtos.InscriptionCodeResponse;
import com.bibliomesh.schools.SchoolDtos.SchoolResponse;
import com.bibliomesh.schools.SchoolDtos.UpdateSchoolSettingsRequest;
import com.bibliomesh.schools.internal.SchoolService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class SchoolController {

    private final SchoolService schoolService;

    public SchoolController(SchoolService schoolService) {
        this.schoolService = schoolService;
    }

    // Lectura pública sin login para que los lectores elijan su escuela
    @GetMapping("/public/schools")
    public List<SchoolResponse> listPublicSchools() {
        return schoolService.listActiveSchools();
    }

    @GetMapping("/public/schools/{code}")
    public SchoolResponse getPublicSchool(@PathVariable String code) {
        return schoolService.getByCode(code);
    }

    // Super Admin crea escuelas (valida rol/alcance cuando conectemos el JWT)
    @PostMapping("/admin/schools")
    @ResponseStatus(HttpStatus.CREATED)
    public SchoolResponse createSchool(
        @RequestHeader(value = "X-User-Role", required = false) String role,
        @Valid @RequestBody CreateSchoolRequest request
    ) {
        requireSuperAdmin(role);
        return schoolService.createSchool(request);
    }

    // Admin de esa escuela (o Super Admin) actualiza código / nombre / padding
    @PutMapping("/admin/schools/{schoolId}/settings")
    public SchoolResponse updateSchoolSettings(
        @PathVariable Long schoolId,
        @RequestHeader(value = "X-User-Role", required = false) String role,
        @RequestHeader(value = "X-User-School-Id", required = false) Long userSchoolId,
        @Valid @RequestBody UpdateSchoolSettingsRequest request
    ) {
        requireSchoolScope(role, userSchoolId, schoolId);
        return schoolService.updateSettings(schoolId, request);
    }

    // Generación atómica del siguiente número de inscripción para un libro de la escuela
    @PostMapping("/admin/schools/{schoolId}/inscriptions/next")
    public InscriptionCodeResponse nextInscription(
        @PathVariable Long schoolId,
        @RequestHeader(value = "X-User-Role", required = false) String role,
        @RequestHeader(value = "X-User-School-Id", required = false) Long userSchoolId
    ) {
        requireSchoolScope(role, userSchoolId, schoolId);
        return schoolService.generateNextInscription(schoolId);
    }

    private void requireSuperAdmin(String role) {
        if (!"SUPER_ADMIN".equalsIgnoreCase(role)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Solo SUPER_ADMIN puede crear escuelas"
            );
        }
    }

    private void requireSchoolScope(String role, Long userSchoolId, Long targetSchoolId) {
        if ("SUPER_ADMIN".equalsIgnoreCase(role)) {
            return;
        }
        boolean isSchoolStaff = "SCHOOL_ADMIN".equalsIgnoreCase(role)
            || "LIBRARIAN".equalsIgnoreCase(role);
        if (!isSchoolStaff || userSchoolId == null || !userSchoolId.equals(targetSchoolId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "No tienes permiso para modificar esta escuela"
            );
        }
    }
}