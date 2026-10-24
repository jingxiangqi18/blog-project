-- Run this file once against an empty MySQL or TiDB database.
-- It is intentionally not executed automatically when the application starts.

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sections (
    id BIGINT NOT NULL AUTO_INCREMENT,
    slug VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(1000) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sections_slug (slug),
    KEY idx_sections_enabled_sort (enabled, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO sections(slug, name, description, sort_order, enabled, created_at, updated_at)
SELECT 'technology', 'Technology', 'Technical learning, principles and engineering practice', 10, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM sections WHERE slug = 'technology');

INSERT INTO sections(slug, name, description, sort_order, enabled, created_at, updated_at)
SELECT 'life', 'Life', 'Life, reading and personal reflections', 20, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM sections WHERE slug = 'life');

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    section_id BIGINT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_categories_name (name),
    KEY idx_categories_section_id (section_id),
    CONSTRAINT fk_categories_section FOREIGN KEY (section_id) REFERENCES sections(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS articles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content LONGTEXT NOT NULL,
    category_id BIGINT NULL,
    author_id BIGINT NOT NULL,
    section_id BIGINT NULL,
    summary VARCHAR(500) NULL,
    content_type VARCHAR(50) NOT NULL DEFAULT 'general',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_articles_category_id (category_id),
    KEY idx_articles_author_id (author_id),
    KEY idx_articles_updated_at (updated_at),
    KEY idx_articles_section_updated (section_id, updated_at, id),
    KEY idx_articles_type_updated (content_type, updated_at, id),
    CONSTRAINT fk_articles_section
        FOREIGN KEY (section_id) REFERENCES sections(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_articles_category
        FOREIGN KEY (category_id) REFERENCES categories (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_articles_author
        FOREIGN KEY (author_id) REFERENCES users (id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS article_drafts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    article_id BIGINT NULL,
    author_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL DEFAULT '',
    content LONGTEXT NOT NULL,
    category_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_article_drafts_article_author (article_id, author_id),
    KEY idx_article_drafts_author_id (author_id),
    KEY idx_article_drafts_updated_at (updated_at),
    CONSTRAINT fk_article_drafts_article
        FOREIGN KEY (article_id) REFERENCES articles (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_article_drafts_author
        FOREIGN KEY (author_id) REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_article_drafts_category
        FOREIGN KEY (category_id) REFERENCES categories (id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS article_revisions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    article_id BIGINT NOT NULL,
    revision_number INT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content LONGTEXT NOT NULL,
    category_id BIGINT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_article_revisions_number (article_id, revision_number),
    KEY idx_article_revisions_created_at (article_id, created_at),
    KEY idx_article_revisions_created_by (created_by),
    CONSTRAINT fk_article_revisions_article
        FOREIGN KEY (article_id) REFERENCES articles (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_article_revisions_created_by
        FOREIGN KEY (created_by) REFERENCES users (id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS knowledge_cards (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    content LONGTEXT NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_knowledge_cards_title (title),
    KEY idx_knowledge_cards_created_by (created_by),
    KEY idx_knowledge_cards_updated_at (updated_at),
    CONSTRAINT fk_knowledge_cards_created_by
        FOREIGN KEY (created_by) REFERENCES users (id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    article_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    content TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_comments_article_id (article_id),
    KEY idx_comments_author_id (author_id),
    KEY idx_comments_parent_id (parent_id),
    CONSTRAINT fk_comments_article
        FOREIGN KEY (article_id) REFERENCES articles (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_comments_author
        FOREIGN KEY (author_id) REFERENCES users (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_comments_parent
        FOREIGN KEY (parent_id) REFERENCES comments (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS article_likes (
    article_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (article_id, user_id),
    KEY idx_article_likes_user_id (user_id),
    CONSTRAINT fk_article_likes_article
        FOREIGN KEY (article_id) REFERENCES articles (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_article_likes_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS article_favorites (
    article_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (article_id, user_id),
    KEY idx_article_favorites_user_id (user_id),
    CONSTRAINT fk_article_favorites_article
        FOREIGN KEY (article_id) REFERENCES articles (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_article_favorites_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS comment_likes (
    comment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (comment_id, user_id),
    KEY idx_comment_likes_user_id (user_id),
    CONSTRAINT fk_comment_likes_comment
        FOREIGN KEY (comment_id) REFERENCES comments (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_comment_likes_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tags (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    description VARCHAR(1000) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tags_slug (slug),
    KEY idx_tags_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS article_tags (
    article_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (article_id, tag_id),
    KEY idx_article_tags_tag_article (tag_id, article_id),
    CONSTRAINT fk_article_tags_article FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE,
    CONSTRAINT fk_article_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS series (
    id BIGINT NOT NULL AUTO_INCREMENT,
    slug VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000) NULL,
    section_id BIGINT NOT NULL,
    category_id BIGINT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_series_slug (slug),
    KEY idx_series_section_updated (section_id, updated_at, id),
    KEY idx_series_category_id (category_id),
    KEY idx_series_created_by (created_by),
    CONSTRAINT fk_series_section FOREIGN KEY (section_id) REFERENCES sections(id) ON DELETE RESTRICT,
    CONSTRAINT fk_series_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    CONSTRAINT fk_series_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS series_chapters (
    id BIGINT NOT NULL AUTO_INCREMENT,
    series_id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    title VARCHAR(200) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_series_chapters_parent_order (series_id, parent_id, sort_order, id),
    KEY idx_series_chapters_parent_id (parent_id),
    CONSTRAINT fk_series_chapters_series FOREIGN KEY (series_id) REFERENCES series(id) ON DELETE CASCADE,
    CONSTRAINT fk_series_chapters_parent FOREIGN KEY (parent_id) REFERENCES series_chapters(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS series_articles (
    series_id BIGINT NOT NULL,
    article_id BIGINT NOT NULL,
    sort_order INT NOT NULL,
    chapter_id BIGINT NULL,
    PRIMARY KEY (series_id, article_id),
    KEY idx_series_articles_order (series_id, sort_order, article_id),
    KEY idx_series_articles_article (article_id, series_id),
    KEY idx_series_articles_chapter_order (chapter_id, sort_order, article_id),
    CONSTRAINT fk_series_articles_series FOREIGN KEY (series_id) REFERENCES series(id) ON DELETE CASCADE,
    CONSTRAINT fk_series_articles_article FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE,
    CONSTRAINT fk_series_articles_chapter FOREIGN KEY (chapter_id) REFERENCES series_chapters(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
