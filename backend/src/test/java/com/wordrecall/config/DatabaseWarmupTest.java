package com.wordrecall.config;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatabaseWarmupTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    @Test
    void returnsAfterFirstSuccessfulProbe() {
        when(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Integer.class))).thenReturn(1);

        new DatabaseWarmup(jdbcTemplate, 5, 0L).run(null);

        verify(jdbcTemplate, times(1)).queryForObject(eq("SELECT 1"), eq(Integer.class));
    }

    @Test
    void retriesWhileDatabaseIsResuming() {
        when(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Integer.class)))
                .thenThrow(new DataAccessResourceFailureException(
                        "CynosDB serverless instance is resuming, please try connecting again."))
                .thenThrow(new DataAccessResourceFailureException("still resuming"))
                .thenReturn(1);

        new DatabaseWarmup(jdbcTemplate, 5, 0L).run(null);

        verify(jdbcTemplate, times(3)).queryForObject(eq("SELECT 1"), eq(Integer.class));
    }

    @Test
    void givesUpAfterMaxAttemptsWithoutThrowing() {
        when(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Integer.class)))
                .thenThrow(new DataAccessResourceFailureException("down"));

        new DatabaseWarmup(jdbcTemplate, 3, 0L).run(null);

        verify(jdbcTemplate, times(3)).queryForObject(eq("SELECT 1"), eq(Integer.class));
    }
}
