package com.example.kbuddy.job.dto;

public record SeoulJobResponse(
        String title,
        String content,
        String writerName,
        String language,
        String registeredAt,
        String updatedAt
) {

    public static SeoulJobResponse from(SeoulJobApiItem item) {
        return new SeoulJobResponse(
                item.titlNm(),
                item.cont(),
                item.writNm(),
                item.langGb(),
                item.regDt(),
                item.updDt()
        );
    }
}
