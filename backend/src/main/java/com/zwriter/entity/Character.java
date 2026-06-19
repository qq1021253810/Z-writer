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
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "character_table")
public class Character {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "novel_id", nullable = false)
    private Long novelId;

    @Column(nullable = false)
    private String name;

    @Column(name = "role_type", nullable = false)
    private String roleType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "basic_info", columnDefinition = "jsonb")
    private Map<String, Object> basicInfo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "core_traits", columnDefinition = "jsonb")
    private Map<String, Object> coreTraits;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> abilities;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> relationships;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "catchphrases", columnDefinition = "TEXT[]")
    private List<String> catchphrases;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "growth_curve", columnDefinition = "jsonb")
    private Map<String, Object> growthCurve;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
