package com.example.kbuddy.user.controller;

import com.example.kbuddy.global.response.ApiResponse;
import com.example.kbuddy.user.dto.UserResponse;
import com.example.kbuddy.user.dto.UserSignupRequest;
import com.example.kbuddy.user.dto.UserSignupResponse;
import com.example.kbuddy.user.dto.UserUpdateRequest;
import com.example.kbuddy.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자", description = "사용자 회원가입 및 프로필 정보를 관리하는 API")
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "회원가입",
            description = "OAuth 로그인 후 발급받은 SIGNUP_TOKEN을 사용하여 사용자의 추가 정보를 입력하고 회원가입을 완료합니다. "
                    + "이 API는 SIGNUP_TOKEN 인증이 필요합니다(ACCESS_TOKEN으로는 접근할 수 없습니다). "
                    + "성공 시 응답 데이터에 ACCESS_TOKEN이 포함됩니다(code: USER_CREATED)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "회원가입 성공 (code: USER_CREATED)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 값 검증 실패(code: INVALID_REQUEST) 또는 유효하지 않은/누락된 claim의 SIGNUP_TOKEN(code: INVALID_SIGNUP_TOKEN)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 요청(토큰 없음/만료/서명 오류 등)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "SIGNUP_TOKEN이 아닌 토큰(예: ACCESS_TOKEN)으로 접근"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 회원가입이 완료된 사용자 (code: USER_ALREADY_EXISTS)"
            )
    })
    @SecurityRequirement(name = "bearerAuth")
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

    @Operation(
            summary = "내 정보 조회",
            description = "현재 로그인한 사용자의 프로필 정보를 조회합니다. "
                    + "이 API는 ACCESS_TOKEN 인증이 필요합니다(SIGNUP_TOKEN으로는 접근할 수 없습니다)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공 (code: USER_INFO_FETCHED)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 요청(토큰 없음/만료/서명 오류 등)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "ACCESS_TOKEN이 아닌 토큰(예: SIGNUP_TOKEN)으로 접근"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 사용자 (code: USER_NOT_FOUND)"
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/api/users/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyInfo(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        UserResponse response = userService.getMyInfo(userId);
        return ResponseEntity.ok(
                ApiResponse.success("USER_INFO_FETCHED", "사용자 정보를 조회했습니다.", response)
        );
    }

    @Operation(
            summary = "내 정보 수정",
            description = "현재 로그인한 사용자의 프로필 정보를 부분 수정합니다. "
                    + "요청 본문에 입력하지 않은 필드는 기존 값이 그대로 유지됩니다. "
                    + "수정할 값이 하나도 없는 요청은 실패 처리됩니다(code: USER_UPDATE_EMPTY). "
                    + "이 API는 ACCESS_TOKEN 인증이 필요합니다(SIGNUP_TOKEN으로는 접근할 수 없습니다)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수정 성공 (code: USER_INFO_UPDATED)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 값 검증 실패(code: INVALID_REQUEST) 또는 수정할 값이 하나도 없는 요청(code: USER_UPDATE_EMPTY)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 요청(토큰 없음/만료/서명 오류 등)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "ACCESS_TOKEN이 아닌 토큰(예: SIGNUP_TOKEN)으로 접근"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 사용자 (code: USER_NOT_FOUND)"
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/api/users/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyInfo(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        Long userId = Long.valueOf(jwt.getSubject());
        UserResponse response = userService.updateMyInfo(userId, request);
        return ResponseEntity.ok(
                ApiResponse.success("USER_INFO_UPDATED", "사용자 정보를 수정했습니다.", response)
        );
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "현재 로그인한 사용자를 탈퇴 처리합니다. 사용자가 생성한 개인 일정도 함께 삭제되며, 공통 일정은 삭제되지 않습니다. "
                    + "이 작업은 되돌릴 수 없습니다. "
                    + "이 API는 ACCESS_TOKEN 인증이 필요합니다(SIGNUP_TOKEN으로는 접근할 수 없습니다)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "탈퇴 성공 (code: USER_DELETED)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 요청(토큰 없음/만료/서명 오류 등)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "ACCESS_TOKEN이 아닌 토큰(예: SIGNUP_TOKEN)으로 접근"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 사용자 (code: USER_NOT_FOUND)"
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/api/users/me")
    public ResponseEntity<ApiResponse<Void>> deleteMyAccount(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        userService.deleteMyAccount(userId);
        return ResponseEntity.ok(
                ApiResponse.success("USER_DELETED", "회원 탈퇴가 완료되었습니다.", null)
        );
    }
}
