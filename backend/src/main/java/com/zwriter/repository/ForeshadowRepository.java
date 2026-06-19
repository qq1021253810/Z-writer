package com.zwriter.repository;

import com.zwriter.entity.Foreshadow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForeshadowRepository extends JpaRepository<Foreshadow, Long> {
    List<Foreshadow> findByNovelId(Long novelId);
    List<Foreshadow> findByNovelIdAndStatus(Long novelId, String status);
}
