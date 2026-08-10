package com.example.kbuddy.global.response;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void success_응답을_생성하면_전달한_code_message_data가_그대로_담긴다() {
        ApiResponse<String> response = ApiResponse.success("USER_CREATED", "회원가입 정보가 저장되었습니다.", "user-data");

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getCode()).isEqualTo("USER_CREATED");
        assertThat(response.getMessage()).isEqualTo("회원가입 정보가 저장되었습니다.");
        assertThat(response.getData()).isEqualTo("user-data");
    }

    @Test
    void fail_응답을_생성하면_success는_false이고_data는_null이다() {
        ApiResponse<Object> response = ApiResponse.fail("USER_ALREADY_EXISTS", "이미 회원가입이 완료된 사용자입니다.");

        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo("USER_ALREADY_EXISTS");
        assertThat(response.getMessage()).isEqualTo("이미 회원가입이 완료된 사용자입니다.");
        assertThat(response.getData()).isNull();
    }

    @Test
    void JSON으로_직렬화하면_success_code_message_data_4개_필드만_존재한다() throws Exception {
        ApiResponse<String> response = ApiResponse.success("USER_CREATED", "회원가입 정보가 저장되었습니다.", "user-data");

        String json = objectMapper.writeValueAsString(response);
        Map<String, Object> map = objectMapper.readValue(json, Map.class);

        assertThat(map).containsOnlyKeys("success", "code", "message", "data");
        assertThat(map.get("success")).isEqualTo(true);
        assertThat(map.get("code")).isEqualTo("USER_CREATED");
        assertThat(map.get("message")).isEqualTo("회원가입 정보가 저장되었습니다.");
        assertThat(map.get("data")).isEqualTo("user-data");
    }
}
