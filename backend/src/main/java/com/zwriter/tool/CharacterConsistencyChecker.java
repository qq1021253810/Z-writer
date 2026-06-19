package com.zwriter.tool;

import com.zwriter.entity.Character;
import com.zwriter.repository.CharacterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 人设一致性校验工具
 * 检查文本中角色的对话、行为、战力是否符合角色档案设定
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CharacterConsistencyChecker {

    private final CharacterRepository characterRepository;

    /** 对话提取正则：中文引号包裹的内容 */
    private static final Pattern DIALOGUE_PATTERN = Pattern.compile("[\u201c\u300c](.+?)[\u201d\u300d]");

    /**
     * 校验文本是否符合角色人设
     * @param novelId 小说ID
     * @param text 待校验文本
     * @return 一致性校验结果
     */
    public ConsistencyCheckResult checkConsistency(Long novelId, String text) {
        if (text == null || text.isEmpty()) {
            return new ConsistencyCheckResult(true, Collections.emptyList());
        }

        List<Character> characters = characterRepository.findByNovelId(novelId);
        if (characters.isEmpty()) {
            log.warn("小说 {} 未找到角色档案，跳过人设校验", novelId);
            return new ConsistencyCheckResult(true, Collections.emptyList());
        }

        List<ConsistencyViolation> violations = new ArrayList<>();

        for (Character character : characters) {
            // 检查对话一致性
            List<String> dialogues = extractDialoguesForCharacter(text, character.getName());
            for (String dialogue : dialogues) {
                ConsistencyViolation violation = checkDialogueConsistency(character, dialogue);
                if (violation != null) {
                    violations.add(violation);
                }
            }

            // 检查行为一致性
            ConsistencyViolation behaviorViolation = checkBehaviorConsistency(character, text);
            if (behaviorViolation != null) {
                violations.add(behaviorViolation);
            }

            // 检查战力一致性
            ConsistencyViolation powerViolation = checkPowerLevelConsistency(character, text);
            if (powerViolation != null) {
                violations.add(powerViolation);
            }
        }

        boolean isConsistent = violations.isEmpty();
        log.info("小说 {} 人设校验完成，发现 {} 处违规", novelId, violations.size());
        return new ConsistencyCheckResult(isConsistent, violations);
    }

    /**
     * 检查对话一致性
     * 校验角色对话是否符合其说话风格和口头禅
     * @param character 角色档案
     * @param dialogue 对话内容
     * @return 违规信息，若一致则返回 null
     */
    public ConsistencyViolation checkDialogueConsistency(Character character, String dialogue) {
        if (dialogue == null || dialogue.isEmpty()) {
            return null;
        }

        Map<String, Object> coreTraits = character.getCoreTraits();
        List<String> catchphrases = character.getCatchphrases();

        // 检查口头禅：如果角色有口头禅，但对话中从未出现，给出提示
        if (catchphrases != null && !catchphrases.isEmpty()) {
            boolean hasCatchphrase = false;
            for (String phrase : catchphrases) {
                if (dialogue.contains(phrase)) {
                    hasCatchphrase = true;
                    break;
                }
            }
            if (!hasCatchphrase && dialogue.length() > 20) {
                // 长对话中未出现口头禅，可能不一致
                String suggestion = String.format("建议在对话中加入角色的口头禅（如：%s）", String.join("、", catchphrases));
                return new ConsistencyViolation("dialogue", character.getName(),
                        String.format("角色「%s」的对话中未体现其口头禅风格", character.getName()),
                        suggestion);
            }
        }

        // 检查说话风格：基于 coreTraits 中的 personality 和 speech_style
        if (coreTraits != null) {
            Object personality = coreTraits.get("personality");
            Object speechStyle = coreTraits.get("speech_style");

            if (speechStyle instanceof String style && !style.isEmpty()) {
                ConsistencyViolation styleViolation = checkSpeechStyle(character.getName(), dialogue, style);
                if (styleViolation != null) {
                    return styleViolation;
                }
            }

            if (personality instanceof String personalityStr && !personalityStr.isEmpty()) {
                ConsistencyViolation personalityViolation = checkDialoguePersonality(character.getName(), dialogue, personalityStr);
                if (personalityViolation != null) {
                    return personalityViolation;
                }
            }
        }

        return null;
    }

    /**
     * 检查行为一致性
     * 校验文本中角色的行为是否符合其性格设定
     * @param character 角色档案
     * @param behavior 包含角色行为的文本
     * @return 违规信息，若一致则返回 null
     */
    public ConsistencyViolation checkBehaviorConsistency(Character character, String behavior) {
        if (behavior == null || behavior.isEmpty()) {
            return null;
        }

        // 仅在文本中包含角色名时才检查
        if (!behavior.contains(character.getName())) {
            return null;
        }

        Map<String, Object> coreTraits = character.getCoreTraits();
        if (coreTraits == null) {
            return null;
        }

        Object personality = coreTraits.get("personality");
        if (!(personality instanceof String personalityStr) || personalityStr.isEmpty()) {
            return null;
        }

        // 检查行为与性格的冲突
        Map<String, List<String>> behaviorConflictMap = buildBehaviorConflictMap();
        for (Map.Entry<String, List<String>> entry : behaviorConflictMap.entrySet()) {
            String trait = entry.getKey();
            List<String> conflictingBehaviors = entry.getValue();

            // 角色具有该性格特征
            if (personalityStr.contains(trait)) {
                for (String conflictingBehavior : conflictingBehaviors) {
                    if (behavior.contains(character.getName()) && behavior.contains(conflictingBehavior)) {
                        String suggestion = String.format("角色「%s」性格为「%s」，建议修改与性格冲突的行为描述", character.getName(), trait);
                        return new ConsistencyViolation("behavior", character.getName(),
                                String.format("角色「%s」的性格为「%s」，但出现了「%s」的行为，与人设不符",
                                        character.getName(), trait, conflictingBehavior),
                                suggestion);
                    }
                }
            }
        }

        return null;
    }

    /**
     * 检查战力一致性
     * 校验文本中角色的战力描述是否符合其能力设定
     * @param character 角色档案
     * @param description 包含战力描述的文本
     * @return 违规信息，若一致则返回 null
     */
    public ConsistencyViolation checkPowerLevelConsistency(Character character, String description) {
        if (description == null || description.isEmpty()) {
            return null;
        }

        // 仅在文本中包含角色名时才检查
        if (!description.contains(character.getName())) {
            return null;
        }

        Map<String, Object> abilities = character.getAbilities();
        if (abilities == null) {
            return null;
        }

        Object powerLevel = abilities.get("power_level");
        Object abilitiesList = abilities.get("skills");

        // 检查战力等级冲突
        if (powerLevel instanceof String level && !level.isEmpty()) {
            Map<String, List<String>> powerConflictMap = buildPowerConflictMap();
            for (Map.Entry<String, List<String>> entry : powerConflictMap.entrySet()) {
                String powerRank = entry.getKey();
                List<String> conflictingDescriptions = entry.getValue();

                if (level.contains(powerRank)) {
                    for (String conflictDesc : conflictingDescriptions) {
                        if (description.contains(character.getName()) && description.contains(conflictDesc)) {
                            String suggestion = String.format("角色「%s」战力等级为「%s」，建议调整战力描述以符合设定", character.getName(), level);
                            return new ConsistencyViolation("power_level", character.getName(),
                                    String.format("角色「%s」战力等级为「%s」，但出现了「%s」的描述，与能力设定不符",
                                            character.getName(), level, conflictDesc),
                                    suggestion);
                        }
                    }
                }
            }
        }

        // 检查使用了角色不具备的技能
        if (abilitiesList instanceof List<?> skills && !skills.isEmpty()) {
            List<String> knownSkills = skills.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();

            // 常见技能关键词检测
            List<String> skillKeywords = List.of("法术", "剑法", "拳法", "术式", "忍术", "魔法", "武功", "内力", "灵力", "神通");
            for (String keyword : skillKeywords) {
                if (description.contains(character.getName()) && description.contains(keyword)) {
                    // 检查角色是否拥有包含该关键词的技能
                    boolean hasSkill = knownSkills.stream().anyMatch(skill -> skill.contains(keyword));
                    if (!hasSkill) {
                        String suggestion = String.format("角色「%s」不具备「%s」相关技能，建议修改或补充角色能力设定", character.getName(), keyword);
                        return new ConsistencyViolation("power_level", character.getName(),
                                String.format("角色「%s」使用了「%s」，但角色能力设定中未包含相关技能", character.getName(), keyword),
                                suggestion);
                    }
                }
            }
        }

        return null;
    }

    // ========== 私有辅助方法 ==========

    /**
     * 提取文本中属于指定角色的对话
     */
    private List<String> extractDialoguesForCharacter(String text, String characterName) {
        List<String> dialogues = new ArrayList<>();
        // 查找角色名附近的对话内容
        Matcher matcher = DIALOGUE_PATTERN.matcher(text);
        while (matcher.find()) {
            String dialogue = matcher.group(1);
            // 检查对话前是否出现角色名（在对话前100字符范围内）
            int dialogueStart = matcher.start();
            int searchStart = Math.max(0, dialogueStart - 100);
            String contextBefore = text.substring(searchStart, dialogueStart);
            if (contextBefore.contains(characterName)) {
                dialogues.add(dialogue);
            }
        }
        return dialogues;
    }

    /**
     * 检查说话风格一致性
     */
    private ConsistencyViolation checkSpeechStyle(String characterName, String dialogue, String speechStyle) {
        // 说话风格与对话内容的冲突检测
        Map<String, List<String>> styleConflictMap = new HashMap<>();
        styleConflictMap.put("粗犷", List.of("温柔地说", "轻声细语", "害羞地", "怯生生"));
        styleConflictMap.put("温柔", List.of("怒吼", "咆哮", "暴怒", "粗暴地"));
        styleConflictMap.put("冷酷", List.of("热情地说", "激动地", "兴奋地喊"));
        styleConflictMap.put("幽默", List.of("严肃地说", "沉声道", "正色道"));
        styleConflictMap.put("高傲", List.of("低声下气", "卑躬屈膝", "恳求", "哀求"));
        styleConflictMap.put("沉稳", List.of("慌张地说", "惊慌失措", "手忙脚乱"));

        for (Map.Entry<String, List<String>> entry : styleConflictMap.entrySet()) {
            String style = entry.getKey();
            List<String> conflicts = entry.getValue();

            if (speechStyle.contains(style)) {
                for (String conflict : conflicts) {
                    if (dialogue.contains(conflict)) {
                        return new ConsistencyViolation("dialogue", characterName,
                                String.format("角色「%s」说话风格为「%s」，但对话中出现了「%s」的描述", characterName, style, conflict),
                                String.format("建议将对话描述调整为符合「%s」风格的表达", style));
                    }
                }
            }
        }
        return null;
    }

    /**
     * 检查对话与性格的一致性
     */
    private ConsistencyViolation checkDialoguePersonality(String characterName, String dialogue, String personality) {
        // 性格与对话内容的冲突检测
        Map<String, List<String>> personalityDialogueMap = new HashMap<>();
        personalityDialogueMap.put("内向", List.of("大声喊", "高声说", "滔滔不绝", "侃侃而谈"));
        personalityDialogueMap.put("外向", List.of("沉默不语", "一言不发", "低头不语"));
        personalityDialogueMap.put("胆小", List.of("毫不畏惧", "勇往直前", "挺身而出"));
        personalityDialogueMap.put("勇敢", List.of("吓得发抖", "畏缩不前", "惊恐万分"));
        personalityDialogueMap.put("聪明", List.of("愚蠢地说", "不明所以", "一头雾水"));
        personalityDialogueMap.put("善良", List.of("恶狠狠", "残忍地笑", "冷血地说"));

        for (Map.Entry<String, List<String>> entry : personalityDialogueMap.entrySet()) {
            String trait = entry.getKey();
            List<String> conflicts = entry.getValue();

            if (personality.contains(trait)) {
                for (String conflict : conflicts) {
                    if (dialogue.contains(conflict)) {
                        return new ConsistencyViolation("dialogue", characterName,
                                String.format("角色「%s」性格为「%s」，但对话中出现了「%s」的表现", characterName, trait, conflict),
                                String.format("建议修改对话内容以符合角色「%s」的性格", trait));
                    }
                }
            }
        }
        return null;
    }

    /**
     * 构建行为与性格冲突映射
     */
    private Map<String, List<String>> buildBehaviorConflictMap() {
        Map<String, List<String>> map = new HashMap<>();
        map.put("善良", List.of("残忍", "冷血", "无情", "狠毒", "暴虐"));
        map.put("胆小", List.of("冲锋陷阵", "勇往直前", "毫不畏惧", "挺身而出", "视死如归"));
        map.put("高傲", List.of("低声下气", "卑躬屈膝", "摇尾乞怜", "阿谀奉承"));
        map.put("正直", List.of("阴谋诡计", "暗箭伤人", "背信弃义", "两面三刀"));
        map.put("冷静", List.of("暴跳如雷", "歇斯底里", "惊慌失措", "手忙脚乱"));
        map.put("忠诚", List.of("背叛", "出卖", "倒戈", "叛变"));
        return map;
    }

    /**
     * 构建战力等级冲突映射
     */
    private Map<String, List<String>> buildPowerConflictMap() {
        Map<String, List<String>> map = new HashMap<>();
        map.put("弱小", List.of("一击必杀", "轻松击败", "碾压", "秒杀", "无人能敌"));
        map.put("普通", List.of("天下无敌", "举世无双", "无人能敌", "横扫千军"));
        map.put("强大", List.of("不堪一击", "毫无还手之力", "轻易落败", "毫无招架之力"));
        map.put("无敌", List.of("被击败", "落败", "不敌", "败下阵来", "惨败"));
        return map;
    }

    /**
     * 一致性校验结果
     */
    public record ConsistencyCheckResult(
            boolean isConsistent,
            List<ConsistencyViolation> violations
    ) {
    }

    /**
     * 一致性违规详情
     */
    public record ConsistencyViolation(
            String type,
            String characterName,
            String description,
            String suggestion
    ) {
    }
}
