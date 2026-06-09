package com.fallguys.itemservice.controller.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class JwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String ROLE_CLAIM = "user_role";
    private static final Set<String> ALLOWED_ROLES = Set.of(
            "ADMIN",
            "HQ_MANAGER",
            "HQ_STAFF",
            "BRANCH_MANAGER",
            "BRANCH_STAFF"
    );

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        String role = jwt.getClaimAsString(ROLE_CLAIM);
        if (role == null || !ALLOWED_ROLES.contains(role)) {
            return List.of();
        }

        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
