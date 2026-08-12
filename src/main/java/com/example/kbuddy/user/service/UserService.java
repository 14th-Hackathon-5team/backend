package com.example.kbuddy.user.service;

import com.example.kbuddy.auth.jwt.JwtProvider;
import com.example.kbuddy.auth.jwt.TokenType;
import com.example.kbuddy.auth.oauth.AuthProvider;
import com.example.kbuddy.global.exception.BusinessException;
import com.example.kbuddy.global.exception.ErrorCode;
import com.example.kbuddy.user.dto.UserSignupRequest;
import com.example.kbuddy.user.dto.UserSignupResponse;
import com.example.kbuddy.user.entity.User;
import com.example.kbuddy.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String TYPE_CLAIM = "type";
    private static final String PROVIDER_CLAIM = "provider";
    private static final String PROVIDER_ID_CLAIM = "providerId";
    private static final String EMAIL_CLAIM = "email";

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @Transactional
    public UserSignupResponse signup(Jwt jwt, UserSignupRequest request) {
        validateSignupTokenType(jwt);

        AuthProvider provider = extractProvider(jwt);
        String providerId = extractRequiredClaim(jwt, PROVIDER_ID_CLAIM);
        String email = extractRequiredClaim(jwt, EMAIL_CLAIM);

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        User user = new User(
                provider,
                providerId,
                email,
                request.name(),
                request.nationality(),
                request.birthYear(),
                request.userStatus(),
                request.schoolName(),
                request.entryDate(),
                request.visaType(),
                request.hasAlienRegistration(),
                request.stayExpirationDate(),
                request.housingType(),
                request.isParentSupported(),
                request.partTimeStatus(),
                request.currentTopikLevel(),
                request.targetTopikLevel()
        );

        User savedUser = userRepository.save(user);
        String accessToken = jwtProvider.issueAccessToken(savedUser.getId());

        return new UserSignupResponse(accessToken);
    }

    private void validateSignupTokenType(Jwt jwt) {
        String type = jwt.getClaimAsString(TYPE_CLAIM);
        if (!TokenType.SIGNUP.name().equals(type)) {
            throw new BusinessException(ErrorCode.INVALID_SIGNUP_TOKEN);
        }
    }

    private AuthProvider extractProvider(Jwt jwt) {
        String provider = jwt.getClaimAsString(PROVIDER_CLAIM);
        if (provider == null || provider.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_SIGNUP_TOKEN);
        }
        try {
            return AuthProvider.valueOf(provider);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_SIGNUP_TOKEN);
        }
    }

    private String extractRequiredClaim(Jwt jwt, String claimName) {
        String value = jwt.getClaimAsString(claimName);
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_SIGNUP_TOKEN);
        }
        return value;
    }
}
