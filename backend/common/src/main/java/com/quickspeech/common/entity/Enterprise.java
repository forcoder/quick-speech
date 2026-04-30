package com.quickspeech.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sys_enterprise")
public class Enterprise extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "code", unique = true, length = 100)
    private String code;

    @Column(name = "contact_name", length = 100)
    private String contactName;

    @Column(name = "contact_email", length = 200)
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "max_users")
    private Integer maxUsers = 10;

    @Column(name = "max_knowledge_bases")
    private Integer maxKnowledgeBases = 5;

    @Column(name = "max_agents")
    private Integer maxAgents = 5;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "expire_at")
    private java.time.LocalDateTime expireAt;
}
