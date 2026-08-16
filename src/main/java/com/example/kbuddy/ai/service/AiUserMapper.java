package com.example.kbuddy.ai.service;

import com.example.kbuddy.ai.dto.AiUser;
import com.example.kbuddy.user.dto.UserResponse;
import org.springframework.stereotype.Component;

/**
 * 인증된 로그인 사용자(UserResponse)를 AI 요청용 {@link AiUser}로 변환한다.
 *
 * STEP C의 TriggerService에도 User(Entity) -> AiUser 매핑이 존재하지만, TriggerService는
 * Scheduler 흐름 전용 orchestration 서비스라 Chat 요청 때문에 의존성을 추가하지 않는다
 * (이번 단계에서 TriggerService는 변경하지 않는다는 원칙과, 입력 타입이 Entity가 아닌
 * UserService의 기존 응답 DTO(UserResponse)라는 점에서도 자연스럽게 별도로 분리된다).
 */
@Component
public class AiUserMapper {

    public AiUser toAiUser(UserResponse user) {
        return new AiUser(
                user.id(),
                user.nationality(),
                user.birthYear(),
                user.userStatus(),
                user.schoolName(),
                user.entryDate(),
                user.visaType(),
                user.hasAlienRegistration(),
                user.stayExpirationDate(),
                user.housingType(),
                user.isParentSupported(),
                user.partTimeStatus(),
                user.currentTopikLevel(),
                user.targetTopikLevel(),
                user.language()
        );
    }
}
