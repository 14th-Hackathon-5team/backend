package com.example.kbuddy.user.service;

import com.example.kbuddy.auth.jwt.JwtProvider;
import com.example.kbuddy.auth.oauth.AuthProvider;
import com.example.kbuddy.calendar.repository.CalendarEventRepository;
import com.example.kbuddy.global.exception.BusinessException;
import com.example.kbuddy.global.exception.ErrorCode;
import com.example.kbuddy.user.dto.UserResponse;
import com.example.kbuddy.user.dto.UserSignupRequest;
import com.example.kbuddy.user.dto.UserSignupResponse;
import com.example.kbuddy.user.dto.UserUpdateRequest;
import com.example.kbuddy.user.entity.HousingType;
import com.example.kbuddy.user.entity.Language;
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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.inOrder;
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

    @Mock
    private CalendarEventRepository calendarEventRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, jwtProvider, calendarEventRepository);
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
    void 회원가입_요청의_language가_KOREAN이면_User에_KOREAN이_저장된다() {
        Jwt jwt = signupJwt("GOOGLE", "109876543210987654321", "test@gmail.com", "OAuth상의이름");
        UserSignupRequest request = validRequest("김철수", Language.KOREAN);

        User savedUser = mock(User.class);
        when(savedUser.getId()).thenReturn(1L);
        when(userRepository.existsByEmail("test@gmail.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtProvider.issueAccessToken(1L)).thenReturn("access-token-value");

        userService.signup(jwt, request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getLanguage()).isEqualTo(Language.KOREAN);
    }

    @Test
    void 회원가입_요청의_language가_ENGLISH이면_User에_ENGLISH가_저장된다() {
        Jwt jwt = signupJwt("GOOGLE", "109876543210987654321", "test@gmail.com", "OAuth상의이름");
        UserSignupRequest request = validRequest("김철수", Language.ENGLISH);

        User savedUser = mock(User.class);
        when(savedUser.getId()).thenReturn(1L);
        when(userRepository.existsByEmail("test@gmail.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtProvider.issueAccessToken(1L)).thenReturn("access-token-value");

        userService.signup(jwt, request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getLanguage()).isEqualTo(Language.ENGLISH);
    }

    @Test
    void 정상적으로_현재_사용자_정보를_조회할_수_있다() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getEmail()).thenReturn("test@gmail.com");
        when(user.getName()).thenReturn("김철수");
        when(user.getNationality()).thenReturn("CN");
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
        when(user.getLanguage()).thenReturn(Language.ENGLISH);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.getMyInfo(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("test@gmail.com");
        assertThat(response.name()).isEqualTo("김철수");
        assertThat(response.nationality()).isEqualTo("CN");
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
        assertThat(response.language()).isEqualTo(Language.ENGLISH);
    }

    @Test
    void 존재하지_않는_userId로_조회하면_USER_NOT_FOUND_예외가_발생한다() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMyInfo(999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void 일부_필드만_수정하면_해당_필드만_변경되고_나머지는_기존_값을_유지한다() {
        User user = baselineUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserUpdateRequest request = new UserUpdateRequest(
                null, null, null, null,
                "연세대학교",
                null, null, null, null, null, null, null, null, null,
                null
        );

        UserResponse response = userService.updateMyInfo(1L, request);

        assertThat(response.schoolName()).isEqualTo("연세대학교");
        assertThat(response.name()).isEqualTo("김철수");
        assertThat(response.nationality()).isEqualTo("CN");
        assertThat(response.birthYear()).isEqualTo(2000);
        assertThat(response.userStatus()).isEqualTo(UserStatus.UNDERGRADUATE);
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
    void 여러_필드를_동시에_수정할_수_있다() {
        User user = baselineUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserUpdateRequest request = new UserUpdateRequest(
                "박영희", null, 1998, null,
                null, null, null, null, null,
                HousingType.RENT, null, PartTimeStatus.WORKING, null, null,
                null
        );

        UserResponse response = userService.updateMyInfo(1L, request);

        assertThat(response.name()).isEqualTo("박영희");
        assertThat(response.birthYear()).isEqualTo(1998);
        assertThat(response.housingType()).isEqualTo(HousingType.RENT);
        assertThat(response.partTimeStatus()).isEqualTo(PartTimeStatus.WORKING);
        assertThat(response.nationality()).isEqualTo("CN");
        assertThat(response.schoolName()).isEqualTo("서울대학교");
    }

    @Test
    void 수정_가능한_필드_14개_전체가_정상적으로_매핑된다() {
        User user = baselineUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserUpdateRequest request = new UserUpdateRequest(
                "박영희",
                "VN",
                1998,
                UserStatus.GRADUATE,
                "연세대학교",
                LocalDate.of(2023, 9, 1),
                VisaType.D4,
                false,
                LocalDate.of(2028, 9, 1),
                HousingType.RENT,
                true,
                PartTimeStatus.WORKING,
                TopikLevel.LEVEL_4,
                TopikLevel.LEVEL_6,
                null
        );

        UserResponse response = userService.updateMyInfo(1L, request);

        assertThat(response.name()).isEqualTo("박영희");
        assertThat(response.nationality()).isEqualTo("VN");
        assertThat(response.birthYear()).isEqualTo(1998);
        assertThat(response.userStatus()).isEqualTo(UserStatus.GRADUATE);
        assertThat(response.schoolName()).isEqualTo("연세대학교");
        assertThat(response.entryDate()).isEqualTo(LocalDate.of(2023, 9, 1));
        assertThat(response.visaType()).isEqualTo(VisaType.D4);
        assertThat(response.hasAlienRegistration()).isFalse();
        assertThat(response.stayExpirationDate()).isEqualTo(LocalDate.of(2028, 9, 1));
        assertThat(response.housingType()).isEqualTo(HousingType.RENT);
        assertThat(response.isParentSupported()).isTrue();
        assertThat(response.partTimeStatus()).isEqualTo(PartTimeStatus.WORKING);
        assertThat(response.currentTopikLevel()).isEqualTo(TopikLevel.LEVEL_4);
        assertThat(response.targetTopikLevel()).isEqualTo(TopikLevel.LEVEL_6);
    }

    @Test
    void PATCH에서_language만_수정하면_language가_변경된다() {
        User user = baselineUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserUpdateRequest request = new UserUpdateRequest(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                Language.ENGLISH
        );

        UserResponse response = userService.updateMyInfo(1L, request);

        assertThat(response.language()).isEqualTo(Language.ENGLISH);
        assertThat(response.name()).isEqualTo("김철수");
        assertThat(response.schoolName()).isEqualTo("서울대학교");
    }

    @Test
    void PATCH에서_language를_보내지_않으면_기존_language가_유지된다() {
        User user = baselineUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserUpdateRequest request = new UserUpdateRequest(
                null, null, null, null,
                "연세대학교",
                null, null, null, null, null, null, null, null, null,
                null
        );

        UserResponse response = userService.updateMyInfo(1L, request);

        assertThat(response.schoolName()).isEqualTo("연세대학교");
        assertThat(response.language()).isEqualTo(Language.KOREAN);
    }

    @Test
    void 존재하지_않는_userId로_수정하면_USER_NOT_FOUND_예외가_발생한다() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        UserUpdateRequest request = new UserUpdateRequest(
                "박영희", null, null, null, null, null, null, null, null, null, null, null, null, null,
                null
        );

        assertThatThrownBy(() -> userService.updateMyInfo(999L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void 수정할_값이_하나도_없으면_USER_UPDATE_EMPTY_예외가_발생한다() {
        User user = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserUpdateRequest request = new UserUpdateRequest(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null
        );

        assertThatThrownBy(() -> userService.updateMyInfo(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_UPDATE_EMPTY);
    }

    @Test
    void 회원_탈퇴_시_개인_일정을_먼저_삭제한_후_User를_삭제한다() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteMyAccount(1L);

        InOrder inOrder = inOrder(calendarEventRepository, userRepository);
        inOrder.verify(calendarEventRepository).deleteByUserId(1L);
        inOrder.verify(userRepository).delete(user);
    }

    @Test
    void 존재하지_않는_userId로_탈퇴하면_USER_NOT_FOUND_예외가_발생하고_삭제가_호출되지_않는다() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteMyAccount(999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(calendarEventRepository, never()).deleteByUserId(any());
        verify(userRepository, never()).delete(any());
    }

    @Test
    void UserUpdateRequest에는_수정_불가능한_필드가_존재하지_않는다() {
        var componentNames = Arrays.stream(UserUpdateRequest.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertThat(componentNames)
                .doesNotContain("id", "provider", "providerId", "email", "createdAt", "updatedAt");
    }

    private User baselineUser() {
        User user = mock(User.class, CALLS_REAL_METHODS);
        when(user.getId()).thenReturn(1L);
        when(user.getEmail()).thenReturn("test@gmail.com");
        user.updateProfile(
                "김철수",
                "CN",
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
                TopikLevel.LEVEL_5,
                Language.KOREAN
        );
        return user;
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
        return validRequest(name, Language.KOREAN);
    }

    private UserSignupRequest validRequest(String name, Language language) {
        return new UserSignupRequest(
                name,
                "CN",
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
                TopikLevel.LEVEL_5,
                language
        );
    }
}
