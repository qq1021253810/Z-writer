package com.zwriter.service;

import com.zwriter.entity.Foreshadow;
import com.zwriter.repository.ForeshadowRepository;
import com.zwriter.repository.CharacterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 伏笔管理服务
 * 负责伏笔的增删改查、回收、冲突检测与超期检查
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ForeshadowService {

    private final ForeshadowRepository foreshadowRepository;
    private final CharacterRepository characterRepository;

    /**
     * 添加伏笔
     */
    @Transactional
    public Foreshadow addForeshadow(Foreshadow foreshadow) {
        foreshadow.setStatus("planted");
        Foreshadow saved = foreshadowRepository.save(foreshadow);
        log.info("[伏笔服务] 添加伏笔: novelId={}, setupChapter={}, id={}",
                saved.getNovelId(), saved.getSetupChapter(), saved.getId());
        return saved;
    }

    /**
     * 获取小说所有伏笔
     */
    public List<Foreshadow> getForeshadowsByNovel(Long novelId) {
        return foreshadowRepository.findByNovelId(novelId);
    }

    /**
     * 获取未回收的伏笔（status=planted）
     */
    public List<Foreshadow> getPlantedForeshadows(Long novelId) {
        return foreshadowRepository.findByNovelIdAndStatus(novelId, "planted");
    }

    /**
     * 获取已回收的伏笔（status=resolved）
     */
    public List<Foreshadow> getResolvedForeshadows(Long novelId) {
        return foreshadowRepository.findByNovelIdAndStatus(novelId, "resolved");
    }

    /**
     * 回收伏笔
     */
    @Transactional
    public Foreshadow resolveForeshadow(Long id, Integer payoffChapter) {
        Foreshadow foreshadow = foreshadowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("伏笔不存在: " + id));
        foreshadow.setStatus("resolved");
        foreshadow.setPayoffChapter(payoffChapter);
        Foreshadow resolved = foreshadowRepository.save(foreshadow);
        log.info("[伏笔服务] 回收伏笔: id={}, payoffChapter={}", id, payoffChapter);
        return resolved;
    }

    /**
     * 检测伏笔冲突
     * 包括：超过预期回收章节未回收、关联角色已删除等
     */
    public List<String> detectConflicts(Long novelId) {
        List<String> conflicts = new ArrayList<>();
        List<Foreshadow> plantedForeshadows = getPlantedForeshadows(novelId);

        // 获取该小说下所有现存角色ID
        List<Long> existingCharacterIds = characterRepository.findByNovelId(novelId)
                .stream()
                .map(com.zwriter.entity.Character::getId)
                .collect(Collectors.toList());

        for (Foreshadow foreshadow : plantedForeshadows) {
            // 检查关联角色是否已被删除
            if (foreshadow.getRelatedCharacters() != null && !foreshadow.getRelatedCharacters().isEmpty()) {
                List<Long> missingCharacters = foreshadow.getRelatedCharacters().stream()
                        .filter(charId -> !existingCharacterIds.contains(charId))
                        .collect(Collectors.toList());
                if (!missingCharacters.isEmpty()) {
                    conflicts.add(String.format("伏笔[id=%d]「%s」关联角色已删除: %s",
                            foreshadow.getId(),
                            foreshadow.getClueDescription(),
                            missingCharacters));
                }
            }

            // 检查已设定回收章节但尚未回收的伏笔（payoffChapter已设置且当前状态仍为planted）
            if (foreshadow.getPayoffChapter() != null) {
                conflicts.add(String.format("伏笔[id=%d]「%s」已设定回收章节(%d)但尚未回收",
                        foreshadow.getId(),
                        foreshadow.getClueDescription(),
                        foreshadow.getPayoffChapter()));
            }
        }

        log.info("[伏笔服务] 冲突检测: novelId={}, 冲突数={}", novelId, conflicts.size());
        return conflicts;
    }

    /**
     * 获取超期未回收的伏笔
     * 即 setupChapter 距离当前章节超过一定范围且仍为 planted 状态的伏笔
     */
    public List<Foreshadow> getOverdueForeshadows(Long novelId, Integer currentChapter) {
        List<Foreshadow> plantedForeshadows = getPlantedForeshadows(novelId);
        return plantedForeshadows.stream()
                .filter(f -> {
                    // 如果设定了回收章节且已超过，则视为超期
                    if (f.getPayoffChapter() != null && currentChapter > f.getPayoffChapter()) {
                        return true;
                    }
                    // 如果未设定回收章节，但埋设章节距当前章节差距过大（超过50章），视为超期
                    if (f.getPayoffChapter() == null && (currentChapter - f.getSetupChapter()) > 50) {
                        return true;
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }
}
