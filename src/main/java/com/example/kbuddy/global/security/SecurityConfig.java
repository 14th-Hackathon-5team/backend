package com.example.kbuddy.global.security;

import com.example.kbuddy.auth.jwt.JwtAuthorityConverter;
import com.example.kbuddy.auth.service.OAuth2LoginSuccessHandler;
import com.example.kbuddy.auth.service.OAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

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
            OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler
    ) throws Exception {

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new JwtAuthorityConverter());

        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PERMIT_ALL_PATHS).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users/me").hasAuthority("TOKEN_SIGNUP")
                        .requestMatchers(HttpMethod.GET, "/api/users/me").hasAuthority("TOKEN_ACCESS")
                        .requestMatchers(HttpMethod.PATCH, "/api/users/me").hasAuthority("TOKEN_ACCESS")
                        .requestMatchers(HttpMethod.GET, "/api/guides/**").hasAuthority("TOKEN_ACCESS")
                        .requestMatchers(HttpMethod.GET, "/api/settings/me").hasAuthority("TOKEN_ACCESS")
                        .requestMatchers(HttpMethod.PATCH, "/api/settings/me/**").hasAuthority("TOKEN_ACCESS")
                        .requestMatchers(HttpMethod.GET, "/api/calendar/events/**").hasAuthority("TOKEN_ACCESS")
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(oAuth2UserService))
                        .successHandler(oAuth2LoginSuccessHandler))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter)));

        return http.build();
    }
}
