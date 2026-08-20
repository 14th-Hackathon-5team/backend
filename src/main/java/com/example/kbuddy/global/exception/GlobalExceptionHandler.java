package com.example.kbuddy.global.exception;

import com.example.kbuddy.ai.client.AiClientException;
import com.example.kbuddy.global.response.ApiResponse;
import com.example.kbuddy.job.client.SeoulJobClientException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(AiClientException.class)
    public ResponseEntity<ApiResponse<Void>> handleAiClientException(AiClientException e) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.fail("AI_SERVICE_UNAVAILABLE", "AI 서비스에 일시적으로 연결할 수 없습니다. 잠시 후 다시 시도해주세요."));
    }

    @ExceptionHandler(SeoulJobClientException.class)
    public ResponseEntity<ApiResponse<Void>> handleSeoulJobClientException(SeoulJobClientException e) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.fail("SEOUL_JOB_SERVICE_UNAVAILABLE", "채용공고 서비스에 일시적으로 연결할 수 없습니다. 잠시 후 다시 시도해주세요."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .orElse("요청 값이 올바르지 않습니다.");
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail("INVALID_REQUEST", message));
    }
}
