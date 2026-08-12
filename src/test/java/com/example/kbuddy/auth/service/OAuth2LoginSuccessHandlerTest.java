package com.example.kbuddy.auth.service;

import com.example.kbuddy.auth.jwt.JwtProvider;
import com.example.kbuddy.auth.oauth.AuthProvider;
import com.example.kbuddy.auth.oauth.CustomOAuth2User;
import com.example.kbuddy.auth.oauth.GoogleOAuth2UserInfo;
import com.example.kbuddy.auth.oauth.OAuth2LoginStatus;
import com.example.kbuddy.auth.oauth.OAuth2UserInfo;
import com.example.kbuddy.user.entity.User;
import com.example.kbuddy.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

    private static final String FRONTEND_REDIRECT_URI = "http://localhost:3000/oauth/callback";

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private OAuth2LoginSuccessHandler successHandler;

    @BeforeEach
    void setUp() {
        successHandler = new OAuth2LoginSuccessHandler(jwtProvider, userRepository, FRONTEND_REDIRECT_URI);
    }

    @Test
    void EXISTING_USER면_ACCESS_TOKEN을_발급하고_status와_accessToken을_fragment로_담아_redirect한다() throws Exception {
        CustomOAuth2User customOAuth2User = customOAuth2User(OAuth2LoginStatus.EXISTING_USER);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(customOAuth2User);

        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "109876543210987654321"))
                .thenReturn(Optional.of(user));
        when(jwtProvider.issueAccessToken(1L)).thenReturn("access-token-value");

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect(FRONTEND_REDIRECT_URI + "#status=EXISTING_USER&accessToken=access-token-value");
    }

    @Test
    void NEW_USER면_SIGNUP_TOKEN을_발급하고_status와_signupToken을_fragment로_담아_redirect한다() throws Exception {
        CustomOAuth2User customOAuth2User = customOAuth2User(OAuth2LoginStatus.NEW_USER);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(customOAuth2User);

        when(jwtProvider.issueSignupToken(AuthProvider.GOOGLE, "109876543210987654321", "test@gmail.com", "홍길동"))
                .thenReturn("signup-token-value");

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect(FRONTEND_REDIRECT_URI + "#status=NEW_USER&signupToken=signup-token-value");
    }

    @Test
    void EMAIL_DUPLICATED이면_토큰_발급_없이_status만_fragment로_담아_redirect한다() throws Exception {
        CustomOAuth2User customOAuth2User = customOAuth2User(OAuth2LoginStatus.EMAIL_DUPLICATED);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(customOAuth2User);

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendRedirect(FRONTEND_REDIRECT_URI + "#status=EMAIL_DUPLICATED");
        verifyNoInteractions(jwtProvider);
    }

    @Test
    void EXISTING_USER인데_User가_존재하지_않으면_OAuth2AuthenticationException을_던지고_redirect하지_않는다() throws Exception {
        CustomOAuth2User customOAuth2User = customOAuth2User(OAuth2LoginStatus.EXISTING_USER);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(customOAuth2User);

        when(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "109876543210987654321"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> successHandler.onAuthenticationSuccess(request, response, authentication))
                .isInstanceOf(OAuth2AuthenticationException.class);

        verify(response, never()).sendRedirect(any());
    }

    private CustomOAuth2User customOAuth2User(OAuth2LoginStatus loginStatus) {
        Map<String, Object> attributes = googleAttributes();
        OAuth2UserInfo oAuth2UserInfo = new GoogleOAuth2UserInfo(attributes);
        return new CustomOAuth2User(oAuth2UserInfo, loginStatus, attributes);
    }

    private Map<String, Object> googleAttributes() {
        return Map.of(
                "sub", "109876543210987654321",
                "email", "test@gmail.com",
                "name", "홍길동"
        );
    }
}
