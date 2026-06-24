package com.zwriter.wiki;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Wiki 服务（系统内置 + 用户自定义）
 */
@Slf4j
@Component
public class WikiService {

    @Value("${zwriter.wiki.builtin-path:./wiki}")
    private String builtinPathStr;

    private Path builtinPath;

    @PostConstruct
    public void init() throws IOException {
        builtinPath = Path.of(builtinPathStr).toAbsolutePath();
        if (!Files.exists(builtinPath)) {
            log.warn("[WikiService] 内置 Wiki 目录不存在: {}", builtinPath);
        }
        log.info("[WikiService] 内置 Wiki 目录: {}", builtinPath);
    }

    /**
     * 获取类型规则
     */
    public String getGenreRule(String genre) throws IOException {
        Path path = builtinPath.resolve("genres").resolve(genre + ".md");
        if (Files.exists(path)) {
            return Files.readString(path);
        }
        log.warn("[WikiService] 类型规则不存在: {}", genre);
        return "";
    }

    /**
     * 列出可用类型
     */
    public List<String> listGenres() throws IOException {
        Path genresDir = builtinPath.resolve("genres");
        if (!Files.exists(genresDir)) return List.of();
        try (Stream<Path> stream = Files.list(genresDir)) {
            return stream
                .filter(p -> p.toString().endsWith(".md"))
                .map(p -> p.getFileName().toString().replace(".md", ""))
                .sorted()
                .collect(Collectors.toList());
        }
    }

    /**
     * 获取写作规范
     */
    public String getRule(String ruleName) throws IOException {
        Path path = builtinPath.resolve("rules").resolve(ruleName + ".md");
        if (Files.exists(path)) {
            return Files.readString(path);
        }
        return "";
    }

    /**
     * 获取模板
     */
    public String getTemplate(String templateName) throws IOException {
        Path path = builtinPath.resolve("templates").resolve(templateName + ".md");
        if (Files.exists(path)) {
            return Files.readString(path);
        }
        return "";
    }

    /**
     * 获取示例
     */
    public String getExample(String exampleName) throws IOException {
        Path path = builtinPath.resolve("examples").resolve(exampleName + ".md");
        if (Files.exists(path)) {
            return Files.readString(path);
        }
        return "";
    }

    /**
     * 获取用户自定义规则（从工作区 wiki 目录）
     */
    public String getCustomRule(Path workspacePath, String genre) throws IOException {
        Path path = workspacePath.resolve("wiki").resolve(genre + ".md");
        if (Files.exists(path)) {
            return Files.readString(path);
        }
        return "";
    }

    /**
     * 保存用户自定义规则
     */
    public void saveCustomRule(Path workspacePath, String genre, String content) throws IOException {
        Path wikiDir = workspacePath.resolve("wiki");
        Files.createDirectories(wikiDir);
        Files.writeString(wikiDir.resolve(genre + ".md"), content);
        log.info("[WikiService] 保存自定义规则: {}/wiki/{}.md", workspacePath.getFileName(), genre);
    }

    /**
     * 删除用户自定义规则
     */
    public void deleteCustomRule(Path workspacePath, String genre) throws IOException {
        Path path = workspacePath.resolve("wiki").resolve(genre + ".md");
        if (Files.exists(path)) {
            Files.delete(path);
            log.info("[WikiService] 删除自定义规则: {}", genre);
        }
    }

    /**
     * 构建 Genre 的完整 Prompt（内置 + 自定义）
     */
    public String buildGenrePrompt(String genre) throws IOException {
        StringBuilder prompt = new StringBuilder();
        
        // 内置规则
        String builtin = getGenreRule(genre);
        if (!builtin.isEmpty()) {
            prompt.append("【类型规则】\n").append(builtin).append("\n\n");
        }
        
        return prompt.toString();
    }
}
