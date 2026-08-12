package com.example.kbuddy.auth.service;

import com.example.kbuddy.auth.oauth.AuthProvider;
import com.example.kbuddy.auth.oauth.CustomOAuth2User;
import com.example.kbuddy.auth.oauth.OAuth2LoginStatus;
import com.example.kbuddy.user.entity.User;
import com.example.kbuddy.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OAuth2UserService oAuth2UserService;

    @Test
    void google_attributes가_공통_OAuth2UserInfo로_정상_변환된다() {
        when(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "109876543210987654321"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByEmail("test@gmail.com")).thenReturn(false);

        CustomOAuth2User customOAuth2User = oAuth2UserService.toCustomOAuth2User("google", googleAttributes());

        assertThat(customOAuth2User.getOAuth2UserInfo().getProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(customOAuth2User.getOAuth2UserInfo().getProviderId()).isEqualTo("109876543210987654321");
        assertThat(customOAuth2User.getOAuth2UserInfo().getEmail()).isEqualTo("test@gmail.com");
        assertThat(customOAuth2User.getOAuth2UserInfo().getName()).isEqualTo("홍길동");
    }

    @Test
    void kakao_attributes가_공통_OAuth2UserInfo로_정상_변환된다() {
        when(userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, "12345"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByEmail("kakao@example.com")).thenReturn(false);

        CustomOAuth2User customOAuth2User = oAuth2UserService.toCustomOAuth2User("kakao", kakaoAttributes());

        assertThat(customOAuth2User.getOAuth2UserInfo().getProvider()).isEqualTo(AuthProvider.KAKAO);
        assertThat(customOAuth2User.getOAuth2UserInfo().getProviderId()).isEqualTo("12345");
        assertThat(customOAuth2User.getOAuth2UserInfo().getEmail()).isEqualTo("kakao@example.com");
        assertThat(customOAuth2User.getOAuth2UserInfo().getName()).isEqualTo("김철수");
    }

    @Test
    void provider와_providerId로_기존_User가_존재하면_EXISTING_USER_상태를_반환한다() {
        User existingUser = mock(User.class);
        when(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "109876543210987654321"))
                .thenReturn(Optional.of(existingUser));

        CustomOAuth2User customOAuth2User = oAuth2UserService.toCustomOAuth2User("google", googleAttributes());

        assertThat(customOAuth2User.getLoginStatus()).isEqualTo(OAuth2LoginStatus.EXISTING_USER);
    }

    @Test
    void provider와_providerId로_User가_없고_email도_없으면_NEW_USER_상태를_반환한다() {
        when(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "109876543210987654321"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByEmail("test@gmail.com")).thenReturn(false);

        CustomOAuth2User customOAuth2User = oAuth2UserService.toCustomOAuth2User("google", googleAttributes());

        assertThat(customOAuth2User.getLoginStatus()).isEqualTo(OAuth2LoginStatus.NEW_USER);
    }

    @Test
    void provider와_providerId로_User가_없지만_email이_이미_존재하면_EMAIL_DUPLICATED_상태를_반환한다() {
        when(userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "109876543210987654321"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByEmail("test@gmail.com")).thenReturn(true);

        CustomOAuth2User customOAuth2User = oAuth2UserService.toCustomOAuth2User("google", googleAttributes());

        assertThat(customOAuth2User.getLoginStatus()).isEqualTo(OAuth2LoginStatus.EMAIL_DUPLICATED);
    }

    private Map<String, Object> googleAttributes() {
        return Map.of(
                "sub", "109876543210987654321",
                "email", "test@gmail.com",
                "name", "홍길동"
        );
    }

    private Map<String, Object> kakaoAttributes() {
        return Map.of(
                "id", 12345L,
                "kakao_account", Map.of("email", "kakao@example.com", "profile", Map.of("nickname", "김철수"))
        );
    }
}
