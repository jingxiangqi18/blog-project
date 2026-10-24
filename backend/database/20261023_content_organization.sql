-- Run ONCE after the earlier migrations, against the selected existing database.
-- Back up first. DDL is not transactional in MySQL/TiDB; record completed steps
-- if execution is interrupted. No historical article/category is assigned a section.

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

ALTER TABLE categories ADD COLUMN section_id BIGINT NULL;
CREATE INDEX idx_categories_section_id ON categories(section_id);
ALTER TABLE categories ADD CONSTRAINT fk_categories_section
    FOREIGN KEY (section_id) REFERENCES sections(id) ON DELETE RESTRICT;

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
