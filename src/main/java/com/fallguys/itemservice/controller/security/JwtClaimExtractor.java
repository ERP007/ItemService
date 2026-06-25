package com.fallguys.itemservice.controller.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

public final class JwtClaimExtractor {

    private static final String EMPLOYEE_NO_CLAIM = "employee_no";

    private JwtClaimExtractor() {
    }

    public static String extractEmployeeNo(Jwt jwt) {
        if (jwt == null) {
            throw new AccessDeniedException("JWT가 필요합니다.");
        }
        String employeeNo = jwt.getClaimAsString(EMPLOYEE_NO_CLAIM);
        if (employeeNo == null || employeeNo.isBlank()) {
            throw new AccessDeniedException("사번 claim이 필요합니다.");
        }
        return employeeNo.trim();
    }
}
