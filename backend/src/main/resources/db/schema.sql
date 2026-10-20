-- WordRecall / 单词日历 schema
-- Apply as MySQL root:
--   mysql --defaults-extra-file=... -D wordapp < schema.sql
-- The application account only has SELECT/INSERT/UPDATE/DELETE.

CREATE TABLE IF NOT EXISTS wordbook (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100)    NOT NULL,
    description VARCHAR(500)    NULL,
    source      VARCHAR(16)     NOT NULL DEFAULT 'BUILTIN',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_wordbook_name (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `user` (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    openid              VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    nickname            VARCHAR(64)     NULL,
    avatar_url          VARCHAR(512)    NULL,
    current_wordbook_id BIGINT UNSIGNED NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_openid (openid),
    KEY idx_user_current_wordbook (current_wordbook_id),
    CONSTRAINT fk_user_current_wordbook
        FOREIGN KEY (current_wordbook_id) REFERENCES wordbook (id)
        ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS word (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    wordbook_id         BIGINT UNSIGNED NOT NULL,
    spelling            VARCHAR(100)    NOT NULL,
    phonetic            VARCHAR(100)    NULL,
    meaning             TEXT            NOT NULL,
    example_sentence    TEXT            NULL,
    example_translation TEXT            NULL,
    sort_order          INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_word_wordbook_spelling (wordbook_id, spelling),
    CONSTRAINT fk_word_wordbook
        FOREIGN KEY (wordbook_id) REFERENCES wordbook (id)
        ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS learning_record (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED NOT NULL,
    word_id     BIGINT UNSIGNED NOT NULL,
    wordbook_id BIGINT UNSIGNED NOT NULL,
    learn_date  DATE            NOT NULL,
    is_wrong    TINYINT(1)      NOT NULL DEFAULT 0,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_word_date (user_id, word_id, learn_date),
    KEY idx_user_date (user_id, learn_date),
    KEY idx_lr_word (word_id),
    KEY idx_lr_wordbook (wordbook_id),
    CONSTRAINT fk_lr_user
        FOREIGN KEY (user_id) REFERENCES `user` (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_lr_word
        FOREIGN KEY (word_id) REFERENCES word (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_lr_wordbook
        FOREIGN KEY (wordbook_id) REFERENCES wordbook (id)
        ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
