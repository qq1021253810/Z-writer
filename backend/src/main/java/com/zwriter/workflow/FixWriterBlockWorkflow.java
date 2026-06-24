package com.zwriter.workflow;

import com.zwriter.agent.base.AgentContext;
import com.zwriter.agent.base.AgentResult;
import com.zwriter.agent.plot.PlotAgent;
import com.zwriter.workspace.Workspace;
import com.zwriter.workspace.WorkspaceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * 卡文修复工作流（与 CLI fix-writer-block 对齐）
 * 
 * 用户描述问题 → PlotAgent 分析 → 提供剧情建议
 */
@Slf4j
@Component
public class FixWriterBlockWorkflow {

    @Autowired
    private WorkspaceManager workspaceManager;

    @Autowired
    private PlotAgent plotAgent;

    /**
     * 修复写作卡壳
     */
    public WorkflowResult fixWriterBlock(String novelName, String problemDescription) throws IOException {
        Workspace workspace = workspaceManager.openNovel(novelName);
        
        log.info("[卡文修复] 小说: {}, 问题: {}", novelName, problemDescription);
        
        // 1. 构建上下文
        String fullContext = workspace.buildFullContext(3);
        
        // 2. 调用 PlotAgent 分析并提供建议
        
        AgentContext ctx = new AgentContext(
                workspace.getRoot(),
                """
                你是网文小说剧情顾问。当作者遇到写作卡壳时：
                1. 分析问题原因
                2. 提供多条剧情走向建议（至少 3 条）
                3. 评估每条走向的爽度和受众适配度
                4. 给出推荐走向和理由
                
                我遇到了写作卡壳，请帮我分析并提供建议。
                
                我的问题: %s
                
                当前小说上下文:
                %s
                """.formatted(problemDescription, fullContext),
                null,
                Map.of("subTask", "analysis")
        );
        
        AgentResult result = plotAgent.execute(ctx);
        
        if (!result.isSuccess()) {
            return new WorkflowResult(false, "分析失败: " + result.getErrorMessage(), null);
        }
        
        return new WorkflowResult(true, result.getContent(), null);
    }

    /**
     * 工作流结果
     */
    public record WorkflowResult(
            boolean success,
            String content,
            String errorMessage
    ) {}
}
