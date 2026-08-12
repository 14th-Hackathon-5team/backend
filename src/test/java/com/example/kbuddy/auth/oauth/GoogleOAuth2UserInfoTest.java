package com.example.kbuddy.auth.oauth;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleOAuth2UserInfoTest {

    @Test
    void provider는_GOOGLE이다() {
        OAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(googleAttributes());

        assertThat(userInfo.getProvider()).isEqualTo(AuthProvider.GOOGLE);
    }

    @Test
    void providerId는_sub_값으로_추출된다() {
        OAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(googleAttributes());

        assertThat(userInfo.getProviderId()).isEqualTo("109876543210987654321");
    }

    @Test
    void email이_정상적으로_추출된다() {
        OAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(googleAttributes());

        assertThat(userInfo.getEmail()).isEqualTo("test@gmail.com");
    }

    @Test
    void name이_정상적으로_추출된다() {
        OAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(googleAttributes());

        assertThat(userInfo.getName()).isEqualTo("홍길동");
    }

    private Map<String, Object> googleAttributes() {
        return Map.of(
                "sub", "109876543210987654321",
                "email", "test@gmail.com",
                "email_verified", true,
                "name", "홍길동",
                "picture", "https://example.com/photo.jpg"
        );
    }
}
