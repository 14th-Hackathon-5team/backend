package com.example.kbuddy.global.security;

import com.example.kbuddy.auth.jwt.JwtProperties;
import com.example.kbuddy.auth.jwt.JwtProvider;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({SecurityConfig.class, JwtProvider.class, SecurityConfigTest.TestKeyConfig.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    void permitAll_경로는_인증_없이_401이_아니다() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    @Test
    void 보호_경로에_토큰_없이_요청하면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/probe"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 보호_경로에_유효한_ACCESS_TOKEN으로_요청하면_인증을_통과한다() throws Exception {
        String accessToken = jwtProvider.issueAccessToken(1L);

        mockMvc.perform(get("/api/probe").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void 보호_경로에_유효하지_않은_토큰으로_요청하면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/probe").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @TestConfiguration
    static class TestKeyConfig {

        @Bean
        KeyPair testKeyPair() throws Exception {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        }

        @Bean
        JwtEncoder jwtEncoder(KeyPair testKeyPair) {
            RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) testKeyPair.getPublic())
                    .privateKey((RSAPrivateKey) testKeyPair.getPrivate())
                    .build();
            return new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(rsaKey)));
        }

        @Bean
        JwtDecoder jwtDecoder(KeyPair testKeyPair) {
            return NimbusJwtDecoder.withPublicKey((RSAPublicKey) testKeyPair.getPublic()).build();
        }

        @Bean
        JwtProperties jwtProperties() {
            return new JwtProperties("kbuddy-test", null, null, Duration.ofMinutes(10), Duration.ofHours(1));
        }
    }
}
