package com.example.kbuddy.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS", "이미 회원가입이 완료된 사용자입니다."),
    INVALID_SIGNUP_TOKEN(HttpStatus.BAD_REQUEST, "INVALID_SIGNUP_TOKEN", "유효하지 않은 회원가입 토큰입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "존재하지 않는 사용자입니다."),
    USER_UPDATE_EMPTY(HttpStatus.BAD_REQUEST, "USER_UPDATE_EMPTY", "수정할 정보가 없습니다."),
    GUIDE_NOT_FOUND(HttpStatus.NOT_FOUND, "GUIDE_NOT_FOUND", "존재하지 않는 가이드입니다."),
    CALENDAR_EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CALENDAR_EVENT_NOT_FOUND", "존재하지 않는 일정입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
