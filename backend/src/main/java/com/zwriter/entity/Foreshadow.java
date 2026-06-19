package com.zwriter.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "foreshadow_lib")
public class Foreshadow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "novel_id", nullable = false)
    private Long novelId;

    @Column(name = "setup_chapter", nullable = false)
    private Integer setupChapter;

    @Column(name = "payoff_chapter")
    private Integer payoffChapter;

    @Column(name = "clue_description", columnDefinition = "TEXT", nullable = false)
    private String clueDescription;

    @Column
    private String status = "planted";

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "related_characters", columnDefinition = "BIGINT[]")
    private List<Long> relatedCharacters;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
