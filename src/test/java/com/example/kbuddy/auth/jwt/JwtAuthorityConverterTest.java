package com.example.kbuddy.auth.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthorityConverterTest {

    private final JwtAuthorityConverter converter = new JwtAuthorityConverter();

    @Test
    void type이_SIGNUP이면_TOKEN_SIGNUP_권한을_반환한다() {
        Jwt jwt = jwtWithType("SIGNUP");

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("TOKEN_SIGNUP");
    }

    @Test
    void type이_ACCESS이면_TOKEN_ACCESS_권한을_반환한다() {
        Jwt jwt = jwtWithType("ACCESS");

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("TOKEN_ACCESS");
    }

    @Test
    void type_claim이_없으면_빈_권한을_반환한다() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .subject("1")
                .build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    private Jwt jwtWithType(String type) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .subject("1")
                .claim("type", type)
                .build();
    }
}
