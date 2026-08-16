package com.example.kbuddy.notification.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationCategoryTest {

    @Test
    void NotificationCategory는_정의된_10개_상수를_가진다() {
        assertThat(NotificationCategory.values())
                .extracting(Enum::name)
                .containsExactly(
                        "VISA",
                        "LEGAL",
                        "TOPIK",
                        "ADMISSION",
                        "SCHOOL",
                        "LIFE",
                        "PART_TIME",
                        "HOUSING",
                        "ENTRY",
                        "SUPPORT"
                );
    }
}
