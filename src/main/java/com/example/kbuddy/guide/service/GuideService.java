package com.example.kbuddy.guide.service;

import com.example.kbuddy.guide.dto.GuideResponse;
import com.example.kbuddy.guide.entity.GuideCategory;
import com.example.kbuddy.guide.repository.GuideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GuideService {

    private final GuideRepository guideRepository;

    @Transactional(readOnly = true)
    public List<GuideResponse> getGuidesByCategory(GuideCategory category) {
        return guideRepository.findByCategoryOrderByIdAsc(category).stream()
                .map(GuideResponse::from)
                .toList();
    }
}
