---
type: concept
title: 基础设施栈
created: 2026-06-18
updated: 2026-06-18
tags: [architecture, infrastructure]
sources: [wiki/raw/specs/agent-memory.md]
confidence: high
contested: false
contradictions: []
verified_by: human
approved: true
---

# 基础设施栈

Z-writer 后端采用 Spring Boot 3 + Spring AI 架构，通过 Docker Compose 统一管理三大基础设施服务，配合 Flyway 进行数据库版本化迁移。

## Docker Compose 服务

路径：`docker-compose.yml`，共定义 **3 个服务**，均配置健康检查和持久化数据卷。

| 服务 | 镜像 | 端口 | 用户/库名 | 健康检查 | 持久卷 |
|------|------|------|-----------|----------|--------|
| PostgreSQL | `postgres:16-alpine` | 5432 | zwriter / zwriter | `pg_isready` | `pgdata:/var/lib/postgresql/data` |
| Redis | `redis:7-alpine` | 6379 | — | `redis-cli ping` | `redisdata:/data` |
| ChromaDB | `chromadb/chroma:latest` | 8000 | — | `curl /api/v1/heartbeat` | `chromadata:/chroma/chroma` |

### 健康检查配置详情

所有服务采用统一的健康检查策略：

```yaml
healthcheck:
  interval: 10s      # 每 10 秒检查一次
  timeout: 5s        # 超时时间 5 秒
  retries: 5         # 连续失败 5 次标记为 unhealthy
```

**各服务检查命令**：
- **PostgreSQL**：`pg_isready -U zwriter` — 检查数据库连接就绪状态
- **Redis**：`redis-cli ping` — 返回 PONG 表示服务正常
- **ChromaDB**：`curl -f http://localhost:8000/api/v1/heartbeat` — HTTP 心跳接口

### 数据卷挂载路径

- **PostgreSQL**：`pgdata:/var/lib/postgresql/data` — 存储数据库文件、WAL 日志
- **Redis**：`redisdata:/data` — 存储 RDB/AOF 持久化文件
- **ChromaDB**：`chromadata:/chroma/chroma` — 存储向量索引和元数据

### 环境变量

**PostgreSQL**：
- `POSTGRES_USER=zwriter`
- `POSTGRES_PASSWORD=zwriter123`
- `POSTGRES_DB=zwriter`

**Redis**：
- 无密码认证（开发环境）

## application.yml 配置

路径：`backend/src/main/resources/application.yml`

### 数据源与 JPA

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/zwriter
    username: zwriter
    password: zwriter123
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate   # Schema 由 Flyway 管理，Hibernate 仅做校验
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
```

**关键配置说明**：
- `ddl-auto: validate` — 启动时校验实体与数据库 schema 是否一致，不一致则启动失败
- `format_sql: true` — 格式化 SQL 输出，便于调试时阅读
- `show-sql: false` — 生产环境关闭 SQL 日志，避免性能损耗

### Flyway 迁移

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

**配置说明**：
- `baseline-on-migrate: true` — 对已有数据库执行基线迁移，避免重复执行历史脚本

### Redis 配置

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

### Spring AI Ollama 配置

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: qwen3:1.7b          # 对话模型
      embedding:
        model: nomic-embed-text     # 嵌入模型
```

### ChromaDB 向量存储

```yaml
spring:
  ai:
    vectorstore:
      chroma:
        host: localhost
        port: 8000
        collection-name: zwriter_novels
        initialize-schema: true
```

**配置说明**：
- `collection-name: zwriter_novels` — 向量集合名称，用于隔离不同业务的向量数据
- `initialize-schema: true` — 自动创建集合和索引，无需手动初始化

### LLM 应用设置

```yaml
llm:
  mock-enabled: false    # 禁用 Mock，使用真实 LLM
  max-tokens: 4096       # 单次生成最大 token 数
  temperature: 0.8       # 生成温度，平衡创造性与稳定性
```

**参数说明**：
- `mock-enabled: false` — 生产环境禁用 Mock LLM，调用真实 Ollama 服务
- `max-tokens: 4096` — 限制单次生成最大 token 数，避免超长输出
- `temperature: 0.8` — 温度参数，0.8 平衡创造性与稳定性，适合小说创作场景

### 日志配置

```yaml
logging:
  level:
    com.zwriter: DEBUG
    org.springframework.ai: DEBUG
```

**日志级别**：
- `com.zwriter: DEBUG` — 业务代码输出 DEBUG 级别日志
- `org.springframework.ai: DEBUG` — Spring AI 框架输出 DEBUG 日志，便于调试 LLM 调用

## Flyway 数据库迁移

迁移脚本：`backend/src/main/resources/db/migration/V1__init_schema.sql`

### 6 张业务表

采用 PostgreSQL 特有类型（`TEXT[]`、`JSONB`、`TIMESTAMP` 等）：

| 表名 | 用途 | 关键字段 | 说明 |
|------|------|----------|------|
| `novel_info` | 小说信息 | `tags TEXT[]`, `golden_finger VARCHAR(500)` | 作品元数据，支持标签数组 |
| `character_table` | 角色 | `basic_info JSONB`, `relationships JSONB` | 表名避开 SQL 关键字 `CHARACTER`，使用 JSONB 存储复杂属性 |
| `volume_outline` | 卷大纲 | `volume_number INTEGER`, `core_conflict TEXT` | 按卷组织的剧情大纲 |
| `plot_timeline` | 剧情时间线 | `event_time TIMESTAMP`, `characters_involved BIGINT[]` | 事件时序记录，支持多角色关联 |
| `foreshadow_lib` | 伏笔库 | `setup_chapter INTEGER`, `related_characters BIGINT[]` | 伏笔埋设与回收追踪 |
| `chapter_content` | 章节内容 | `word_count INTEGER`, `hook_strength INTEGER` | 正文章节存储，包含质量评分 |

**PostgreSQL 特有类型使用**：
- `TEXT[]` — 文本数组，用于标签、角色列表
- `BIGINT[]` — 整数数组，用于关联 ID 列表
- `JSONB` — 二进制 JSON，用于存储复杂嵌套结构
- `TIMESTAMP` — 时间戳，自动记录创建/更新时间

### 外键与级联删除

所有子表均通过 `REFERENCES novel_info(id) ON DELETE CASCADE` 关联到 `novel_info`，确保删除小说时自动清理所有关联数据：

| 子表 | 外键字段 | 引用表 | 级联行为 |
|------|----------|--------|----------|
| `character_table` | `novel_id` | `novel_info(id)` | `ON DELETE CASCADE` |
| `volume_outline` | `novel_id` | `novel_info(id)` | `ON DELETE CASCADE` |
| `plot_timeline` | `novel_id` | `novel_info(id)` | `ON DELETE CASCADE` |
| `foreshadow_lib` | `novel_id` | `novel_info(id)` | `ON DELETE CASCADE` |
| `chapter_content` | `novel_id` | `novel_info(id)` | `ON DELETE CASCADE` |

### 6 个索引

```sql
CREATE INDEX idx_character_novel ON character_table(novel_id);
CREATE INDEX idx_volume_novel ON volume_outline(novel_id);
CREATE INDEX idx_timeline_novel ON plot_timeline(novel_id);
CREATE INDEX idx_foreshadow_novel ON foreshadow_lib(novel_id);
CREATE INDEX idx_chapter_novel ON chapter_content(novel_id);
CREATE INDEX idx_chapter_number ON chapter_content(novel_id, volume_number, chapter_number);
```

- 前 5 个为单列外键索引，加速按小说 ID 的关联查询
- 第 6 个为复合索引 `(novel_id, volume_number, chapter_number)`，支持章节定位查询，同时配合 `UNIQUE` 约束保证章节编号唯一性

## 启动顺序

1. 启动 Docker Desktop
2. 启动基础设施：`docker-compose up -d`
3. 等待服务健康检查通过（`docker-compose ps` 确认所有服务状态为 `healthy`）
4. 启动后端：`cd backend; mvn spring-boot:run`（端口 8080）
5. 启动前端：`cd frontend; npm run dev`（端口 5173）

## 相关页面

- [[vector-knowledge-service]] — 向量知识服务，使用 ChromaDB 存储嵌入向量
- [[data-model]] — 数据模型层，实体定义与表结构详情
- [[common-issues]] — 常见问题，包含基础设施启动与配置排障
