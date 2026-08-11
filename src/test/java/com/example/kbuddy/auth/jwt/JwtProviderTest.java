package com.example.kbuddy.auth.jwt;

import com.example.kbuddy.auth.oauth.AuthProvider;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private static JwtProvider jwtProvider;
    private static JwtDecoder jwtDecoder;

    @BeforeAll
    static void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        RSAKey rsaKey = new RSAKey.Builder(publicKey).privateKey(privateKey).build();
        JwtEncoder jwtEncoder = new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(rsaKey)));
        jwtDecoder = NimbusJwtDecoder.withPublicKey(publicKey).build();

        JwtProperties jwtProperties = new JwtProperties(
                "kbuddy-test",
                null,
                null,
                Duration.ofMinutes(10),
                Duration.ofHours(1)
        );

        jwtProvider = new JwtProvider(jwtEncoder, jwtProperties);
    }

    @Test
    void SIGNUP_TOKEN은_provider_providerId_email_name_claim을_포함한다() {
        String token = jwtProvider.issueSignupToken(AuthProvider.KAKAO, "12345", "test@kbuddy.com", "홍길동");

        Jwt jwt = jwtDecoder.decode(token);

        assertThat(jwt.getClaimAsString("type")).isEqualTo("SIGNUP");
        assertThat(jwt.getClaimAsString("provider")).isEqualTo("KAKAO");
        assertThat(jwt.getClaimAsString("providerId")).isEqualTo("12345");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("test@kbuddy.com");
        assertThat(jwt.getClaimAsString("name")).isEqualTo("홍길동");
    }

    @Test
    void SIGNUP_TOKEN의_sub는_provider와_providerId_조합이다() {
        String token = jwtProvider.issueSignupToken(AuthProvider.KAKAO, "12345", "test@kbuddy.com", "홍길동");

        Jwt jwt = jwtDecoder.decode(token);

        assertThat(jwt.getSubject()).isEqualTo("KAKAO:12345");
    }

    @Test
    void ACCESS_TOKEN의_sub는_User_id이다() {
        String token = jwtProvider.issueAccessToken(42L);

        Jwt jwt = jwtDecoder.decode(token);

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("type")).isEqualTo("ACCESS");
    }

    @Test
    void ACCESS_TOKEN에는_프로필_정보_claim이_없다() {
        String token = jwtProvider.issueAccessToken(42L);

        Jwt jwt = jwtDecoder.decode(token);

        assertThat(jwt.getClaims()).doesNotContainKeys("email", "name", "provider", "providerId");
    }

    @Test
    void SIGNUP_TOKEN과_ACCESS_TOKEN의_만료시간이_다르다() {
        String signupToken = jwtProvider.issueSignupToken(AuthProvider.KAKAO, "1", "a@b.com", "a");
        String accessToken = jwtProvider.issueAccessToken(1L);

        Jwt signupJwt = jwtDecoder.decode(signupToken);
        Jwt accessJwt = jwtDecoder.decode(accessToken);

        Duration signupTtl = Duration.between(signupJwt.getIssuedAt(), signupJwt.getExpiresAt());
        Duration accessTtl = Duration.between(accessJwt.getIssuedAt(), accessJwt.getExpiresAt());

        assertThat(signupTtl).isEqualTo(Duration.ofMinutes(10));
        assertThat(accessTtl).isEqualTo(Duration.ofHours(1));
    }
}
