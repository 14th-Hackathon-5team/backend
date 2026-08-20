package com.example.kbuddy.job.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SeoulJobApiItem(
        @JsonProperty("TITL_NM") String titlNm,
        @JsonProperty("CONT") String cont,
        @JsonProperty("WRIT_NM") String writNm,
        @JsonProperty("LANG_GB") String langGb,
        @JsonProperty("REG_DT") String regDt,
        @JsonProperty("UPD_DT") String updDt
) {
}
