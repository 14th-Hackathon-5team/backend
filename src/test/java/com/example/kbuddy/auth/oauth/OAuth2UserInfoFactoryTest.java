package com.example.kbuddy.auth.oauth;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2UserInfoFactoryTest {

    @Test
    void registrationId가_google이면_GoogleOAuth2UserInfo를_생성한다() {
        Map<String, Object> attributes = Map.of("sub", "1", "email", "a@a.com", "name", "a");

        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.of("google", attributes);

        assertThat(userInfo).isInstanceOf(GoogleOAuth2UserInfo.class);
        assertThat(userInfo.getProvider()).isEqualTo(AuthProvider.GOOGLE);
    }

    @Test
    void registrationId가_kakao이면_KakaoOAuth2UserInfo를_생성한다() {
        Map<String, Object> attributes = Map.of(
                "id", 1L,
                "kakao_account", Map.of("email", "a@a.com", "profile", Map.of("nickname", "a"))
        );

        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.of("kakao", attributes);

        assertThat(userInfo).isInstanceOf(KakaoOAuth2UserInfo.class);
        assertThat(userInfo.getProvider()).isEqualTo(AuthProvider.KAKAO);
    }
}
