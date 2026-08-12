-- Run this migration once on an existing blog database.

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
