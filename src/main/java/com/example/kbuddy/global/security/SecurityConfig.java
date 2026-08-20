package com.example.kbuddy.global.security;

import com.example.kbuddy.auth.jwt.JwtAuthorityConverter;
import com.example.kbuddy.auth.service.OAuth2LoginFailureHandler;
import com.example.kbuddy.auth.service.OAuth2LoginSuccessHandler;
import com.example.kbuddy.auth.service.OAuth2UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PERMIT_ALL_PATHS = {
            "/oauth2/**",
            "/login/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtDecoder jwtDecoder,
            OAuth2UserService oAuth2UserService,
            OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
            OAuth2LoginFailureHandler oAuth2LoginFailureHandler,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new JwtAuthorityConverter());

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PERMIT_ALL_PATHS).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users/me").hasAuthority("TOKEN_SIGNUP")
                        .requestMatchers(HttpMethod.GET, "/api/users/me").hasAuthority("TOKEN_ACCESS")
                        .requestMatchers(HttpMethod.PATCH, "/api/users/me").hasAuthority("TOKEN_ACCESS")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/me").hasAuthority("TOKEN_ACCESS")
                        .requestMatchers(HttpMethod.GET, "/api/guides/**").hasAuthority("TOKEN_ACCESS")
                        .requestMatchers(HttpMethod.GET, "/api/settings/me").hasAuthority("TOKEN_ACCESS")
                        .requestMatchers(HttpMethod.PATCH, "/api/settings/me/**").hasAuthority("TOKEN_ACCESS")
                        .requestMatchers(HttpMethod.GET, "/api/calendar/events/**").hasAuthority("TOKEN_ACCESS")
                        .requestMatchers(HttpMethod.GET, "/notifications").hasAuthority("TOKEN_ACCESS")
                        .requestMatchers(HttpMethod.PATCH, "/notifications/*/read").hasAuthority("TOKEN_ACCESS")
                        .requestMatchers(HttpMethod.POST, "/ai/chat").hasAuthority("TOKEN_ACCESS")
                        .requestMatchers(HttpMethod.GET, "/api/external/seoul-jobs").hasAuthority("TOKEN_ACCESS")
                        .requestMatchers(HttpMethod.GET, "/api/news").hasAuthority("TOKEN_ACCESS")
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(oAuth2UserService))
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureHandler(oAuth2LoginFailureHandler))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter)));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origin}") String allowedOrigin
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigin));
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // API 인증은 Authorization 헤더의 Bearer 토큰만 사용하며 쿠키 기반 세션을 쓰지 않으므로 credentials는 비활성 상태로 둔다.
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
