package com.example.kbuddy.user.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HousingTypeTest {

    @Test
    void HousingType은_정의된_6개_상수를_가진다() {
        assertThat(HousingType.values())
                .extracting(Enum::name)
                .containsExactly(
                        "DORMITORY",
                        "RENT",
                        "HOMESTAY",
                        "GOSIWON",
                        "SHARE_HOUSE",
                        "OTHER"
                );
    }
}
