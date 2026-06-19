package com.zwriter.repository;

import com.zwriter.entity.VolumeOutline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VolumeOutlineRepository extends JpaRepository<VolumeOutline, Long> {
    List<VolumeOutline> findByNovelIdOrderByVolumeNumber(Long novelId);
    Optional<VolumeOutline> findByNovelIdAndVolumeNumber(Long novelId, Integer volumeNumber);
}
