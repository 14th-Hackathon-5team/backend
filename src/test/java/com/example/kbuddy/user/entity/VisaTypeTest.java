package com.example.kbuddy.user.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VisaTypeTest {

    @Test
    void VisaType은_정의된_7개_상수를_가진다() {
        assertThat(VisaType.values())
                .extracting(Enum::name)
                .containsExactly(
                        "D2",
                        "D4",
                        "H1",
                        "F2",
                        "F5",
                        "F6",
                        "OTHER"
                );
    }
}
