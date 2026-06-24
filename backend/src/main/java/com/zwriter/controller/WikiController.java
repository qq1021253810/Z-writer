package com.zwriter.controller;

import com.zwriter.common.ApiResponse;
import com.zwriter.wiki.WikiService;
import com.zwriter.workspace.WorkspaceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Wiki 管理 API
 * - GET /api/wiki/genres → 列出类型
 * - GET /api/wiki/genres/{genre} → 获取类型规则
 * - POST /api/wiki/custom → 添加自定义规则
 * - DELETE /api/wiki/custom/{novelName}/{genre} → 删除自定义规则
 */
@Slf4j
@RestController
@RequestMapping("/api/wiki")
public class WikiController {

    @Autowired
    private WikiService wikiService;

    @Autowired
    private WorkspaceManager workspaceManager;

    /**
     * 列出可用类型
     */
    @GetMapping("/genres")
    public ApiResponse<List<String>> listGenres() {
        try {
            return ApiResponse.success(wikiService.listGenres());
        } catch (IOException e) {
            log.error("[WikiController] 列出类型失败", e);
            return ApiResponse.failure("列出类型失败: " + e.getMessage());
        }
    }

    /**
     * 获取类型规则
     */
    @GetMapping("/genres/{genre}")
    public ApiResponse<String> getGenreRule(@PathVariable String genre) {
        try {
            String rule = wikiService.getGenreRule(genre);
            if (rule.isEmpty()) {
                return ApiResponse.failure("类型规则不存在: " + genre);
            }
            return ApiResponse.success(rule);
        } catch (IOException e) {
            log.error("[WikiController] 获取类型规则失败: {}", genre, e);
            return ApiResponse.failure("获取类型规则失败: " + e.getMessage());
        }
    }

    /**
     * 添加自定义规则
     */
    @PostMapping("/custom")
    public ApiResponse<Void> saveCustomRule(@RequestBody CustomRuleRequest request) {
        try {
            Path workspacePath = workspaceManager.openNovel(request.getNovelName()).getRoot();
            wikiService.saveCustomRule(workspacePath, request.getGenre(), request.getContent());
            return ApiResponse.success(null);
        } catch (IOException e) {
            log.error("[WikiController] 保存自定义规则失败", e);
            return ApiResponse.failure("保存自定义规则失败: " + e.getMessage());
        }
    }

    /**
     * 获取自定义规则
     */
    @GetMapping("/custom/{novelName}/{genre}")
    public ApiResponse<String> getCustomRule(@PathVariable String novelName,
                                              @PathVariable String genre) {
        try {
            Path workspacePath = workspaceManager.openNovel(novelName).getRoot();
            String rule = wikiService.getCustomRule(workspacePath, genre);
            if (rule.isEmpty()) {
                return ApiResponse.failure("自定义规则不存在: " + genre);
            }
            return ApiResponse.success(rule);
        } catch (IOException e) {
            log.error("[WikiController] 获取自定义规则失败", e);
            return ApiResponse.failure("获取自定义规则失败: " + e.getMessage());
        }
    }

    /**
     * 删除自定义规则
     */
    @DeleteMapping("/custom/{novelName}/{genre}")
    public ApiResponse<Void> deleteCustomRule(@PathVariable String novelName,
                                               @PathVariable String genre) {
        try {
            Path workspacePath = workspaceManager.openNovel(novelName).getRoot();
            wikiService.deleteCustomRule(workspacePath, genre);
            return ApiResponse.success(null);
        } catch (IOException e) {
            log.error("[WikiController] 删除自定义规则失败", e);
            return ApiResponse.failure("删除自定义规则失败: " + e.getMessage());
        }
    }

    public static class CustomRuleRequest {
        private String novelName;
        private String genre;
        private String content;

        public String getNovelName() { return novelName; }
        public void setNovelName(String novelName) { this.novelName = novelName; }
        public String getGenre() { return genre; }
        public void setGenre(String genre) { this.genre = genre; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}
