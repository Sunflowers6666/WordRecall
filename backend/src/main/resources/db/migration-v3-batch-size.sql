-- 阶段 3：每批新学词数（10 / 20 / 30，默认 20）
-- 应用账号没有 DDL 权限，用 root 执行：
--   mysql --defaults-extra-file="E:\Develop\MySQL\.secrets\client-root.cnf" -D wordapp < migration-v3-batch-size.sql

ALTER TABLE `user`
    ADD COLUMN batch_size TINYINT UNSIGNED NOT NULL DEFAULT 20 AFTER current_wordbook_id;
