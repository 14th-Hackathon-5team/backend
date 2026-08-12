package com.example.kbuddy.auth.oauth;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KakaoOAuth2UserInfoTest {

    @Test
    void provider는_KAKAO이다() {
        OAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(kakaoAttributes());

        assertThat(userInfo.getProvider()).isEqualTo(AuthProvider.KAKAO);
    }

    @Test
    void providerId는_id_값으로_추출된다() {
        OAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(kakaoAttributes());

        assertThat(userInfo.getProviderId()).isEqualTo("123456789");
    }

    @Test
    void email이_kakao_account에서_정상적으로_추출된다() {
        OAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(kakaoAttributes());

        assertThat(userInfo.getEmail()).isEqualTo("test@kakao.com");
    }

    @Test
    void name은_profile_nickname에서_정상적으로_추출된다() {
        OAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(kakaoAttributes());

        assertThat(userInfo.getName()).isEqualTo("카카오테스트");
    }

    private Map<String, Object> kakaoAttributes() {
        Map<String, Object> profile = Map.of(
                "nickname", "카카오테스트",
                "profile_image_url", "https://example.com/photo.jpg"
        );
        Map<String, Object> kakaoAccount = Map.of(
                "email", "test@kakao.com",
                "profile", profile
        );
        return Map.of(
                "id", 123456789L,
                "kakao_account", kakaoAccount
        );
    }
}
