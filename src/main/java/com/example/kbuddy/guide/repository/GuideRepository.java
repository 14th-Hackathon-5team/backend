package com.example.kbuddy.guide.repository;

import com.example.kbuddy.guide.entity.Guide;
import com.example.kbuddy.guide.entity.GuideCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuideRepository extends JpaRepository<Guide, Long> {

    List<Guide> findByCategoryOrderByIdAsc(GuideCategory category);
}
