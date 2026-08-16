package com.example.kbuddy.auth.service;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);
    private static final String FAILURE_PATH = "/login?error=true";

    private final String frontendRedirectUri;

    public OAuth2LoginFailureHandler(
            @Value("${app.oauth2.redirect-uri}") String frontendRedirectUri
    ) {
        this.frontendRedirectUri = frontendRedirectUri;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        log.warn("OAuth2 login failed. provider={}, exceptionType={}",
                extractProvider(request), exception.getClass().getSimpleName());
        response.sendRedirect(buildFailureRedirectUrl());
    }

    private String buildFailureRedirectUrl() {
        URI redirectUri = URI.create(frontendRedirectUri);
        String origin = redirectUri.getScheme() + "://" + redirectUri.getAuthority();
        return origin + FAILURE_PATH;
    }

    private String extractProvider(HttpServletRequest request) {
        String path = request.getRequestURI();
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }
}
