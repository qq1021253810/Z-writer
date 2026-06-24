package com.zwriter.workspace;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 工作区管理器（Spring 组件）
 */
@Slf4j
@Component
public class WorkspaceManager {

    @Value("${zwriter.workspace.base-path:./workspaces}")
    private String basePathStr;

    private Path basePath;

    @PostConstruct
    public void init() throws IOException {
        basePath = Path.of(basePathStr);
        Files.createDirectories(basePath);
        log.info("[WorkspaceManager] 工作区根目录: {}", basePath.toAbsolutePath());
    }

    /**
     * 创建小说工作区
     */
    public Workspace createNovel(String name) throws IOException {
        return Workspace.create(basePath, name);
    }

    /**
     * 打开小说工作区
     */
    public Workspace openNovel(String name) throws IOException {
        return Workspace.open(basePath.resolve(name));
    }

    /**
     * 列出所有小说
     */
    public List<String> listNovels() throws IOException {
        return Workspace.listAll(basePath);
    }

    /**
     * 删除小说
     */
    public void deleteNovel(String name) throws IOException {
        Workspace ws = openNovel(name);
        ws.delete();
    }

    /**
     * 获取工作区根目录
     */
    public Path getBasePath() {
        return basePath;
    }
}
