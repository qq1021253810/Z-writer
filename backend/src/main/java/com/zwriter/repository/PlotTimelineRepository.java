package com.zwriter.repository;

import com.zwriter.entity.PlotTimeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlotTimelineRepository extends JpaRepository<PlotTimeline, Long> {
    List<PlotTimeline> findByNovelIdOrderByEventTime(Long novelId);
}
