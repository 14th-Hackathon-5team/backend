package com.example.kbuddy.job.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 서울 열린데이터광장 Open API는 서비스명(GlobalJobSearch)을 응답 최상위 키로 그대로 사용한다.
 */
public record SeoulJobApiEnvelope(
        @JsonProperty("GlobalJobSearch") SeoulJobApiPayload globalJobSearch
) {
}
