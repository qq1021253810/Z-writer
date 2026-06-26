package com.zwriter.agent.character;

import com.zwriter.agent.base.AgentResult;
import com.zwriter.agent.base.BaseAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 人物塑造 Agent
 * 负责：角色档案（行为逻辑链）、关系网络（权力博弈）、成长弧线（认知升级）、对话生成（潜台词驱动）
 */
@Slf4j
@Component
public class CharacterAgent extends BaseAgent {

    public CharacterAgent() {
        registerSubTask("profile", this::generateProfile);
        registerSubTask("relation", this::generateRelation);
        registerSubTask("growth", this::generateGrowth);
        registerSubTask("dialogue", this::generateDialogue);
    }

    @Override
    public String name() {
        return "人物塑造 Agent";
    }

    @Override
    protected String defaultSubTask() {
        return "profile";
    }

    /**
     * 角色档案生成（行为逻辑链驱动）
     */
    private AgentResult generateProfile(Map<String, Object> params, String userInput) {
        String worldSetting = getParam(params, "worldSetting");
        String roleType = getParam(params, "roleType", "主角");

        String prompt = String.format("""
                请基于以下世界观生成角色档案（行为逻辑链驱动）：

                世界观:
                %s

                角色类型: %s

                需要包含：
                1. 基本信息（姓名、年龄、身份、外貌特征）
                   - 外貌要有辨识度，能暗示性格或经历

                2. 行为逻辑链（核心模块）
                   - 核心信念：角色最深层的价值观（形成于什么经历？）
                   - 决策模式：面对选择时的优先考量（利益/情感/原则？）
                   - 行为偏好：习惯性反应模式（进攻型/防守型/观察型？）
                   - 底线与禁区：绝对不做的事（违反时会怎样？）

                3. 背景故事（因果链）
                   - 出身环境（如何塑造了初始认知？）
                   - 关键经历（哪些事件改变了认知？如何改变的？）
                   - 当前处境（基于以上经历，为何处于这个位置？）

                4. 能力与资源
                   - 核心能力（基于经历获得的技能，非天赋开挂）
                   - 资源网络（人脉、信息、资金等）
                   - 能力边界（什么是做不到的？代价是什么？）

                5. 动机系统
                   - 表面目标（角色自己认为追求的）
                   - 深层动机（真正驱动行为的心理需求）
                   - 内在冲突（表面目标与深层动机的矛盾）

                6. 人际关系
                   - 重要联系人（关系基于什么？利益/情感/恩义？）
                   - 关系中的博弈（各取所需？相互制衡？）

                要求：
                - 所有设定必须有因果关系（A 经历导致 B 认知，B 认知导致 C 行为）
                - 禁止"完美角色"，必须有弱点和盲区
                - 智商情商在线，决策符合其认知水平和信息掌握
                - 每个行为都能追溯到其动机和经历

                请以 Markdown 格式输出。
                """, worldSetting, roleType);

        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "profile", "content", response));
    }

    /**
     * 角色关系网络（权力博弈与利益纠葛）
     */
    private AgentResult generateRelation(Map<String, Object> params, String userInput) {
        String characterProfiles = getParam(params, "characterProfiles", userInput);

        String prompt = String.format("""
                请基于以下角色档案生成关系网络（权力博弈与利益纠葛）：

                角色档案:
                %s

                需要输出：
                1. 关系图谱
                   - 每对角色间的关系类型（敌对/同盟/暧昧/师徒/利用/制衡）
                   - 关系基础（基于什么建立？利益交换/情感纽带/恩义回报/共同敌人？）
                   - 关系强度（1-10，基于共同利益/情感深度/制衡程度）

                2. 权力博弈分析
                   - 谁在关系中占据主导？为什么？
                   - 弱势方的制衡手段是什么？
                   - 关系中的信息差（谁知道什么秘密？）

                3. 利益纠葛
                   - 各方的核心诉求
                   - 诉求之间的冲突点
                   - 可能的合作空间

                4. 关系演变预测
                   - 在当前剧情压力下，哪些关系会变化？
                   - 变化的触发条件
                   - 变化后的新平衡

                5. 关键冲突点
                   - 哪些事件会引爆关系危机？
                   - 危机中各方的可能反应（基于其性格和动机）

                要求：
                - 关系必须有逻辑基础，禁止"无缘无故的爱/恨"
                - 每段关系都有博弈成分，没有纯粹的好人/坏人
                - 关系变化必须符合角色性格和利益考量
                - 信息差是推动关系变化的重要因素

                请以 Markdown 格式输出。
                """, characterProfiles);

        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "relation", "content", response));
    }

    /**
     * 角色成长弧线（认知升级驱动）
     */
    private AgentResult generateGrowth(Map<String, Object> params, String userInput) {
        String characterProfile = getParam(params, "characterProfile", userInput);

        String prompt = String.format("""
                请为以下角色设计成长弧线（认知升级驱动）：

                角色档案:
                %s

                需要包含：
                1. 初始认知状态
                   - 角色对世界/自己/核心问题的认知水平
                   - 认知局限（盲区、偏见、错误信念）
                   - 这些局限是如何形成的？

                2. 关键转折点（至少 3 个）
                   每个转折点需包含：
                   - 触发事件（什么打破了原有认知？）
                   - 认知冲突（新旧认知的碰撞）
                   - 选择与代价（角色做了什么选择？付出了什么？）
                   - 认知升级（新的认知是什么？）

                3. 成长轨迹
                   - 认知升级的递进关系（前一次升级为后一次奠基）
                   - 每次升级后的行为变化（决策模式、人际关系、目标追求）
                   - 是否有退步/反复？（真实成长不是线性的）

                4. 最终状态
                   - 最终认知水平（对世界/自己/核心问题的理解）
                   - 仍保留的局限（人不可能全知全能）
                   - 成长的意义（这个角色弧光传达了什么？）

                要求：
                - 成长是认知升级，不是能力开挂
                - 每次转折必须有触发事件和内在逻辑
                - 成长有代价，不是免费的
                - 最终状态与初始状态形成对比，但保留人性真实

                请以 Markdown 格式输出。
                """, characterProfile);

        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "growth", "content", response));
    }

    /**
     * 角色对话生成（潜台词与目的驱动）
     */
    private AgentResult generateDialogue(Map<String, Object> params, String userInput) {
        String characterA = getParam(params, "characterA");
        String characterB = getParam(params, "characterB");
        String scene = getParam(params, "scene");
        String context = getParam(params, "context", "");

        String prompt = String.format("""
                请生成以下场景的角色对话（潜台词与目的驱动）：

                角色 A: %s
                角色 B: %s
                场景: %s
                背景: %s

                对话设计原则：
                1. 每句对话都有目的
                   - 角色想从对方得到什么？（信息/承诺/让步/支持）
                   - 角色在隐藏什么？（真实想法/秘密/弱点）
                   - 角色在试探什么？（底线/态度/信息）

                2. 潜台词层次
                   - 字面意思（说出来的）
                   - 真实意图（想表达的）
                   - 情感暗流（未说出口的情绪）

                3. 信息博弈
                   - 双方掌握的信息不对称
                   - 通过对话逐步揭示/隐藏信息
                   - 信息差如何影响对话走向

                4. 关系动态
                   - 对话中权力关系的变化
                   - 试探与反试探
                   - 攻防转换

                输出格式：
                对每句对话标注：
                - 【字面】说出来的话
                - 【潜台词】真实意图
                - 【目的】想达到什么效果

                要求：
                - 对话符合角色性格和认知水平
                - 禁止废话，每句话都推动剧情或揭示关系
                - 体现信息差和博弈感
                - 言外之意 > 字面意思

                请以剧本格式输出。
                """, characterA, characterB, scene, context);

        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "dialogue", "content", response));
    }

    @Override
    protected String buildSystemPrompt() {
        return """
                你是人物心理学大师，专精高智商、高情商角色的塑造。

                你的核心理念：
                1. 行为逻辑完整：每个角色都有完整的行为逻辑链条（经历→认知→动机→行为）
                2. 智商情商在线：角色决策符合其认知水平和信息掌握，不会突然降智
                3. 潜台词丰富：每句话都有深意，言外之意大于字面意思
                4. 动机可追溯：每个行为都能追溯到其经历和动机

                你的设计原则：
                - 禁止无脑角色：所有行为都有合理铺垫和动机
                - 禁止完美角色：必须有弱点、盲区、内在冲突
                - 禁止巧合推动：关系变化、成长转折都有触发事件
                - 禁止脸谱化：没有纯粹的好人/坏人，只有不同立场的人

                你的输出要求：
                - 角色档案必须包含完整的行为逻辑链
                - 关系网络必须有权力博弈和利益纠葛
                - 成长弧线是认知升级，不是能力开挂
                - 对话设计体现信息差和博弈感

                输出格式：Markdown
                """;
    }
}
