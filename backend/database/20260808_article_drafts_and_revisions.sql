-- Run this migration once on an existing blog database.

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

INSERT INTO article_revisions(
    article_id,
    revision_number,
    title,
    content,
    category_id,
    created_by,
    created_at
)
SELECT articles.id,
       1,
       articles.title,
       articles.content,
       articles.category_id,
       articles.author_id,
       articles.updated_at
FROM articles
WHERE NOT EXISTS (
    SELECT 1
    FROM article_revisions
    WHERE article_revisions.article_id = articles.id
);
