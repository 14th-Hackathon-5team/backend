package com.example.kbuddy.user.controller;

import com.example.kbuddy.global.response.ApiResponse;
import com.example.kbuddy.user.dto.UserResponse;
import com.example.kbuddy.user.dto.UserSignupRequest;
import com.example.kbuddy.user.dto.UserSignupResponse;
import com.example.kbuddy.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/api/users/me")
    public ResponseEntity<ApiResponse<UserSignupResponse>> signup(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserSignupRequest request
    ) {
        UserSignupResponse response = userService.signup(jwt, request);
        return ResponseEntity.ok(
                ApiResponse.success("USER_CREATED", "회원가입 정보가 저장되었습니다.", response)
        );
    }

    @GetMapping("/api/users/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyInfo(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        UserResponse response = userService.getMyInfo(userId);
        return ResponseEntity.ok(
                ApiResponse.success("USER_INFO_FETCHED", "사용자 정보를 조회했습니다.", response)
        );
    }
}
