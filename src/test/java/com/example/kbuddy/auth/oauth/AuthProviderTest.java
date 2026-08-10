package com.example.kbuddy.auth.oauth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthProviderTest {

    @Test
    void AuthProvider는_KAKAO와_GOOGLE_상수를_가진다() {
        assertThat(AuthProvider.values())
                .extracting(Enum::name)
                .containsExactly("KAKAO", "GOOGLE");
    }
}
