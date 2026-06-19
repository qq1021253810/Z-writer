package com.zwriter.controller;

import com.zwriter.service.SettingSyncService;
import com.zwriter.service.SettingSyncService.SyncResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 设定修改同步控制器
 * 修改人设/世界观后自动同步更新全库
 */
@Slf4j
@RestController
@RequestMapping("/api/setting-sync")
@RequiredArgsConstructor
public class SettingSyncController {

    private final SettingSyncService settingSyncService;

    /**
     * 同步角色修改
     * 请求体：{"novelId": 1, "characterId": 1, "oldName": "旧名", "newName": "新名"}
     */
    @PostMapping("/character")
    public ResponseEntity<Map<String, Object>> syncCharacter(@RequestBody Map<String, Object> request) {
        Long novelId = ((Number) request.get("novelId")).longValue();
        Long characterId = ((Number) request.get("characterId")).longValue();
        String oldName = (String) request.get("oldName");
        String newName = (String) request.get("newName");

        SyncResult result = settingSyncService.syncCharacterUpdate(novelId, characterId, oldName, newName);

        Map<String, Object> data = new HashMap<>();
        data.put("updatedForeshadows", result.updatedForeshadows());
        data.put("updatedChapters", result.updatedChapters());
        data.put("updatedVectorDocs", result.updatedVectorDocs());
        data.put("syncDetails", result.syncDetails());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    /**
     * 同步世界观修改
     * 请求体：{"novelId": 1, "field": "worldName", "oldValue": "旧世界名", "newValue": "新世界名"}
     */
    @PostMapping("/worldview")
    public ResponseEntity<Map<String, Object>> syncWorldview(@RequestBody Map<String, Object> request) {
        Long novelId = ((Number) request.get("novelId")).longValue();
        String field = (String) request.get("field");
        String oldValue = (String) request.get("oldValue");
        String newValue = (String) request.get("newValue");

        SyncResult result = settingSyncService.syncWorldviewUpdate(novelId, field, oldValue, newValue);

        Map<String, Object> data = new HashMap<>();
        data.put("updatedForeshadows", result.updatedForeshadows());
        data.put("updatedChapters", result.updatedChapters());
        data.put("updatedVectorDocs", result.updatedVectorDocs());
        data.put("syncDetails", result.syncDetails());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    /**
     * 同步战力等级修改
     * 请求体：{"novelId": 1, "characterId": 1, "oldLevel": "筑基", "newLevel": "金丹"}
     */
    @PostMapping("/power-level")
    public ResponseEntity<Map<String, Object>> syncPowerLevel(@RequestBody Map<String, Object> request) {
        Long novelId = ((Number) request.get("novelId")).longValue();
        Long characterId = ((Number) request.get("characterId")).longValue();
        String oldLevel = (String) request.get("oldLevel");
        String newLevel = (String) request.get("newLevel");

        SyncResult result = settingSyncService.syncPowerLevelUpdate(novelId, characterId, oldLevel, newLevel);

        Map<String, Object> data = new HashMap<>();
        data.put("updatedForeshadows", result.updatedForeshadows());
        data.put("updatedChapters", result.updatedChapters());
        data.put("updatedVectorDocs", result.updatedVectorDocs());
        data.put("syncDetails", result.syncDetails());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取受影响的章节列表
     * GET /api/setting-sync/affected-chapters?novelId=1&keyword=xxx
     */
    @GetMapping("/affected-chapters")
    public ResponseEntity<Map<String, Object>> getAffectedChapters(
            @RequestParam Long novelId,
            @RequestParam String keyword) {

        List<Map<String, Object>> affectedChapters = settingSyncService.getAffectedChapters(novelId, keyword);

        Map<String, Object> data = new HashMap<>();
        data.put("novelId", novelId);
        data.put("keyword", keyword);
        data.put("affectedCount", affectedChapters.size());
        data.put("chapters", affectedChapters);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }
}
