package com.example.kbuddy.guide.dto;

import com.example.kbuddy.guide.entity.Guide;
import com.example.kbuddy.guide.entity.GuideCategory;

public record GuideDetailResponse(
        Long guideId,
        GuideCategory category,
        String title,
        String content,
        String referenceUrl
) {

    public static GuideDetailResponse from(Guide guide) {
        return new GuideDetailResponse(
                guide.getId(),
                guide.getCategory(),
                guide.getTitle(),
                guide.getContent(),
                guide.getReferenceUrl()
        );
    }
}
