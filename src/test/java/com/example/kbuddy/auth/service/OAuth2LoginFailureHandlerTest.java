package com.example.kbuddy.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginFailureHandlerTest {

    private static final String FRONTEND_REDIRECT_URI = "http://localhost:3000/oauth/callback";
    private static final String EXPECTED_FAILURE_URL = "http://localhost:3000/login?error=true";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private OAuth2LoginFailureHandler failureHandler;

    @BeforeEach
    void setUp() {
        failureHandler = new OAuth2LoginFailureHandler(FRONTEND_REDIRECT_URI);
    }

    @Test
    void onAuthenticationFailure_redirectsToFrontendLoginErrorPath() throws Exception {
        when(request.getRequestURI()).thenReturn("/login/oauth2/code/google");
        OAuth2AuthenticationException exception = new OAuth2AuthenticationException(
                new OAuth2Error("access_denied", "state validation failed", null)
        );

        failureHandler.onAuthenticationFailure(request, response, exception);

        verify(response).sendRedirect(EXPECTED_FAILURE_URL);
    }

    @Test
    void onAuthenticationFailure_doesNotExposeExceptionDetailsInRedirectUrl() throws Exception {
        when(request.getRequestURI()).thenReturn("/login/oauth2/code/kakao");
        String sensitiveDetail = "authorization_code=SENSITIVE_CODE&state=SENSITIVE_STATE";
        OAuth2AuthenticationException exception = new OAuth2AuthenticationException(
                new OAuth2Error("invalid_grant", sensitiveDetail, null)
        );

        failureHandler.onAuthenticationFailure(request, response, exception);

        verify(response, never()).sendRedirect(contains("SENSITIVE"));
        verify(response, never()).sendRedirect(contains("invalid_grant"));
        verify(response).sendRedirect(EXPECTED_FAILURE_URL);
    }

    @Test
    void onAuthenticationFailure_reusesOriginFromFrontendRedirectUri() throws Exception {
        OAuth2LoginFailureHandler prodFailureHandler =
                new OAuth2LoginFailureHandler("https://frontend-chi-pied-78.vercel.app/oauth/callback");
        when(request.getRequestURI()).thenReturn("/login/oauth2/code/google");

        prodFailureHandler.onAuthenticationFailure(
                request, response, new OAuth2AuthenticationException("provider_error"));

        verify(response).sendRedirect("https://frontend-chi-pied-78.vercel.app/login?error=true");
    }
}
