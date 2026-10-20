package com.wordrecall.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 云托管场景：容器缩容到 0 后，第一次请求会冷启动 Spring Boot，
 * 同时云托管 MySQL 可能处于「自动暂停」状态，恢复瞬间会报
 * "CynosDB serverless instance is resuming"。
 *
 * 这里在应用启动最后一步用 SELECT 1 把数据库唤醒，失败就重试，
 * 让第一个用户请求落在数据库已就绪之后，而不是直接看到 500。
 */
@Slf4j
@Component
public class DatabaseWarmup implements ApplicationRunner {

    private static final int DEFAULT_ATTEMPTS = 20;
    private static final long DEFAULT_BACKOFF_MILLIS = 1500L;

    private final JdbcTemplate jdbcTemplate;
    private final int maxAttempts;
    private final long backoffMillis;

    public DatabaseWarmup(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, DEFAULT_ATTEMPTS, DEFAULT_BACKOFF_MILLIS);
    }

    DatabaseWarmup(JdbcTemplate jdbcTemplate, int maxAttempts, long backoffMillis) {
        this.jdbcTemplate = jdbcTemplate;
        this.maxAttempts = maxAttempts;
        this.backoffMillis = backoffMillis;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                jdbcTemplate.queryForObject("SELECT 1", Integer.class);
                log.info("database warmup ok (attempt {})", attempt);
                return;
            } catch (DataAccessException exception) {
                String message = exception.getMostSpecificCause().getMessage();
                log.warn("database not ready (attempt {}/{}): {}", attempt, maxAttempts, message);
                sleep(backoffMillis);
            }
        }
        log.error("database warmup failed after {} attempts, serving anyway", maxAttempts);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
