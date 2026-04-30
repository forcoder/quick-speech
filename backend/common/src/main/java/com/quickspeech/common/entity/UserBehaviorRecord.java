package com.quickspeech.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user_behavior_record")
public class UserBehaviorRecord extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "session_id", length = 200)
    private String sessionId;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Column(name = "original_text", columnDefinition = "TEXT")
    private String originalText;

    @Column(name = "modified_text", columnDefinition = "TEXT")
    private String modifiedText;

    @Column(name = "context", length = 500)
    private String context;

    @Column(name = "app_package", length = 200)
    private String appPackage;

    @Column(name = "scene", length = 100)
    private String scene;

    @Column(name = "accepted_suggestion", length = 500)
    private String acceptedSuggestion;

    @Column(name = "rejected_suggestions", columnDefinition = "TEXT")
    private String rejectedSuggestions;

    @Column(name = "extra_data", columnDefinition = "TEXT")
    private String extraData;
}
