package com.quickspeech.agent.entity;

import com.quickspeech.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "prompt_template")
public class PromptTemplate extends BaseEntity {

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "template_content", nullable = false, columnDefinition = "TEXT")
    private String templateContent;

    @Column(name = "variables", length = 2000)
    private String variables;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "description", length = 1000)
    private String description;
}
