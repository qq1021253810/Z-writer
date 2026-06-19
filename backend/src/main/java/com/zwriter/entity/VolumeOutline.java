package com.zwriter.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "volume_outline")
public class VolumeOutline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "novel_id", nullable = false)
    private Long novelId;

    @Column(name = "volume_number", nullable = false)
    private Integer volumeNumber;

    @Column
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "core_conflict", columnDefinition = "TEXT")
    private String coreConflict;

    @Column(name = "chapter_count")
    private Integer chapterCount = 0;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
