package com.example.kbuddy.global.security;

import com.example.kbuddy.auth.jwt.JwtProperties;
import com.example.kbuddy.auth.jwt.JwtProvider;
import com.example.kbuddy.auth.oauth.AuthProvider;
import com.example.kbuddy.auth.service.OAuth2LoginSuccessHandler;
import com.example.kbuddy.auth.service.OAuth2UserService;
import com.example.kbuddy.guide.service.GuideService;
import com.example.kbuddy.calendar.service.CalendarEventService;
import com.example.kbuddy.user.repository.UserRepository;
import com.example.kbuddy.user.service.UserService;
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
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
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
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({
        SecurityConfig.class,
        JwtProvider.class,
        OAuth2UserService.class,
        OAuth2LoginSuccessHandler.class,
        SecurityConfigTest.TestKeyConfig.class
})
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

    @Test
    void oauth2_인가_요청_경로는_인증_없이_401이_아니다() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    @Test
    void ACCESS_TOKEN으로_POST_api_users_me에_접근하면_403을_반환한다() throws Exception {
        String accessToken = jwtProvider.issueAccessToken(1L);

        mockMvc.perform(post("/api/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void ACCESS_TOKEN으로_GET_api_users_me에_접근하면_403이_아니다() throws Exception {
        String accessToken = jwtProvider.issueAccessToken(1L);

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }

    @Test
    void SIGNUP_TOKEN으로_GET_api_users_me에_접근하면_403을_반환한다() throws Exception {
        String signupToken = jwtProvider.issueSignupToken(
                AuthProvider.GOOGLE, "109876543210987654321", "test@gmail.com", "홍길동");

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + signupToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void ACCESS_TOKEN으로_PATCH_api_users_me에_접근하면_403이_아니다() throws Exception {
        String accessToken = jwtProvider.issueAccessToken(1L);

        mockMvc.perform(patch("/api/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }

    @Test
    void SIGNUP_TOKEN으로_PATCH_api_users_me에_접근하면_403을_반환한다() throws Exception {
        String signupToken = jwtProvider.issueSignupToken(
                AuthProvider.GOOGLE, "109876543210987654321", "test@gmail.com", "홍길동");

        mockMvc.perform(patch("/api/users/me").header("Authorization", "Bearer " + signupToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void ACCESS_TOKEN으로_DELETE_api_users_me에_접근하면_403이_아니다() throws Exception {
        String accessToken = jwtProvider.issueAccessToken(1L);

        mockMvc.perform(delete("/api/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }

    @Test
    void SIGNUP_TOKEN으로_DELETE_api_users_me에_접근하면_403을_반환한다() throws Exception {
        String signupToken = jwtProvider.issueSignupToken(
                AuthProvider.GOOGLE, "109876543210987654321", "test@gmail.com", "홍길동");

        mockMvc.perform(delete("/api/users/me").header("Authorization", "Bearer " + signupToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void DELETE_api_users_me에_토큰_없이_요청하면_401을_반환한다() throws Exception {
        mockMvc.perform(delete("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    private static final String ALLOWED_ORIGIN = "https://frontend-chi-pied-78.vercel.app";

    @Test
    void 허용된_Origin의_preflight_요청은_인증_없이_허용된다() throws Exception {
        mockMvc.perform(options("/api/users/me")
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "DELETE")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN));
    }

    @Test
    void 허용된_Origin으로_인증된_요청을_보내면_Access_Control_Allow_Origin_헤더가_반환된다() throws Exception {
        String accessToken = jwtProvider.issueAccessToken(1L);

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Origin", ALLOWED_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN));
    }

    @Test
    void 허용되지_않은_Origin의_preflight_요청은_거부된다() throws Exception {
        mockMvc.perform(options("/api/users/me")
                        .header("Origin", "https://evil.example.com")
                        .header("Access-Control-Request-Method", "DELETE"))
                .andExpect(status().isForbidden());
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

        @Bean
        UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        UserService userService() {
            return mock(UserService.class);
        }

        @Bean
        GuideService guideService() {
            return mock(GuideService.class);
        }

        @Bean
        CalendarEventService calendarEventService() {
            return mock(CalendarEventService.class);
        }

        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            return new InMemoryClientRegistrationRepository(googleClientRegistration(), kakaoClientRegistration());
        }

        private ClientRegistration googleClientRegistration() {
            return ClientRegistration.withRegistrationId("google")
                    .clientId("test-google-client-id")
                    .clientSecret("test-google-client-secret")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .scope("openid", "profile", "email")
                    .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                    .tokenUri("https://www.googleapis.com/oauth2/v4/token")
                    .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                    .userNameAttributeName("sub")
                    .clientName("Google")
                    .build();
        }

        private ClientRegistration kakaoClientRegistration() {
            return ClientRegistration.withRegistrationId("kakao")
                    .clientId("test-kakao-client-id")
                    .clientSecret("test-kakao-client-secret")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .scope("profile_nickname", "account_email")
                    .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                    .tokenUri("https://kauth.kakao.com/oauth/token")
                    .userInfoUri("https://kapi.kakao.com/v2/user/me")
                    .userNameAttributeName("id")
                    .clientName("Kakao")
                    .build();
        }
    }
}
