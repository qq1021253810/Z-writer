package com.zwriter.workspace;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 小说工作区（与 CLI workspace.rs 对齐）
 */
@Slf4j
@Getter
public class Workspace {
    private final Path root;
    private final String name;

    private Workspace(Path root, String name) {
        this.root = root;
        this.name = name;
    }

    /**
     * 打开已有工作区
     */
    public static Workspace open(Path root) throws IOException {
        Path novelPath = root.resolve("novel.md");
        if (!Files.exists(novelPath)) {
            throw new IOException("工作区不存在: " + root);
        }
        String name = root.getFileName().toString();
        return new Workspace(root, name);
    }

    /**
     * 创建新工作区
     */
    public static Workspace create(Path basePath, String name) throws IOException {
        Path root = basePath.resolve(name);
        if (Files.exists(root)) {
            throw new IOException("小说 '" + name + "' 已存在");
        }

        // 创建目录结构
        Files.createDirectories(root.resolve("characters"));
        Files.createDirectories(root.resolve("chapters"));
        Files.createDirectories(root.resolve("wiki"));
        Files.createDirectories(root.resolve("volumes"));
        Files.createDirectories(root.resolve(".context"));
        Files.createDirectories(root.resolve("vector_store"));

        // 创建 novel.md
        String novelContent = "# " + name + "\n\n- 类型: ?\n- 状态: 初始化\n- 简介: ?\n";
        Files.writeString(root.resolve("novel.md"), novelContent);

        // 创建 worldview.md
        Files.writeString(root.resolve("worldview.md"), "# 世界观设定\n\n待补充\n");

        // 创建 outline.md
        Files.writeString(root.resolve("outline.md"), "# 大纲\n\n待补充\n");

        // 创建 memory_tree.json
        String memoryTree = """
            {
              "novel_id": "%s",
              "volumes": [],
              "foreshadows": []
            }
            """.formatted(name);
        Files.writeString(root.resolve("memory_tree.json"), memoryTree);

        // 创建 materials.json
        Files.writeString(root.resolve("vector_store/materials.json"), "[]");

        log.info("[Workspace] 创建工作区: {}", root);
        return new Workspace(root, name);
    }

    /**
     * 读取小说信息
     */
    public String readNovelMd() throws IOException {
        return Files.readString(root.resolve("novel.md"));
    }

    /**
     * 读取世界观
     */
    public String readWorldviewMd() throws IOException {
        Path path = root.resolve("worldview.md");
        if (!Files.exists(path)) return "";
        return Files.readString(path);
    }

    /**
     * 读取大纲
     */
    public String readOutlineMd() throws IOException {
        Path path = root.resolve("outline.md");
        if (!Files.exists(path)) return "";
        return Files.readString(path);
    }

    /**
     * 读取剧情设计
     */
    public String readPlotDesignMd() throws IOException {
        Path path = root.resolve("plot_design.md");
        if (!Files.exists(path)) return "";
        return Files.readString(path);
    }

    /**
     * 获取角色列表
     */
    public List<String> listCharacters() throws IOException {
        Path dir = root.resolve("characters");
        if (!Files.exists(dir)) return List.of();
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                .filter(p -> p.toString().endsWith(".md"))
                .map(p -> {
                    try { return Files.readString(p); }
                    catch (IOException e) { throw new RuntimeException(e); }
                })
                .collect(Collectors.toList());
        }
    }

    /**
     * 保存角色
     */
    public void saveCharacter(String characterName, String content) throws IOException {
        Path dir = root.resolve("characters");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(characterName + ".md"), content);
    }

    /**
     * 读取角色
     */
    public String readCharacter(String characterName) throws IOException {
        Path path = root.resolve("characters").resolve(characterName + ".md");
        if (!Files.exists(path)) return "";
        return Files.readString(path);
    }

    /**
     * 保存章节
     */
    public void saveChapter(int chapterNum, String content) throws IOException {
        Path dir = root.resolve("chapters");
        Files.createDirectories(dir);
        String filename = "chapter-%03d.md".formatted(chapterNum);
        Files.writeString(dir.resolve(filename), content);
    }

    /**
     * 读取章节
     */
    public String readChapter(int chapterNum) throws IOException {
        String filename = "chapter-%03d.md".formatted(chapterNum);
        Path path = root.resolve("chapters").resolve(filename);
        if (!Files.exists(path)) return "";
        return Files.readString(path);
    }

    /**
     * 获取下一章编号
     */
    public int getNextChapterNum() throws IOException {
        Path dir = root.resolve("chapters");
        if (!Files.exists(dir)) return 1;
        int maxNum = 0;
        try (Stream<Path> stream = Files.list(dir)) {
            maxNum = stream
                .filter(p -> p.getFileName().toString().matches("chapter-\\d+\\.md"))
                .mapToInt(p -> {
                    String name = p.getFileName().toString();
                    String num = name.replaceAll("chapter-(\\d+)\\.md", "$1");
                    return Integer.parseInt(num);
                })
                .max()
                .orElse(0);
        }
        return maxNum + 1;
    }

    /**
     * 获取最近 N 章内容
     */
    public List<ChapterInfo> getRecentChapters(int count) throws IOException {
        int next = getNextChapterNum();
        if (next <= 1) return List.of();
        int start = Math.max(1, next - count);
        List<ChapterInfo> chapters = new ArrayList<>();
        for (int i = start; i < next; i++) {
            String content = readChapter(i);
            if (!content.isEmpty()) {
                chapters.add(new ChapterInfo(i, content));
            }
        }
        return chapters;
    }

    /**
     * 构建完整小说上下文
     */
    public String buildFullContext(int recentChaptersCount) throws IOException {
        StringBuilder context = new StringBuilder();

        String novelInfo = readNovelMd();
        context.append("【小说信息】\n").append(novelInfo).append("\n\n");

        String worldview = readWorldviewMd();
        if (!worldview.isEmpty() && !worldview.contains("待补充")) {
            context.append("【世界观】\n").append(worldview).append("\n\n");
        }

        String outline = readOutlineMd();
        if (!outline.isEmpty() && !outline.contains("待补充")) {
            context.append("【大纲】\n").append(outline).append("\n\n");
        }

        List<String> characters = listCharacters();
        if (!characters.isEmpty()) {
            context.append("【角色设定】\n");
            for (String ch : characters) {
                context.append(ch).append("\n");
            }
            context.append("\n");
        }

        List<ChapterInfo> recent = getRecentChapters(recentChaptersCount);
        if (!recent.isEmpty()) {
            context.append("【最近章节】\n");
            for (ChapterInfo ch : recent) {
                context.append("第 ").append(ch.number()).append(" 章:\n")
                       .append(ch.content()).append("\n\n");
            }
        }

        return context.toString();
    }

    /**
     * 列出所有工作区
     */
    public static List<String> listAll(Path basePath) throws IOException {
        if (!Files.exists(basePath)) return List.of();
        try (Stream<Path> stream = Files.list(basePath)) {
            return stream
                .filter(Files::isDirectory)
                .filter(d -> Files.exists(d.resolve("novel.md")))
                .map(d -> d.getFileName().toString())
                .sorted()
                .collect(Collectors.toList());
        }
    }

    /**
     * 删除工作区
     */
    public void delete() throws IOException {
        deleteRecursively(root);
        log.info("[Workspace] 删除工作区: {}", root);
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> stream = Files.list(path)) {
                stream.forEach(p -> {
                    try { deleteRecursively(p); }
                    catch (IOException e) { throw new RuntimeException(e); }
                });
            }
        }
        Files.delete(path);
    }

    public record ChapterInfo(int number, String content) {}
}
