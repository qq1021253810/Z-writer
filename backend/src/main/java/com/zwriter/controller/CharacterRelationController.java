package com.zwriter.controller;

import com.zwriter.tool.CharacterRelationTool;
import com.zwriter.tool.CharacterRelationTool.RelationGraph;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 角色关系图控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/character-relation")
@RequiredArgsConstructor
public class CharacterRelationController {
    
    private final CharacterRelationTool characterRelationTool;
    
    /**
     * 获取角色关系图数据
     */
    @GetMapping("/graph/{novelId}")
    public ResponseEntity<Map<String, Object>> getRelationGraph(@PathVariable Long novelId) {
        RelationGraph graph = characterRelationTool.generateRelationGraph(novelId);
        
        Map<String, Object> data = new HashMap<>();
        data.put("novelId", novelId);
        data.put("nodes", graph.nodes());
        data.put("edges", graph.edges());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取 Mermaid 格式的关系图
     */
    @GetMapping("/mermaid/{novelId}")
    public ResponseEntity<Map<String, Object>> getMermaidGraph(@PathVariable Long novelId) {
        String mermaidCode = characterRelationTool.generateMermaidGraph(novelId);
        
        Map<String, Object> data = new HashMap<>();
        data.put("novelId", novelId);
        data.put("mermaidCode", mermaidCode);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        
        return ResponseEntity.ok(response);
    }
}
