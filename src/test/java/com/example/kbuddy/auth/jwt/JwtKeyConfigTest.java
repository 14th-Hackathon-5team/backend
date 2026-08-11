package com.example.kbuddy.auth.jwt;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtKeyConfigTest {

    @Test
    void PEM_문자열로부터_RSA_PrivateKey와_PublicKey를_파싱한다() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        String privatePem = toPem("PRIVATE KEY", keyPair.getPrivate().getEncoded());
        String publicPem = toPem("PUBLIC KEY", keyPair.getPublic().getEncoded());

        RSAPrivateKey parsedPrivateKey = JwtKeyConfig.parsePrivateKey(privatePem);
        RSAPublicKey parsedPublicKey = JwtKeyConfig.parsePublicKey(publicPem);

        assertThat(parsedPrivateKey).isEqualTo(keyPair.getPrivate());
        assertThat(parsedPublicKey).isEqualTo(keyPair.getPublic());
    }

    @Test
    void issuer가_설정값과_일치하면_검증에_성공한다() throws Exception {
        KeyPair keyPair = generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        JwtProperties issuerProperties = jwtProperties("kbuddy");
        JwtProvider jwtProvider = new JwtProvider(buildEncoder(publicKey, privateKey), issuerProperties);
        JwtDecoder jwtDecoder = new JwtKeyConfig().jwtDecoder(publicKey, issuerProperties);

        String token = jwtProvider.issueAccessToken(1L);

        assertThatCode(() -> jwtDecoder.decode(token)).doesNotThrowAnyException();
    }

    @Test
    void issuer가_설정값과_다르면_검증에_실패한다() throws Exception {
        KeyPair keyPair = generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        JwtProvider jwtProvider = new JwtProvider(buildEncoder(publicKey, privateKey), jwtProperties("other-issuer"));
        JwtDecoder jwtDecoder = new JwtKeyConfig().jwtDecoder(publicKey, jwtProperties("kbuddy"));

        String token = jwtProvider.issueAccessToken(1L);

        assertThatThrownBy(() -> jwtDecoder.decode(token)).isInstanceOf(JwtException.class);
    }

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private JwtEncoder buildEncoder(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        RSAKey rsaKey = new RSAKey.Builder(publicKey).privateKey(privateKey).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(rsaKey)));
    }

    private JwtProperties jwtProperties(String issuer) {
        return new JwtProperties(issuer, null, null, Duration.ofMinutes(10), Duration.ofHours(1));
    }

    private String toPem(String label, byte[] encoded) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(encoded);
        return "-----BEGIN " + label + "-----\n" + base64 + "\n-----END " + label + "-----\n";
    }
}
