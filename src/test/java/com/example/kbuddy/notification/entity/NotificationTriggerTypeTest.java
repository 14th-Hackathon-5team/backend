package com.example.kbuddy.notification.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTriggerTypeTest {

    @Test
    void NotificationTriggerType은_정의된_2개_상수를_가진다() {
        assertThat(NotificationTriggerType.values())
                .extracting(Enum::name)
                .containsExactly(
                        "VISA_EXPIRATION",
                        "ALIEN_REGISTRATION"
                );
    }
}
