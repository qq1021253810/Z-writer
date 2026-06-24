package com.zwriter.service;

import com.zwriter.context.*;
import com.zwriter.llm.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 上下文管理服务（L2 上下文管理器 + 滚动摘要 + 角色追踪 + Token 优化）
 */
@Slf4j
@Service
public class ContextService {

    @Autowired
    private LlmService llmService;

    /**
     * 获取小说完整上下文（兼容旧接口，委托给新上下文管理模块）
     * TODO: 后续由 WorkspaceManager 提供 novelId -> workspacePath 映射后完全迁移
     */
    public String getNovelContext(Long novelId) {
        log.debug("[ContextService] getNovelContext novelId={} (兼容接口，返回占位上下文)", novelId);
        return "【小说信息】\n novelId: " + novelId + "\n（上下文管理模块已升级，待 WorkspaceManager 提供映射后完全迁移）\n";
    }

    /**
     * 获取章节上下文 (兼容旧接口)
     */
    public String getChapterContext(Long novelId, Integer volumeNumber, Integer chapterNumber) {
        return getNovelContext(novelId);
    }

    /**
     * 获取章节上下文（带向量检索）(兼容旧接口)
     */
    public String getChapterContextWithVectorSearch(Long novelId, Integer volumeNumber,
                                                     Integer chapterNumber, String query) {
        return getNovelContext(novelId);
    }

    /**
     * 存储章节内容到向量库 (兼容旧接口)
     */
    public void storeChapterToVector(Long novelId, Integer chapterNumber, String content) {
        log.debug("[ContextService] storeChapterToVector 兼容调用 novelId={}, chapter={}", novelId, chapterNumber);
    }

    /**
     * 创建上下文管理器
     */
    public ContextManager createContextManager(Path workspacePath) {
        return new ContextManager(workspacePath, 20); // 最多 20 轮
    }

    /**
     * 创建滚动摘要
     */
    public RollingSummary createRollingSummary(Path workspacePath) {
        return new RollingSummary(workspacePath, llmService, 8000); // 8000 token 阈值
    }

    /**
     * 创建角色追踪器
     */
    public CharacterTracker createCharacterTracker(Path workspacePath) {
        return new CharacterTracker(workspacePath);
    }

    /**
     * 创建 Token 优化器
     */
    public TokenOptimizer createTokenOptimizer() {
        return new TokenOptimizer(32000, 4096); // 最大 32K，预留 4K 给回复
    }

    /**
     * 构建完整的小说上下文（结合 RAG + Context 管理）
     */
    public String buildFullContext(Path workspacePath, String query) throws IOException {
        ContextManager ctxManager = createContextManager(workspacePath);
        RollingSummary summary = createRollingSummary(workspacePath);
        CharacterTracker tracker = createCharacterTracker(workspacePath);
        TokenOptimizer optimizer = createTokenOptimizer();

        StringBuilder context = new StringBuilder();

        // 1. 角色状态
        String charCtx = tracker.buildCharacterContext();
        if (!charCtx.isEmpty()) {
            context.append(charCtx).append("\n");
        }

        // 2. 对话历史（带滚动摘要）
        String dialogueCtx = summary.buildContext(ctxManager);
        if (!dialogueCtx.isEmpty()) {
            context.append(dialogueCtx).append("\n");
        }

        // 3. Token 优化
        String optimized = optimizer.trimContext(context.toString(), 16000);
        
        return optimized;
    }
}
