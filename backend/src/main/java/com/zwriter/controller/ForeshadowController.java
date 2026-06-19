package com.zwriter.controller;

import com.zwriter.entity.Foreshadow;
import com.zwriter.service.ForeshadowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 伏笔管理控制器
 * 提供伏笔的增删改查、回收、冲突检测与超期检查等接口
 */
@RestController
@RequestMapping("/api/foreshadow")
@RequiredArgsConstructor
public class ForeshadowController {

    private final ForeshadowService foreshadowService;

    /**
     * 添加伏笔
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> addForeshadow(@RequestBody Foreshadow foreshadow) {
        Foreshadow saved = foreshadowService.addForeshadow(foreshadow);
        return ResponseEntity.ok(Map.of("success", true, "data", saved));
    }

    /**
     * 获取小说所有伏笔
     */
    @GetMapping("/novel/{novelId}")
    public ResponseEntity<Map<String, Object>> getForeshadowsByNovel(@PathVariable Long novelId) {
        List<Foreshadow> list = foreshadowService.getForeshadowsByNovel(novelId);
        return ResponseEntity.ok(Map.of("success", true, "data", list));
    }

    /**
     * 获取未回收伏笔
     */
    @GetMapping("/novel/{novelId}/planted")
    public ResponseEntity<Map<String, Object>> getPlantedForeshadows(@PathVariable Long novelId) {
        List<Foreshadow> list = foreshadowService.getPlantedForeshadows(novelId);
        return ResponseEntity.ok(Map.of("success", true, "data", list));
    }

    /**
     * 获取已回收伏笔
     */
    @GetMapping("/novel/{novelId}/resolved")
    public ResponseEntity<Map<String, Object>> getResolvedForeshadows(@PathVariable Long novelId) {
        List<Foreshadow> list = foreshadowService.getResolvedForeshadows(novelId);
        return ResponseEntity.ok(Map.of("success", true, "data", list));
    }

    /**
     * 回收伏笔
     */
    @PutMapping("/{id}/resolve")
    public ResponseEntity<Map<String, Object>> resolveForeshadow(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body) {
        Integer payoffChapter = body.get("payoffChapter");
        Foreshadow resolved = foreshadowService.resolveForeshadow(id, payoffChapter);
        return ResponseEntity.ok(Map.of("success", true, "data", resolved));
    }

    /**
     * 检测伏笔冲突
     */
    @GetMapping("/novel/{novelId}/conflicts")
    public ResponseEntity<Map<String, Object>> detectConflicts(@PathVariable Long novelId) {
        List<String> conflicts = foreshadowService.detectConflicts(novelId);
        return ResponseEntity.ok(Map.of("success", true, "data", conflicts));
    }

    /**
     * 获取超期未回收的伏笔
     */
    @GetMapping("/novel/{novelId}/overdue")
    public ResponseEntity<Map<String, Object>> getOverdueForeshadows(
            @PathVariable Long novelId,
            @RequestParam Integer currentChapter) {
        List<Foreshadow> list = foreshadowService.getOverdueForeshadows(novelId, currentChapter);
        return ResponseEntity.ok(Map.of("success", true, "data", list));
    }
}
