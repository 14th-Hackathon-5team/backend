package com.example.kbuddy.auth.jwt;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class JwtAuthorityConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String AUTHORITY_PREFIX = "TOKEN_";
    private static final String TYPE_CLAIM = "type";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        String type = jwt.getClaimAsString(TYPE_CLAIM);
        if (type == null || type.isBlank()) {
            return Collections.emptyList();
        }
        return List.of(new SimpleGrantedAuthority(AUTHORITY_PREFIX + type));
    }
}
