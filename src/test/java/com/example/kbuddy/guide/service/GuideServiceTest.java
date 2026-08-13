package com.example.kbuddy.guide.service;

import com.example.kbuddy.guide.dto.GuideResponse;
import com.example.kbuddy.guide.entity.Guide;
import com.example.kbuddy.guide.entity.GuideCategory;
import com.example.kbuddy.guide.repository.GuideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuideServiceTest {

    @Mock
    private GuideRepository guideRepository;

    private GuideService guideService;

    @BeforeEach
    void setUp() {
        guideService = new GuideService(guideRepository);
    }

    @Test
    void 카테고리로_가이드_목록을_조회한다() {
        Guide guide = guide(1L, GuideCategory.VISA, "D-2 비자 연장 방법");
        when(guideRepository.findByCategoryOrderByIdAsc(GuideCategory.VISA))
                .thenReturn(List.of(guide));

        List<GuideResponse> responses = guideService.getGuidesByCategory(GuideCategory.VISA);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).guideId()).isEqualTo(1L);
        assertThat(responses.get(0).title()).isEqualTo("D-2 비자 연장 방법");
        assertThat(responses.get(0).category()).isEqualTo(GuideCategory.VISA);
    }

    private Guide guide(Long id, GuideCategory category, String title) {
        Guide guide = mock(Guide.class);
        when(guide.getId()).thenReturn(id);
        when(guide.getCategory()).thenReturn(category);
        when(guide.getTitle()).thenReturn(title);
        return guide;
    }
}
