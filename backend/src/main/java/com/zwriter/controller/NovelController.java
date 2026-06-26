package com.zwriter.controller;

import com.zwriter.common.ApiResponse;
import com.zwriter.workspace.Workspace;
import com.zwriter.workspace.WorkspaceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 小说管理 API
 * - GET /api/novels → 获取小说列表
 * - POST /api/novels → 创建小说
 * - GET /api/novels/{name} → 获取小说信息
 * - DELETE /api/novels/{name} → 删除小说
 * - GET /api/novels/{name}/chapters → 获取章节列表
 * - GET /api/novels/{name}/chapters/{chapterNum} → 获取章节内容
 */
@Slf4j
@RestController
@RequestMapping("/api/novels")
public class NovelController {

    @Autowired
    private WorkspaceManager workspaceManager;

    /**
     * 列出所有小说
     */
    @GetMapping
    public ApiResponse<List<String>> listNovels() {
        try {
            List<String> novels = workspaceManager.listNovels();
            return ApiResponse.success(novels);
        } catch (IOException e) {
            log.error("[NovelController] 列出小说失败", e);
            return ApiResponse.failure("列出小说失败: " + e.getMessage());
        }
    }

    /**
     * 获取小说信息
     */
    @GetMapping("/{name}")
    public ApiResponse<Map<String, Object>> getNovel(@PathVariable String name) {
        try {
            Workspace ws = workspaceManager.openNovel(name);
            Map<String, Object> info = Map.of(
                    "name", ws.getName(),
                    "root", ws.getRoot().toString(),
                    "novelMd", ws.readNovelMd(),
                    "worldviewMd", ws.readWorldviewMd(),
                    "outlineMd", ws.readOutlineMd()
            );
            return ApiResponse.success(info);
        } catch (IOException e) {
            log.error("[NovelController] 获取小说信息失败: {}", name, e);
            return ApiResponse.failure("小说不存在: " + name);
        }
    }

    /**
     * 创建小说（返回 sessionId，进入对话流程）
     */
    @PostMapping
    public ApiResponse<Map<String, Object>> createNovel(@RequestBody CreateNovelRequest request) {
        try {
            Workspace ws = workspaceManager.createNovel(request.getName());
            Map<String, Object> result = Map.of(
                    "name", ws.getName(),
                    "root", ws.getRoot().toString(),
                    "message", "工作区已创建，请使用 /api/sessions 开始对话"
            );
            return ApiResponse.success(result);
        } catch (IOException e) {
            log.error("[NovelController] 创建小说失败: {}", request.getName(), e);
            return ApiResponse.failure("创建小说失败: " + e.getMessage());
        }
    }

    /**
     * 删除小说
     */
    @DeleteMapping("/{name}")
    public ApiResponse<Void> deleteNovel(@PathVariable String name) {
        try {
            workspaceManager.deleteNovel(name);
            return ApiResponse.success(null);
        } catch (IOException e) {
            log.error("[NovelController] 删除小说失败: {}", name, e);
            return ApiResponse.failure("删除小说失败: " + e.getMessage());
        }
    }

    /**
     * 获取章节列表
     */
    @GetMapping("/{name}/chapters")
    public ApiResponse<List<Map<String, Object>>> listChapters(@PathVariable String name) {
        try {
            Workspace ws = workspaceManager.openNovel(name);
            List<Map<String, Object>> chapters = new ArrayList<>();

            // 扫描 chapters 目录
            java.nio.file.Path chaptersDir = ws.getRoot().resolve("chapters");
            if (Files.exists(chaptersDir) && Files.isDirectory(chaptersDir)) {
                Pattern pattern = Pattern.compile("chapter-(\\d+)\\.md");
                try (var stream = Files.list(chaptersDir)) {
                    stream.filter(p -> pattern.matcher(p.getFileName().toString()).matches())
                          .sorted()
                          .forEach(p -> {
                              try {
                                  String content = Files.readString(p);
                                  String filename = p.getFileName().toString();
                                  Matcher matcher = pattern.matcher(filename);
                                  if (matcher.matches()) {
                                      int chapterNum = Integer.parseInt(matcher.group(1));
                                      // 提取标题（第一行 # 开头）
                                      String title = "";
                                      String[] lines = content.split("\n");
                                      if (lines.length > 0 && lines[0].startsWith("# ")) {
                                          title = lines[0].substring(2).trim();
                                      }
                                      Map<String, Object> chapterInfo = new HashMap<>();
                                      chapterInfo.put("chapterNumber", chapterNum);
                                      chapterInfo.put("title", title);
                                      chapterInfo.put("wordCount", content.length());
                                      chapters.add(chapterInfo);
                                  }
                              } catch (IOException e) {
                                  log.error("[NovelController] 读取章节失败: {}", p, e);
                              }
                          });
                }
            }

            return ApiResponse.success(chapters);
        } catch (IOException e) {
            log.error("[NovelController] 获取章节列表失败: {}", name, e);
            return ApiResponse.failure("小说不存在: " + name);
        }
    }

    /**
     * 获取章节内容
     */
    @GetMapping("/{name}/chapters/{chapterNum}")
    public ApiResponse<Map<String, Object>> getChapter(
            @PathVariable String name,
            @PathVariable int chapterNum) {
        try {
            Workspace ws = workspaceManager.openNovel(name);
            String content = ws.readChapter(chapterNum);

            if (content == null || content.isEmpty()) {
                return ApiResponse.failure("章节不存在: " + chapterNum);
            }

            // 提取标题
            String title = "";
            String[] lines = content.split("\n");
            if (lines.length > 0 && lines[0].startsWith("# ")) {
                title = lines[0].substring(2).trim();
            }

            Map<String, Object> chapterInfo = Map.of(
                    "chapterNumber", chapterNum,
                    "title", title,
                    "content", content,
                    "wordCount", content.length()
            );

            return ApiResponse.success(chapterInfo);
        } catch (IOException e) {
            log.error("[NovelController] 获取章节失败: {} chapter {}", name, chapterNum, e);
            return ApiResponse.failure("读取章节失败: " + e.getMessage());
        }
    }

    public static class CreateNovelRequest {
        private String name;
        private String genre;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getGenre() { return genre; }
        public void setGenre(String genre) { this.genre = genre; }
    }
}
