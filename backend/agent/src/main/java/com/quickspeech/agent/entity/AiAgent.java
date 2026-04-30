package com.quickspeech.agent.entity;

import com.quickspeech.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ai_agent")
public class AiAgent extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "model_provider", nullable = false, length = 50)
    private String modelProvider;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "temperature")
    private Double temperature = 0.7;

    @Column(name = "max_tokens")
    private Integer maxTokens = 2000;

    @Column(name = "top_p")
    private Double topP = 1.0;

    @Column(name = "frequency_penalty")
    private Double frequencyPenalty = 0.0;

    @Column(name = "presence_penalty")
    private Double presencePenalty = 0.0;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "knowledge_base_id")
    private Long knowledgeBaseId;

    @Column(name = "scenario", length = 100)
    private String scenario;
}
