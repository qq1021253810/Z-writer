package com.zwriter.repository;

import com.zwriter.entity.NovelInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NovelInfoRepository extends JpaRepository<NovelInfo, Long> {
    List<NovelInfo> findByStatus(String status);
    List<NovelInfo> findByGenre(String genre);
}
