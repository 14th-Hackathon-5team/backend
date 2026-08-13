package com.example.kbuddy.guide.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "guides")
public class Guide {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guide_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20, nullable = false)
    private GuideCategory category;

    @Column(name = "title", length = 100, nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "reference_url", length = 255)
    private String referenceUrl;

    public Guide(
            GuideCategory category,
            String title,
            String content,
            String referenceUrl
    ) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.referenceUrl = referenceUrl;
    }
}
