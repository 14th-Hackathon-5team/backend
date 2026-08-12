package com.example.kbuddy.user.service;

import com.example.kbuddy.auth.jwt.JwtProvider;
import com.example.kbuddy.auth.oauth.AuthProvider;
import com.example.kbuddy.global.exception.BusinessException;
import com.example.kbuddy.global.exception.ErrorCode;
import com.example.kbuddy.user.dto.UserResponse;
import com.example.kbuddy.user.dto.UserSignupRequest;
import com.example.kbuddy.user.dto.UserSignupResponse;
import com.example.kbuddy.user.entity.HousingType;
import com.example.kbuddy.user.entity.PartTimeStatus;
import com.example.kbuddy.user.entity.TopikLevel;
import com.example.kbuddy.user.entity.User;
import com.example.kbuddy.user.entity.UserStatus;
import com.example.kbuddy.user.entity.VisaType;
import com.example.kbuddy.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtProvider jwtProvider;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, jwtProvider);
    }

    @Test
    void 정상적인_SIGNUP_TOKEN과_Request면_User를_저장하고_ACCESS_TOKEN을_발급한다() {
        Jwt jwt = signupJwt("GOOGLE", "109876543210987654321", "test@gmail.com", "OAuth상의이름");
        UserSignupRequest request = validRequest("김철수");

        User savedUser = mock(User.class);
        when(savedUser.getId()).thenReturn(1L);
        when(userRepository.existsByEmail("test@gmail.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtProvider.issueAccessToken(1L)).thenReturn("access-token-value");

        UserSignupResponse response = userService.signup(jwt, request);

        assertThat(response.accessToken()).isEqualTo("access-token-value");
    }

    @Test
    void type이_ACCESS인_JWT면_INVALID_SIGNUP_TOKEN_예외가_발생한다() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .subject("1")
                .claim("type", "ACCESS")
                .build();
        UserSignupRequest request = validRequest("김철수");

        assertThatThrownBy(() -> userService.signup(jwt, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_SIGNUP_TOKEN);
    }

    @Test
    void provider_claim이_없으면_INVALID_SIGNUP_TOKEN_예외가_발생한다() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .subject("GOOGLE:109876543210987654321")
                .claim("type", "SIGNUP")
                .claim("providerId", "109876543210987654321")
                .claim("email", "test@gmail.com")
                .build();
        UserSignupRequest request = validRequest("김철수");

        assertThatThrownBy(() -> userService.signup(jwt, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_SIGNUP_TOKEN);
    }

    @Test
    void providerId_claim이_없으면_INVALID_SIGNUP_TOKEN_예외가_발생한다() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .subject("GOOGLE:109876543210987654321")
                .claim("type", "SIGNUP")
                .claim("provider", "GOOGLE")
                .claim("email", "test@gmail.com")
                .build();
        UserSignupRequest request = validRequest("김철수");

        assertThatThrownBy(() -> userService.signup(jwt, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_SIGNUP_TOKEN);
    }

    @Test
    void email_claim이_없으면_INVALID_SIGNUP_TOKEN_예외가_발생한다() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .subject("GOOGLE:109876543210987654321")
                .claim("type", "SIGNUP")
                .claim("provider", "GOOGLE")
                .claim("providerId", "109876543210987654321")
                .build();
        UserSignupRequest request = validRequest("김철수");

        assertThatThrownBy(() -> userService.signup(jwt, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_SIGNUP_TOKEN);
    }

    @Test
    void 이미_존재하는_email이면_USER_ALREADY_EXISTS_예외가_발생하고_save와_issueAccessToken은_호출되지_않는다() {
        Jwt jwt = signupJwt("GOOGLE", "109876543210987654321", "test@gmail.com", "홍길동");
        UserSignupRequest request = validRequest("김철수");

        when(userRepository.existsByEmail("test@gmail.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.signup(jwt, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_ALREADY_EXISTS);

        verify(userRepository, never()).save(any());
        verify(jwtProvider, never()).issueAccessToken(any());
    }

    @Test
    void Request의_name이_SIGNUP_TOKEN의_name과_다르면_Request의_name으로_User가_생성된다() {
        Jwt jwt = signupJwt("GOOGLE", "109876543210987654321", "test@gmail.com", "OAuth상의이름");
        UserSignupRequest request = validRequest("실제입력한이름");

        User savedUser = mock(User.class);
        when(savedUser.getId()).thenReturn(1L);
        when(userRepository.existsByEmail("test@gmail.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtProvider.issueAccessToken(1L)).thenReturn("access-token-value");

        userService.signup(jwt, request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getName()).isEqualTo("실제입력한이름");
        assertThat(userCaptor.getValue().getProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(userCaptor.getValue().getProviderId()).isEqualTo("109876543210987654321");
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("test@gmail.com");
    }

    @Test
    void 정상적으로_현재_사용자_정보를_조회할_수_있다() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getEmail()).thenReturn("test@gmail.com");
        when(user.getName()).thenReturn("김철수");
        when(user.getNationality()).thenReturn("중국");
        when(user.getBirthYear()).thenReturn(2000);
        when(user.getUserStatus()).thenReturn(UserStatus.UNDERGRADUATE);
        when(user.getSchoolName()).thenReturn("서울대학교");
        when(user.getEntryDate()).thenReturn(LocalDate.of(2022, 3, 1));
        when(user.getVisaType()).thenReturn(VisaType.D2);
        when(user.getHasAlienRegistration()).thenReturn(true);
        when(user.getStayExpirationDate()).thenReturn(LocalDate.of(2027, 3, 1));
        when(user.getHousingType()).thenReturn(HousingType.DORMITORY);
        when(user.getIsParentSupported()).thenReturn(false);
        when(user.getPartTimeStatus()).thenReturn(PartTimeStatus.SEARCHING);
        when(user.getCurrentTopikLevel()).thenReturn(TopikLevel.LEVEL_3);
        when(user.getTargetTopikLevel()).thenReturn(TopikLevel.LEVEL_5);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.getMyInfo(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("test@gmail.com");
        assertThat(response.name()).isEqualTo("김철수");
        assertThat(response.nationality()).isEqualTo("중국");
        assertThat(response.birthYear()).isEqualTo(2000);
        assertThat(response.userStatus()).isEqualTo(UserStatus.UNDERGRADUATE);
        assertThat(response.schoolName()).isEqualTo("서울대학교");
        assertThat(response.entryDate()).isEqualTo(LocalDate.of(2022, 3, 1));
        assertThat(response.visaType()).isEqualTo(VisaType.D2);
        assertThat(response.hasAlienRegistration()).isTrue();
        assertThat(response.stayExpirationDate()).isEqualTo(LocalDate.of(2027, 3, 1));
        assertThat(response.housingType()).isEqualTo(HousingType.DORMITORY);
        assertThat(response.isParentSupported()).isFalse();
        assertThat(response.partTimeStatus()).isEqualTo(PartTimeStatus.SEARCHING);
        assertThat(response.currentTopikLevel()).isEqualTo(TopikLevel.LEVEL_3);
        assertThat(response.targetTopikLevel()).isEqualTo(TopikLevel.LEVEL_5);
    }

    @Test
    void 존재하지_않는_userId로_조회하면_USER_NOT_FOUND_예외가_발생한다() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMyInfo(999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    private Jwt signupJwt(String provider, String providerId, String email, String name) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .subject(provider + ":" + providerId)
                .claim("type", "SIGNUP")
                .claim("provider", provider)
                .claim("providerId", providerId)
                .claim("email", email)
                .claim("name", name)
                .build();
    }

    private UserSignupRequest validRequest(String name) {
        return new UserSignupRequest(
                name,
                "중국",
                2000,
                UserStatus.UNDERGRADUATE,
                "서울대학교",
                LocalDate.of(2022, 3, 1),
                VisaType.D2,
                true,
                LocalDate.of(2027, 3, 1),
                HousingType.DORMITORY,
                false,
                PartTimeStatus.SEARCHING,
                TopikLevel.LEVEL_3,
                TopikLevel.LEVEL_5
        );
    }
}
