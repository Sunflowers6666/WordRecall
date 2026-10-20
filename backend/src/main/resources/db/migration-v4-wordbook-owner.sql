-- 阶段 4：自建词库
-- owner_id 为 NULL 表示预置词库；用户自建词库只有本人可见/可改
-- 应用账号没有 DDL 权限，用 root 执行：
--   mysql --defaults-extra-file="E:\Develop\MySQL\.secrets\client-root.cnf" -D wordapp < migration-v4-wordbook-owner.sql

ALTER TABLE wordbook
    ADD COLUMN owner_id BIGINT UNSIGNED NULL AFTER source,
    ADD KEY idx_wordbook_owner (owner_id),
    ADD CONSTRAINT fk_wordbook_owner
        FOREIGN KEY (owner_id) REFERENCES `user` (id)
        ON DELETE CASCADE;
