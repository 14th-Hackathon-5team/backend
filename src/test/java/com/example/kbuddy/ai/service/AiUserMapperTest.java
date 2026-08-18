package com.example.kbuddy.ai.service;

import com.example.kbuddy.ai.dto.AiUser;
import com.example.kbuddy.user.dto.UserResponse;
import com.example.kbuddy.user.entity.HousingType;
import com.example.kbuddy.user.entity.Language;
import com.example.kbuddy.user.entity.PartTimeStatus;
import com.example.kbuddy.user.entity.TopikLevel;
import com.example.kbuddy.user.entity.UserStatus;
import com.example.kbuddy.user.entity.VisaType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AiUserMapperTest {

    private final AiUserMapper mapper = new AiUserMapper();

    @Test
    void UserResponse의_모든_필드가_AiUser로_정확히_매핑된다() {
        UserResponse user = new UserResponse(
                1L,
                "test@gmail.com",
                "김철수",
                "VN",
                2000,
                UserStatus.UNDERGRADUATE,
                "경북대학교",
                LocalDate.of(2026, 2, 20),
                VisaType.D2,
                true,
                LocalDate.of(2027, 2, 19),
                HousingType.DORMITORY,
                true,
                PartTimeStatus.NOT_PLANNED,
                TopikLevel.LEVEL_3,
                TopikLevel.LEVEL_5,
                Language.KOREAN
        );

        AiUser aiUser = mapper.toAiUser(user);

        assertThat(aiUser.userId()).isEqualTo(1L);
        assertThat(aiUser.nationality()).isEqualTo("VN");
        assertThat(aiUser.birthYear()).isEqualTo(2000);
        assertThat(aiUser.userStatus()).isEqualTo(UserStatus.UNDERGRADUATE);
        assertThat(aiUser.schoolName()).isEqualTo("경북대학교");
        assertThat(aiUser.entryDate()).isEqualTo(LocalDate.of(2026, 2, 20));
        assertThat(aiUser.visaType()).isEqualTo(VisaType.D2);
        assertThat(aiUser.hasAlienRegistration()).isTrue();
        assertThat(aiUser.stayExpirationDate()).isEqualTo(LocalDate.of(2027, 2, 19));
        assertThat(aiUser.housingType()).isEqualTo(HousingType.DORMITORY);
        assertThat(aiUser.isParentSupported()).isTrue();
        assertThat(aiUser.partTimeStatus()).isEqualTo(PartTimeStatus.NOT_PLANNED);
        // User Entity/UserResponse에 아직 없는 필드라 매핑 데이터가 없으므로 항상 null이어야 한다.
        assertThat(aiUser.partTimeStartDate()).isNull();
        assertThat(aiUser.hasPartTimePermit()).isNull();
        assertThat(aiUser.currentTopikLevel()).isEqualTo(TopikLevel.LEVEL_3);
        assertThat(aiUser.targetTopikLevel()).isEqualTo(TopikLevel.LEVEL_5);
        assertThat(aiUser.language()).isEqualTo(Language.KOREAN);
    }

    @Test
    void nationality는_변환없이_원본_ISO_코드가_그대로_전달된다() {
        UserResponse user = new UserResponse(
                2L, "a@a.com", "이름", "JP", 1999, UserStatus.GRADUATE, null,
                LocalDate.of(2020, 1, 1), VisaType.OTHER, false, null, null, null,
                PartTimeStatus.NOT_PLANNED, TopikLevel.NONE, TopikLevel.NONE, Language.ENGLISH
        );

        assertThat(mapper.toAiUser(user).nationality()).isEqualTo("JP");
    }
}
