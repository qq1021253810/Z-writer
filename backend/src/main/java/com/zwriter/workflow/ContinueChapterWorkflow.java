package com.zwriter.workflow;

import com.zwriter.agent.base.AgentContext;
import com.zwriter.agent.base.AgentResult;
import com.zwriter.agent.writing.WritingAgent;
import com.zwriter.workspace.Workspace;
import com.zwriter.workspace.WorkspaceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * 续写章节工作流
 * 
 * 工作区上下文 → WritingAgent → 保存章节
 */
@Slf4j
@Component
public class ContinueChapterWorkflow {

    @Autowired
    private WorkspaceManager workspaceManager;

    @Autowired
    private WritingAgent writingAgent;

    /**
     * 续写章节（同步模式）
     * 读取最近章节上下文，生成下一章并保存
     */
    public WorkflowResult continueChapter(String novelName) throws IOException {
        Workspace workspace = workspaceManager.openNovel(novelName);
        
        log.info("[续写工作流] 小说: {}, 开始续写", novelName);
        
        // 1. 构建上下文
        String fullContext = workspace.buildFullContext(3); // 最近 3 章
        log.info("[续写工作流] 上下文长度: {}", fullContext.length());
        
        // 2. 获取下一章编号
        int chapterNum = workspace.getNextChapterNum();
        log.info("[续写工作流] 下一章编号: {}", chapterNum);
        
        // 3. 调用 WritingAgent 生成章节
        String systemPrompt = """
                你是网文小说续写助手。基于已有内容生成新章节。
                要求：
                - 保持文风一致
                - 角色设定不矛盾
                - 剧情延续自然
                - 每章 2000-3000 字
                - 结尾留下钩子
                """;
        
        AgentContext ctx = new AgentContext(
                workspace.getRoot(),
                "请续写第 " + chapterNum + " 章。基于以下上下文：\n\n" + fullContext,
                systemPrompt,
                Map.of(
                        "subTask", "chapter",
                        "writingStyle", "热血爽文",
                        "wordCount", "3000"
                )
        );
        
        AgentResult result = writingAgent.execute(ctx);
        
        if (!result.isSuccess()) {
            log.error("[续写工作流] Agent 执行失败: {}", result.getErrorMessage());
            return new WorkflowResult(false, "续写失败: " + result.getErrorMessage(), null, null);
        }
        
        // 4. 保存到工作区
        String content = result.getContent();
        workspace.saveChapter(chapterNum, content);
        
        log.info("[续写工作流] 第 {} 章已保存，字数: {}", chapterNum, content.length());
        
        return new WorkflowResult(true, content, null, Map.of(
                "chapterNum", chapterNum,
                "wordCount", content.length(),
                "novelName", novelName
        ));
    }

    /**
     * 续写章节（对话模式 - 轻量级引导）
     */
    public DialogueStep getNextStep(String novelName, Map<String, Object> context) {
        int step = getProgressStep(context);
        return switch (step) {
            case 1 -> new DialogueStep(1, "续写方向", 
                "你想让下一章剧情往什么方向发展？（例如：战斗、日常、转折、揭秘等）",
                "direction", false);
            case 2 -> new DialogueStep(2, "特殊要求",
                "有没有特殊要求？（例如：某个角色必须有戏份、某个伏笔要揭示等，没有可以说'无'）",
                "specialReqs", false);
            case 3 -> new DialogueStep(3, "开始生成",
                "好的，我现在开始生成第 %d 章。".formatted(getNextChapterNum(novelName)),
                "generate", true);
            default -> new DialogueStep(0, "完成", "对话流程已完成", "done", true);
        };
    }

    public DialogueStep processUserReply(String novelName, String userReply, Map<String, Object> context) {
        int step = getProgressStep(context);
        context.put("step_" + step + "_reply", userReply);
        context.put("progress", step + 1);
        return getNextStep(novelName, context);
    }

    public WorkflowResult executeGeneration(String novelName, Map<String, Object> context) throws IOException {
        Workspace workspace = workspaceManager.openNovel(novelName);
        int chapterNum = workspace.getNextChapterNum();
        
        // 构建完整上下文
        String fullContext = workspace.buildFullContext(3);
        
        // 合并用户输入的续写方向和特殊要求
        String direction = (String) context.getOrDefault("step_1_reply", "");
        String specialReqs = (String) context.getOrDefault("step_2_reply", "无");
        
        String userPrompt = """
                请续写第 %d 章。
                
                上下文:
                %s
                
                续写方向: %s
                特殊要求: %s
                """.formatted(chapterNum, fullContext, direction, specialReqs);
        
        AgentContext ctx = new AgentContext(
                workspace.getRoot(),
                userPrompt,
                """
                你是网文小说续写助手。基于已有内容生成新章节。
                要求：
                - 保持文风一致
                - 角色设定不矛盾
                - 剧情延续自然
                - 每章 2000-3000 字
                - 结尾留下钩子
                """,
                Map.of(
                        "subTask", "chapter",
                        "writingStyle", "热血爽文",
                        "wordCount", "3000"
                )
        );
        
        AgentResult result = writingAgent.execute(ctx);
        
        if (!result.isSuccess()) {
            return new WorkflowResult(false, "续写失败: " + result.getErrorMessage(), null, null);
        }
        
        workspace.saveChapter(chapterNum, result.getContent());
        
        return new WorkflowResult(true, result.getContent(), null, Map.of(
                "chapterNum", chapterNum,
                "wordCount", result.getContent().length(),
                "novelName", novelName
        ));
    }

    private int getProgressStep(Map<String, Object> context) {
        return (int) context.getOrDefault("progress", 1);
    }

    private int getNextChapterNum(String novelName) {
        try {
            return workspaceManager.openNovel(novelName).getNextChapterNum();
        } catch (IOException e) {
            return 1;
        }
    }

    /**
     * 工作流结果
     */
    public record WorkflowResult(
            boolean success,
            String content,
            String errorMessage,
            Map<String, Object> metadata
    ) {}
}
