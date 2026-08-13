package com.example.kbuddy.guide.controller;

import com.example.kbuddy.guide.dto.GuideDetailResponse;
import com.example.kbuddy.guide.dto.GuideResponse;
import com.example.kbuddy.guide.entity.GuideCategory;
import com.example.kbuddy.guide.service.GuideService;
import com.example.kbuddy.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "세부정보", description = "비자, 토픽, 법률, 대입 가이드를 조회하는 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/guides")
public class GuideController {

    private final GuideService guideService;

    @Operation(
            summary = "카테고리별 가이드 목록 조회",
            description = "선택한 카테고리(비자, 토픽, 법률, 대입)의 가이드 목록을 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공 (code: GUIDE_LIST_FETCHED)"
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<ApiResponse<List<GuideResponse>>> getGuidesByCategory(
            @RequestParam GuideCategory category
    ) {
        List<GuideResponse> response = guideService.getGuidesByCategory(category);
        return ResponseEntity.ok(
                ApiResponse.success("GUIDE_LIST_FETCHED", "가이드 목록을 조회했습니다.", response)
        );
    }

    @Operation(
            summary = "가이드 상세 조회",
            description = "선택한 가이드의 상세 설명과 참고 링크를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공 (code: GUIDE_DETAIL_FETCHED)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 가이드 (code: GUIDE_NOT_FOUND)"
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{guideId}")
    public ResponseEntity<ApiResponse<GuideDetailResponse>> getGuideDetail(@PathVariable Long guideId) {
        GuideDetailResponse response = guideService.getGuideDetail(guideId);
        return ResponseEntity.ok(
                ApiResponse.success("GUIDE_DETAIL_FETCHED", "가이드 상세 정보를 조회했습니다.", response)
        );
    }
}
