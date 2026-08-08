-- Run this migration once on an existing blog database.

ALTER TABLE comments
    ADD COLUMN IF NOT EXISTS parent_id BIGINT NULL AFTER author_id;

ALTER TABLE comments
    ADD INDEX IF NOT EXISTS idx_comments_parent_id (parent_id);

ALTER TABLE comments
    ADD CONSTRAINT fk_comments_parent
        FOREIGN KEY (parent_id) REFERENCES comments (id)
        ON DELETE CASCADE;

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
