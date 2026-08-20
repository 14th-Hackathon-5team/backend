package com.example.kbuddy.job.controller;

import com.example.kbuddy.global.response.ApiResponse;
import com.example.kbuddy.job.dto.SeoulJobResponse;
import com.example.kbuddy.job.service.SeoulJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "일자리 정보", description = "서울 열린데이터광장 외국인 채용공고를 백엔드가 대신 조회해주는 프록시 API")
@RestController
@RequiredArgsConstructor
public class SeoulJobController {

    private final SeoulJobService seoulJobService;

    @Operation(
            summary = "서울 외국인 채용공고 조회",
            description = "서울 열린데이터광장 Open API(GlobalJobSearch)를 백엔드가 대신 호출해 결과를 반환합니다. "
                    + "이 API는 HTTPS를 지원하지 않아 브라우저에서 직접 호출 시 mixed-content로 차단되므로 프록시가 필요합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공 (code: SEOUL_JOB_LIST_FETCHED)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 요청(토큰 없음/만료/서명 오류 등)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503",
                    description = "서울 열린데이터광장 API 통신 실패(timeout/네트워크 오류/5xx/응답 형식 오류) (code: SEOUL_JOB_SERVICE_UNAVAILABLE)"
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/api/external/seoul-jobs")
    public ResponseEntity<ApiResponse<List<SeoulJobResponse>>> getJobs(
            @RequestParam(defaultValue = "1") int startIndex,
            @RequestParam(defaultValue = "20") int endIndex
    ) {
        List<SeoulJobResponse> response = seoulJobService.search(startIndex, endIndex);
        return ResponseEntity.ok(
                ApiResponse.success("SEOUL_JOB_LIST_FETCHED", "채용공고 목록을 조회했습니다.", response)
        );
    }
}
