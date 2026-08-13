package com.example.kbuddy.guide.dto;

import com.example.kbuddy.guide.entity.Guide;
import com.example.kbuddy.guide.entity.GuideCategory;

public record GuideResponse(
        Long guideId,
        GuideCategory category,
        String title
) {

    public static GuideResponse from(Guide guide) {
        return new GuideResponse(
                guide.getId(),
                guide.getCategory(),
                guide.getTitle()
        );
    }
}
