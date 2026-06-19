package com.zwriter.controller;

import com.zwriter.tool.MindMapExporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 思维导图导出控制器
 * 提供 Mermaid、Markdown、JSON 三种格式的思维导图导出接口
 */
@Slf4j
@RestController
@RequestMapping("/api/mindmap")
@RequiredArgsConstructor
public class MindMapController {

    private final MindMapExporter mindMapExporter;

    /**
     * 导出 Mermaid 格式思维导图
     */
    @GetMapping("/mermaid/{novelId}")
    public ResponseEntity<Map<String, Object>> exportMermaid(@PathVariable Long novelId) {
        log.info("导出 Mermaid 思维导图，novelId={}", novelId);
        String mermaid = mindMapExporter.exportToMermaid(novelId);

        Map<String, Object> response = new HashMap<>();
        if (mermaid.isEmpty()) {
            response.put("success", false);
            response.put("message", "小说不存在");
            return ResponseEntity.badRequest().body(response);
        }

        response.put("success", true);
        response.put("data", mermaid);
        return ResponseEntity.ok(response);
    }

    /**
     * 导出 Markdown 层级格式思维导图
     */
    @GetMapping("/markdown/{novelId}")
    public ResponseEntity<Map<String, Object>> exportMarkdown(@PathVariable Long novelId) {
        log.info("导出 Markdown 思维导图，novelId={}", novelId);
        String markdown = mindMapExporter.exportToMarkdown(novelId);

        Map<String, Object> response = new HashMap<>();
        if (markdown.isEmpty()) {
            response.put("success", false);
            response.put("message", "小说不存在");
            return ResponseEntity.badRequest().body(response);
        }

        response.put("success", true);
        response.put("data", markdown);
        return ResponseEntity.ok(response);
    }

    /**
     * 导出 JSON 树形结构思维导图
     */
    @GetMapping("/json/{novelId}")
    public ResponseEntity<Map<String, Object>> exportJson(@PathVariable Long novelId) {
        log.info("导出 JSON 思维导图，novelId={}", novelId);
        Map<String, Object> data = mindMapExporter.exportToJSON(novelId);

        Map<String, Object> response = new HashMap<>();
        if (data.isEmpty()) {
            response.put("success", false);
            response.put("message", "小说不存在");
            return ResponseEntity.badRequest().body(response);
        }

        response.put("success", true);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }
}
