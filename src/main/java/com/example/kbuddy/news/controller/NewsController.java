package com.example.kbuddy.news.controller;

import com.example.kbuddy.global.response.ApiResponse;
import com.example.kbuddy.news.client.NewsClient;
import com.example.kbuddy.news.dto.NewsItem;
import com.example.kbuddy.news.dto.NewsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "뉴스", description = "외국인 유학생 관련 뉴스를 AI 서버로부터 대신 조회해주는 프록시 API")
@RestController
@RequiredArgsConstructor
public class NewsController {

    private final NewsClient newsClient;

    @Operation(
            summary = "맞춤 뉴스 조회",
            description = "AI 서버가 네이버 뉴스 검색과 요약을 통해 수집한 외국인 유학생 관련 뉴스를 백엔드가 대신 호출해 반환합니다. "
                    + "결과가 없을 수 있으며, 이 경우에도 200과 빈 배열을 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공 (code: NEWS_FETCHED, 뉴스가 없으면 빈 배열)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 요청(토큰 없음/만료/서명 오류 등)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503",
                    description = "AI 서버 뉴스 API 통신 실패(timeout/네트워크 오류/5xx/응답 형식 오류) (code: NEWS_SERVICE_UNAVAILABLE)"
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/api/news")
    public ResponseEntity<ApiResponse<List<NewsItem>>> getNews() {
        NewsResponse response = newsClient.getNews();
        List<NewsItem> news = response != null && response.news() != null ? response.news() : List.of();
        return ResponseEntity.ok(
                ApiResponse.success("NEWS_FETCHED", "뉴스 목록을 조회했습니다.", news)
        );
    }
}
