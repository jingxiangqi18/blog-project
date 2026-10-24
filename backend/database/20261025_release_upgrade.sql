-- Blog release upgrade: content organization and series reading.
-- This is an ALTERNATIVE to running these two files separately:
--   20261023_content_organization.sql
--   20261024_series_reading.sql
-- It is NOT a third migration to run after those two files.
--
-- Before running:
-- 1. Back up the production database and select the correct database.
--    Check the selected database with: SELECT DATABASE();
-- 2. The existing users, categories and articles tables must already exist.
--    Earlier draft, social-interaction and knowledge-card migrations must
--    already be applied. This file does not upgrade those older features.
-- 3. Run the FULL file only if NEITHER migration above has been applied.
--    If 20261023 is already complete, run PART B only.
--    If both are complete, do not run this file.
--    For a new empty database, use schema.sql instead.
--
-- Run the required parts ONCE. ALTER statements are not repeatable.
-- MySQL/TiDB DDL does not roll back as one transaction. Stop on errors and
-- inspect completed statements before continuing; do not blindly rerun.
-- No tables or historical rows are deleted, replaced or copied from local.
-- Existing article/category sections remain NULL. Only the two missing
-- default sections are inserted; existing sections are left unchanged.

-- PART A: Content organization (20261023).

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

-- Categories gain an optional section; historical categories stay global.
ALTER TABLE categories ADD COLUMN section_id BIGINT NULL;
CREATE INDEX idx_categories_section_id ON categories(section_id);
ALTER TABLE categories ADD CONSTRAINT fk_categories_section
    FOREIGN KEY (section_id) REFERENCES sections(id) ON DELETE RESTRICT;

-- Existing article text is unchanged. The default content type is general.
ALTER TABLE articles ADD COLUMN section_id BIGINT NULL;
ALTER TABLE articles ADD COLUMN summary VARCHAR(500) NULL;
ALTER TABLE articles ADD COLUMN content_type VARCHAR(50) NOT NULL DEFAULT 'general';
CREATE INDEX idx_articles_section_updated ON articles(section_id, updated_at, id);
CREATE INDEX idx_articles_type_updated ON articles(content_type, updated_at, id);
ALTER TABLE articles ADD CONSTRAINT fk_articles_section
    FOREIGN KEY (section_id) REFERENCES sections(id) ON DELETE RESTRICT;

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

CREATE TABLE IF NOT EXISTS series_articles (
    series_id BIGINT NOT NULL,
    article_id BIGINT NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (series_id, article_id),
    KEY idx_series_articles_order (series_id, sort_order, article_id),
    KEY idx_series_articles_article (article_id, series_id),
    CONSTRAINT fk_series_articles_series FOREIGN KEY (series_id) REFERENCES series(id) ON DELETE CASCADE,
    CONSTRAINT fk_series_articles_article FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- PART B: Series reading (20261024).
-- Start here ONLY when PART A / 20261023 has already completed successfully.

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

-- Preserve series membership and order; existing entries remain ungrouped.
ALTER TABLE series_articles ADD COLUMN chapter_id BIGINT NULL;
CREATE INDEX idx_series_articles_chapter_order ON series_articles(chapter_id, sort_order, article_id);
ALTER TABLE series_articles ADD CONSTRAINT fk_series_articles_chapter
    FOREIGN KEY (chapter_id) REFERENCES series_chapters(id) ON DELETE SET NULL;

-- Optional read-only checks after the required parts finish successfully:
-- SHOW CREATE TABLE articles;
-- SHOW CREATE TABLE categories;
-- SHOW CREATE TABLE series_articles;
-- SHOW CREATE TABLE series_chapters;
-- SELECT slug, name, enabled FROM sections ORDER BY sort_order, id;
