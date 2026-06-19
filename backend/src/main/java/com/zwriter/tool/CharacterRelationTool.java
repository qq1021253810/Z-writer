package com.zwriter.tool;

import com.zwriter.entity.Character;
import com.zwriter.repository.CharacterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色关系图生成工具
 * 支持：生成角色关系网络、输出可视化数据格式
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CharacterRelationTool {
    
    private final CharacterRepository characterRepository;
    
    /**
     * 生成角色关系图数据
     * @param novelId 小说ID
     * @return 关系图数据（nodes + edges）
     */
    public RelationGraph generateRelationGraph(Long novelId) {
        List<Character> characters = characterRepository.findByNovelId(novelId);
        
        if (characters.isEmpty()) {
            return new RelationGraph(Collections.emptyList(), Collections.emptyList());
        }
        
        // 生成节点
        List<GraphNode> nodes = characters.stream()
            .map(c -> {
                String roleLabel = switch (c.getRoleType()) {
                    case "protagonist" -> "主角";
                    case "antagonist" -> "反派";
                    case "supporting" -> "配角";
                    case "love_interest" -> "女主/男主";
                    default -> c.getRoleType();
                };
                return new GraphNode(
                    String.valueOf(c.getId()),
                    c.getName(),
                    roleLabel,
                    getCharacterSize(c.getRoleType())
                );
            })
            .collect(Collectors.toList());
        
        // 生成边（从 relationships JSON 解析）
        List<GraphEdge> edges = new ArrayList<>();
        Set<Long> characterIds = characters.stream().map(Character::getId).collect(Collectors.toSet());
        
        for (Character c : characters) {
            if (c.getRelationships() != null) {
                parseRelationships(c, characterIds, edges);
            }
        }
        
        return new RelationGraph(nodes, edges);
    }
    
    /**
     * 生成 Mermaid 格式的关系图
     */
    public String generateMermaidGraph(Long novelId) {
        RelationGraph graph = generateRelationGraph(novelId);
        
        StringBuilder sb = new StringBuilder();
        sb.append("graph TD\n");
        
        for (GraphNode node : graph.nodes()) {
            sb.append(String.format("    %s[\"%s\\n(%s)\"]\n", node.id(), node.label(), node.type()));
        }
        
        sb.append("\n");
        
        for (GraphEdge edge : graph.edges()) {
            sb.append(String.format("    %s -->|\"%s\"| %s\n", edge.source(), edge.relation(), edge.target()));
        }
        
        return sb.toString();
    }
    
    private int getCharacterSize(String roleType) {
        return switch (roleType) {
            case "protagonist" -> 30;
            case "love_interest" -> 25;
            case "antagonist" -> 25;
            default -> 20;
        };
    }
    
    @SuppressWarnings("unchecked")
    private void parseRelationships(Character character, Set<Long> characterIds, List<GraphEdge> edges) {
        try {
            Object relationships = character.getRelationships();
            if (relationships instanceof Map) {
                Map<String, Object> relMap = (Map<String, Object>) relationships;
                for (Map.Entry<String, Object> entry : relMap.entrySet()) {
                    String targetName = entry.getKey();
                    String relation = entry.getValue().toString();
                    
                    // 尝试找到目标角色ID
                    Optional<Long> targetId = characterIds.stream()
                        .filter(id -> id.toString().equals(targetName))
                        .findFirst();
                    
                    if (targetId.isPresent()) {
                        edges.add(new GraphEdge(
                            String.valueOf(character.getId()),
                            String.valueOf(targetId.get()),
                            relation
                        ));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析角色 {} 的关系数据失败", character.getName(), e);
        }
    }
    
    public record GraphNode(String id, String label, String type, int size) {}
    public record GraphEdge(String source, String target, String relation) {}
    public record RelationGraph(List<GraphNode> nodes, List<GraphEdge> edges) {}
}
