package com.bibliomesh.auth;

public record AuthPrincipal(
    String email,
    String role,
    Long schoolId
) {
    public boolean isSuperAdmin() {
        return "SUPER_ADMIN".equals(role);
    }

    public boolean canManageSchool(Long targetSchoolId) {
        if (isSuperAdmin()) return true;
        return targetSchoolId != null && targetSchoolId.equals(schoolId);
    }
}