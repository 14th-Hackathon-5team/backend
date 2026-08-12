package com.example.kbuddy.auth.service;

import com.example.kbuddy.auth.jwt.JwtProvider;
import com.example.kbuddy.auth.oauth.CustomOAuth2User;
import com.example.kbuddy.auth.oauth.OAuth2LoginStatus;
import com.example.kbuddy.auth.oauth.OAuth2UserInfo;
import com.example.kbuddy.user.entity.User;
import com.example.kbuddy.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final String STATUS_PARAM = "status";
    private static final String ACCESS_TOKEN_PARAM = "accessToken";
    private static final String SIGNUP_TOKEN_PARAM = "signupToken";

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final String frontendRedirectUri;

    public OAuth2LoginSuccessHandler(
            JwtProvider jwtProvider,
            UserRepository userRepository,
            @Value("${app.oauth2.redirect-uri}") String frontendRedirectUri
    ) {
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
        this.frontendRedirectUri = frontendRedirectUri;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        CustomOAuth2User customOAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        response.sendRedirect(resolveTargetUrl(customOAuth2User));
    }

    private String resolveTargetUrl(CustomOAuth2User customOAuth2User) {
        OAuth2UserInfo oAuth2UserInfo = customOAuth2User.getOAuth2UserInfo();
        return switch (customOAuth2User.getLoginStatus()) {
            case EXISTING_USER -> existingUserRedirectUrl(oAuth2UserInfo);
            case NEW_USER -> newUserRedirectUrl(oAuth2UserInfo);
            case EMAIL_DUPLICATED -> buildFragmentUrl(OAuth2LoginStatus.EMAIL_DUPLICATED, null, null);
        };
    }

    private String existingUserRedirectUrl(OAuth2UserInfo oAuth2UserInfo) {
        User user = userRepository
                .findByProviderAndProviderId(oAuth2UserInfo.getProvider(), oAuth2UserInfo.getProviderId())
                .orElseThrow(() -> new OAuth2AuthenticationException(
                        new OAuth2Error(
                                "existing_user_not_found",
                                "provider와 providerId로 조회되는 User가 존재하지 않습니다.",
                                null
                        )
                ));
        String accessToken = jwtProvider.issueAccessToken(user.getId());
        return buildFragmentUrl(OAuth2LoginStatus.EXISTING_USER, ACCESS_TOKEN_PARAM, accessToken);
    }

    private String newUserRedirectUrl(OAuth2UserInfo oAuth2UserInfo) {
        String signupToken = jwtProvider.issueSignupToken(
                oAuth2UserInfo.getProvider(),
                oAuth2UserInfo.getProviderId(),
                oAuth2UserInfo.getEmail(),
                oAuth2UserInfo.getName()
        );
        return buildFragmentUrl(OAuth2LoginStatus.NEW_USER, SIGNUP_TOKEN_PARAM, signupToken);
    }

    private String buildFragmentUrl(OAuth2LoginStatus loginStatus, String tokenParam, String tokenValue) {
        StringBuilder fragment = new StringBuilder()
                .append(STATUS_PARAM).append('=').append(loginStatus.name());
        if (tokenParam != null) {
            fragment.append('&').append(tokenParam).append('=').append(encode(tokenValue));
        }
        return frontendRedirectUri + "#" + fragment;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
