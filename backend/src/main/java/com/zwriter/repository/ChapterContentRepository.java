package com.zwriter.repository;

import com.zwriter.entity.ChapterContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterContentRepository extends JpaRepository<ChapterContent, Long> {
    List<ChapterContent> findByNovelId(Long novelId);
    List<ChapterContent> findByNovelIdOrderByVolumeNumberAscChapterNumberAsc(Long novelId);
    Optional<ChapterContent> findByNovelIdAndVolumeNumberAndChapterNumber(Long novelId, Integer volumeNumber, Integer chapterNumber);
    List<ChapterContent> findByNovelIdAndVolumeNumberOrderByChapterNumberAsc(Long novelId, Integer volumeNumber);
}
