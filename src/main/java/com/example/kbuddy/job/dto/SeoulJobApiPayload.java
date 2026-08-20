package com.example.kbuddy.job.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SeoulJobApiPayload(
        @JsonProperty("list_total_count") Integer listTotalCount,
        List<SeoulJobApiItem> row
) {
}
