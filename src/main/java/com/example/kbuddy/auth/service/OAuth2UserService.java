package com.example.kbuddy.auth.service;

import com.example.kbuddy.auth.oauth.CustomOAuth2User;
import com.example.kbuddy.auth.oauth.OAuth2LoginStatus;
import com.example.kbuddy.auth.oauth.OAuth2UserInfo;
import com.example.kbuddy.auth.oauth.OAuth2UserInfoFactory;
import com.example.kbuddy.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class OAuth2UserService
        implements org.springframework.security.oauth2.client.userinfo.OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final org.springframework.security.oauth2.client.userinfo.OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate =
            new DefaultOAuth2UserService();

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = delegate.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        return toCustomOAuth2User(registrationId, oAuth2User.getAttributes());
    }

    CustomOAuth2User toCustomOAuth2User(String registrationId, Map<String, Object> attributes) {
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.of(registrationId, attributes);
        OAuth2LoginStatus loginStatus = resolveLoginStatus(oAuth2UserInfo);
        return new CustomOAuth2User(oAuth2UserInfo, loginStatus, attributes);
    }

    private OAuth2LoginStatus resolveLoginStatus(OAuth2UserInfo oAuth2UserInfo) {
        boolean existsByProviderAndProviderId = userRepository
                .findByProviderAndProviderId(oAuth2UserInfo.getProvider(), oAuth2UserInfo.getProviderId())
                .isPresent();
        if (existsByProviderAndProviderId) {
            return OAuth2LoginStatus.EXISTING_USER;
        }
        if (userRepository.existsByEmail(oAuth2UserInfo.getEmail())) {
            return OAuth2LoginStatus.EMAIL_DUPLICATED;
        }
        return OAuth2LoginStatus.NEW_USER;
    }
}
