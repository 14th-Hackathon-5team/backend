package com.example.kbuddy.auth.jwt;

import com.example.kbuddy.auth.oauth.AuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private static final String TYPE_CLAIM = "type";
    private static final String PROVIDER_CLAIM = "provider";
    private static final String PROVIDER_ID_CLAIM = "providerId";
    private static final String EMAIL_CLAIM = "email";
    private static final String NAME_CLAIM = "name";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public String issueSignupToken(AuthProvider provider, String providerId, String email, String name) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .issuedAt(now)
                .expiresAt(now.plus(jwtProperties.signupTokenExpiration()))
                .subject(provider.name() + ":" + providerId)
                .claim(TYPE_CLAIM, TokenType.SIGNUP.name())
                .claim(PROVIDER_CLAIM, provider.name())
                .claim(PROVIDER_ID_CLAIM, providerId)
                .claim(EMAIL_CLAIM, email)
                .claim(NAME_CLAIM, name)
                .build();
        return encode(claims);
    }

    public String issueAccessToken(Long userId) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .issuedAt(now)
                .expiresAt(now.plus(jwtProperties.accessTokenExpiration()))
                .subject(String.valueOf(userId))
                .claim(TYPE_CLAIM, TokenType.ACCESS.name())
                .build();
        return encode(claims);
    }

    private String encode(JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
