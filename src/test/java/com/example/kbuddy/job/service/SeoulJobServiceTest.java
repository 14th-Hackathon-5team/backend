package com.example.kbuddy.job.service;

import com.example.kbuddy.job.client.SeoulJobClient;
import com.example.kbuddy.job.dto.SeoulJobApiEnvelope;
import com.example.kbuddy.job.dto.SeoulJobApiItem;
import com.example.kbuddy.job.dto.SeoulJobApiPayload;
import com.example.kbuddy.job.dto.SeoulJobResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeoulJobServiceTest {

    @Mock
    private SeoulJobClient seoulJobClient;

    private SeoulJobService seoulJobService;

    @BeforeEach
    void setUp() {
        seoulJobService = new SeoulJobService(seoulJobClient);
    }

    @Test
    void search는_row_배열을_SeoulJobResponse_리스트로_변환한다() {
        SeoulJobApiItem item = new SeoulJobApiItem(
                "카페 아르바이트", "설명", "홍길동", "한국어", "2026-08-01", "2026-08-02");
        when(seoulJobClient.search(1, 20)).thenReturn(
                new SeoulJobApiEnvelope(new SeoulJobApiPayload(1, List.of(item))));

        List<SeoulJobResponse> result = seoulJobService.search(1, 20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("카페 아르바이트");
        assertThat(result.get(0).content()).isEqualTo("설명");
        assertThat(result.get(0).writerName()).isEqualTo("홍길동");
        assertThat(result.get(0).language()).isEqualTo("한국어");
        assertThat(result.get(0).registeredAt()).isEqualTo("2026-08-01");
        assertThat(result.get(0).updatedAt()).isEqualTo("2026-08-02");
    }

    @Test
    void row가_null이면_빈_리스트를_반환한다() {
        when(seoulJobClient.search(1, 20)).thenReturn(
                new SeoulJobApiEnvelope(new SeoulJobApiPayload(0, null)));

        List<SeoulJobResponse> result = seoulJobService.search(1, 20);

        assertThat(result).isEmpty();
    }

    @Test
    void 결과_건수가_0건이면_빈_리스트를_반환한다() {
        when(seoulJobClient.search(1, 20)).thenReturn(
                new SeoulJobApiEnvelope(new SeoulJobApiPayload(0, List.of())));

        List<SeoulJobResponse> result = seoulJobService.search(1, 20);

        assertThat(result).isEmpty();
    }
}
