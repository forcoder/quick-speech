package com.quickspeech.knowledge.entity;

import com.quickspeech.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "knowledge_base")
public class KnowledgeBase extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "document_count")
    private Integer documentCount = 0;

    @Column(name = "total_chunks")
    private Integer totalChunks = 0;

    @Column(name = "embedding_model", length = 100)
    private String embeddingModel = "text-embedding-ada-002";

    @Column(name = "vector_db_type", length = 50)
    private String vectorDbType = "chroma";

    @Column(name = "vector_collection", length = 200)
    private String vectorCollection;
}
