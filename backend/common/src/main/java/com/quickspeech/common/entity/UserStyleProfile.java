package com.quickspeech.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user_style_profile")
public class UserStyleProfile extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "formality_level")
    private Double formalityLevel = 0.5;

    @Column(name = "verbosity_level")
    private Double verbosityLevel = 0.5;

    @Column(name = "emoji_frequency")
    private Double emojiFrequency = 0.3;

    @Column(name = "preferred_tone", length = 50)
    private String preferredTone = "neutral";

    @Column(name = "common_phrases", columnDefinition = "TEXT")
    private String commonPhrases;

    @Column(name = "vocabulary_level", length = 50)
    private String vocabularyLevel = "standard";

    @Column(name = "avg_sentence_length")
    private Double avgSentenceLength = 20.0;

    @Column(name = "preferred_language_mix", length = 100)
    private String preferredLanguageMix = "zh";

    @Column(name = "style_keywords", columnDefinition = "TEXT")
    private String styleKeywords;

    @Column(name = "sample_count")
    private Integer sampleCount = 0;

    @Column(name = "last_analyzed_at")
    private java.time.LocalDateTime lastAnalyzedAt;
}
