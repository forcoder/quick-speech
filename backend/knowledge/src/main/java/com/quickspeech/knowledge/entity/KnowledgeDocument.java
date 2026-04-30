package com.quickspeech.knowledge.entity;

import com.quickspeech.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "knowledge_document")
public class KnowledgeDocument extends BaseEntity {

    @Column(name = "knowledge_base_id", nullable = false)
    private Long knowledgeBaseId;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "file_type", length = 50)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_path", length = 1000)
    private String filePath;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "chunk_count")
    private Integer chunkCount = 0;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "chunk_strategy", length = 50)
    private String chunkStrategy = "PARAGRAPH";

    @Column(name = "chunk_size")
    private Integer chunkSize = 500;

    @Column(name = "chunk_overlap")
    private Integer chunkOverlap = 50;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;
}
