package com.zwriter.controller;

import com.zwriter.entity.Character;
import com.zwriter.repository.CharacterRepository;
import com.zwriter.tool.CharacterConsistencyChecker;
import com.zwriter.tool.CharacterConsistencyChecker.ConsistencyCheckResult;
import com.zwriter.tool.CharacterConsistencyChecker.ConsistencyViolation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 人设一致性校验控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/consistency")
@RequiredArgsConstructor
public class ConsistencyController {

    private final CharacterConsistencyChecker consistencyChecker;
    private final CharacterRepository characterRepository;

    /**
     * 校验文本人设一致性
     * 请求体：{"novelId": 1, "text": "..."}
     */
    @PostMapping("/check")
    public ResponseEntity<Map<String, Object>> check(@RequestBody Map<String, Object> request) {
        Long novelId = ((Number) request.get("novelId")).longValue();
        String text = (String) request.get("text");

        ConsistencyCheckResult result = consistencyChecker.checkConsistency(novelId, text);

        Map<String, Object> data = new HashMap<>();
        data.put("isConsistent", result.isConsistent());
        data.put("violations", result.violations().stream().map(v -> {
            Map<String, String> violationMap = new HashMap<>();
            violationMap.put("type", v.type());
            violationMap.put("characterName", v.characterName());
            violationMap.put("description", v.description());
            violationMap.put("suggestion", v.suggestion());
            return violationMap;
        }).toList());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取角色人设档案
     */
    @GetMapping("/character/{characterId}/profile")
    public ResponseEntity<Map<String, Object>> getCharacterProfile(@PathVariable Long characterId) {
        Optional<Character> characterOpt = characterRepository.findById(characterId);

        if (characterOpt.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("data", "角色不存在");
            return ResponseEntity.ok(response);
        }

        Character character = characterOpt.get();

        Map<String, Object> data = new HashMap<>();
        data.put("id", character.getId());
        data.put("novelId", character.getNovelId());
        data.put("name", character.getName());
        data.put("roleType", character.getRoleType());
        data.put("basicInfo", character.getBasicInfo());
        data.put("coreTraits", character.getCoreTraits());
        data.put("abilities", character.getAbilities());
        data.put("relationships", character.getRelationships());
        data.put("catchphrases", character.getCatchphrases());
        data.put("growthCurve", character.getGrowthCurve());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }
}
