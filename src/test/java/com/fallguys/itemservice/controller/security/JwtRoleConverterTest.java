package com.fallguys.itemservice.controller.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

class JwtRoleConverterTest {

    private final JwtRoleConverter converter = new JwtRoleConverter();

    @Test
    void convertsUserRoleClaimToRoleAuthority() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "tester")
                .claim("user_role", "HQ_STAFF")
                .build();

        assertThat(converter.convert(jwt))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_HQ_STAFF");
    }

    @Test
    void returnsNoAuthoritiesWhenUserRoleClaimIsMissingOrUnknown() {
        Jwt missingRoleJwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "tester")
                .build();
        Jwt unknownRoleJwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "tester")
                .claim("user_role", "UNKNOWN")
                .build();

        assertThat(converter.convert(missingRoleJwt)).isEmpty();
        assertThat(converter.convert(unknownRoleJwt)).isEmpty();
    }
}
