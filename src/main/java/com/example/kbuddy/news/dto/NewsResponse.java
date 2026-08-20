package com.example.kbuddy.news.dto;

import java.util.List;

/**
 * AI 서버 GET /news의 응답을 그대로 담는 envelope. 최상위 키가 "news"로 Java 필드명과 그대로 일치해
 * 별도 JsonProperty 매핑이 필요 없다.
 */
public record NewsResponse(
        List<NewsItem> news
) {
}
