package com.example.kbuddy.news.dto;

import java.util.List;

public record NewsItem(
        String title,
        List<String> threeLineSummary,
        String detailedSummary,
        String link
) {
}
