package com.zwriter.session;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zwriter.workspace.WorkspaceManager;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话管理器（多轮对话模式）
 */
@Slf4j
@Component
public class SessionManager {

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    @Autowired
    private WorkspaceManager workspaceManager;

    /**
     * 创建会话
     */
    public Session createSession(String novelName, String mode) throws IOException {
        String sessionId = UUID.randomUUID().toString();
        Session session = new Session();
        session.setId(sessionId);
        session.setNovelName(novelName);
        session.setMode(mode); // "create" | "continue" | "chat"
        session.setState(Session.SessionState.ACTIVE);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        session.setHistory(new ArrayList<>());
        session.setContextData(new HashMap<>());

        sessions.put(sessionId, session);
        log.info("[Session] 创建会话: {} (小说: {}, 模式: {})", sessionId, novelName, mode);
        return session;
    }

    /**
     * 获取会话
     */
    public Session getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * 添加对话消息
     */
    public void addMessage(String sessionId, String role, String content) {
        Session session = sessions.get(sessionId);
        if (session != null) {
            session.getHistory().add(new Session.Message(role, content, LocalDateTime.now()));
            session.setUpdatedAt(LocalDateTime.now());
        }
    }

    /**
     * 更新会话上下文数据（存储工作流中间结果）
     */
    public void updateContext(String sessionId, String key, Object value) {
        Session session = sessions.get(sessionId);
        if (session != null) {
            session.getContextData().put(key, value);
        }
    }

    /**
     * 获取会话上下文数据
     */
    public Object getContext(String sessionId, String key) {
        Session session = sessions.get(sessionId);
        return session != null ? session.getContextData().get(key) : null;
    }

    /**
     * 关闭会话
     */
    public void closeSession(String sessionId) {
        Session session = sessions.get(sessionId);
        if (session != null) {
            session.setState(Session.SessionState.CLOSED);
            session.setUpdatedAt(LocalDateTime.now());
            log.info("[Session] 关闭会话: {}", sessionId);
        }
    }

    /**
     * 获取会话历史
     */
    public List<Session.Message> getHistory(String sessionId) {
        Session session = sessions.get(sessionId);
        return session != null ? session.getHistory() : List.of();
    }

    /**
     * 清理已关闭的会话
     */
    public void cleanupClosedSessions() {
        sessions.entrySet().removeIf(entry ->
            entry.getValue().getState() == Session.SessionState.CLOSED
        );
    }

    /**
     * Session 数据模型
     */
    @Data
    public static class Session {
        private String id;
        private String novelName;
        private String mode;
        private SessionState state;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<Message> history;
        private Map<String, Object> contextData;

        public enum SessionState {
            ACTIVE, WAITING_USER, COMPLETED, CLOSED
        }

        public record Message(String role, String content, LocalDateTime timestamp) {}
    }
}
