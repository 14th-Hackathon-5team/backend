package com.example.kbuddy.auth.oauth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User {

    private final OAuth2UserInfo oAuth2UserInfo;
    private final OAuth2LoginStatus loginStatus;
    private final Map<String, Object> attributes;

    public CustomOAuth2User(OAuth2UserInfo oAuth2UserInfo, OAuth2LoginStatus loginStatus, Map<String, Object> attributes) {
        this.oAuth2UserInfo = oAuth2UserInfo;
        this.loginStatus = loginStatus;
        this.attributes = attributes;
    }

    public OAuth2UserInfo getOAuth2UserInfo() {
        return oAuth2UserInfo;
    }

    public OAuth2LoginStatus getLoginStatus() {
        return loginStatus;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public List<GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getName() {
        return oAuth2UserInfo.getProviderId();
    }
}
