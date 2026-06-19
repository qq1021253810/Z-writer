-- 小说基础信息表
CREATE TABLE novel_info (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    genre VARCHAR(100) NOT NULL,
    tags TEXT[],
    synopsis TEXT,
    golden_finger VARCHAR(500),
    total_volumes INTEGER DEFAULT 1,
    status VARCHAR(50) DEFAULT 'draft',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 角色档案表
CREATE TABLE character_table (
    id BIGSERIAL PRIMARY KEY,
    novel_id BIGINT NOT NULL REFERENCES novel_info(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    role_type VARCHAR(50) NOT NULL,
    basic_info JSONB,
    core_traits JSONB,
    abilities JSONB,
    relationships JSONB,
    catchphrases TEXT[],
    growth_curve JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 分卷大纲表
CREATE TABLE volume_outline (
    id BIGSERIAL PRIMARY KEY,
    novel_id BIGINT NOT NULL REFERENCES novel_info(id) ON DELETE CASCADE,
    volume_number INTEGER NOT NULL,
    title VARCHAR(255),
    summary TEXT,
    core_conflict TEXT,
    chapter_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 时间线事件表
CREATE TABLE plot_timeline (
    id BIGSERIAL PRIMARY KEY,
    novel_id BIGINT NOT NULL REFERENCES novel_info(id) ON DELETE CASCADE,
    event_time TIMESTAMP NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    characters_involved BIGINT[],
    power_level VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 伏笔库表
CREATE TABLE foreshadow_lib (
    id BIGSERIAL PRIMARY KEY,
    novel_id BIGINT NOT NULL REFERENCES novel_info(id) ON DELETE CASCADE,
    setup_chapter INTEGER NOT NULL,
    payoff_chapter INTEGER,
    clue_description TEXT NOT NULL,
    status VARCHAR(50) DEFAULT 'planted',
    related_characters BIGINT[],
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 章节内容表
CREATE TABLE chapter_content (
    id BIGSERIAL PRIMARY KEY,
    novel_id BIGINT NOT NULL REFERENCES novel_info(id) ON DELETE CASCADE,
    volume_number INTEGER NOT NULL,
    chapter_number INTEGER NOT NULL,
    title VARCHAR(255),
    content TEXT NOT NULL,
    word_count INTEGER DEFAULT 0,
    hook_strength INTEGER,
    satisfaction_rating INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(novel_id, volume_number, chapter_number)
);

-- 索引
CREATE INDEX idx_character_novel ON character_table(novel_id);
CREATE INDEX idx_volume_novel ON volume_outline(novel_id);
CREATE INDEX idx_timeline_novel ON plot_timeline(novel_id);
CREATE INDEX idx_foreshadow_novel ON foreshadow_lib(novel_id);
CREATE INDEX idx_chapter_novel ON chapter_content(novel_id);
CREATE INDEX idx_chapter_number ON chapter_content(novel_id, volume_number, chapter_number);
