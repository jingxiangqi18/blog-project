-- Run ONCE after 20261023_content_organization.sql, against the selected database.
-- Existing series order and article membership are preserved. Old entries remain ungrouped.

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

ALTER TABLE series_articles ADD COLUMN chapter_id BIGINT NULL;
CREATE INDEX idx_series_articles_chapter_order ON series_articles(chapter_id, sort_order, article_id);
ALTER TABLE series_articles ADD CONSTRAINT fk_series_articles_chapter
    FOREIGN KEY (chapter_id) REFERENCES series_chapters(id) ON DELETE SET NULL;
