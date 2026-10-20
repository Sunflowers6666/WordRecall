package com.wordrecall.service;

import com.wordrecall.domain.LearningRecord;
import com.wordrecall.dto.StatsDto;
import com.wordrecall.mapper.LearningRecordMapper;
import com.wordrecall.support.MybatisTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StatsServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    @BeforeAll
    static void initMybatisMetadata() {
        MybatisTestSupport.initAll();
    }

    private final LearningRecordMapper learningRecordMapper = mock(LearningRecordMapper.class);
    private final StatsService statsService = new StatsService(learningRecordMapper);

    @Test
    void aggregatesCountsAndConsecutiveDays() {
        when(learningRecordMapper.selectList(any())).thenReturn(
                List.of(day(TODAY), day(TODAY.minusDays(1)), day(TODAY.minusDays(2))),
                List.of(wordId(1L), wordId(2L), wordId(3L), wordId(4L), wordId(5L)));
        when(learningRecordMapper.selectCount(any())).thenReturn(2L, 4L);

        StatsDto stats = statsService.stats(1L, TODAY);

        assertThat(stats.learnedDays()).isEqualTo(3);
        assertThat(stats.learnedWords()).isEqualTo(5);
        assertThat(stats.wrongWords()).isEqualTo(2);
        assertThat(stats.todayWords()).isEqualTo(4);
        assertThat(stats.streakDays()).isEqualTo(3);
    }

    @Test
    void streakCountsBackFromToday() {
        assertThat(statsService.streak(Set.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(2)), TODAY))
                .isEqualTo(3);
    }

    @Test
    void streakStillCountsWhenTodayIsNotStudiedYet() {
        assertThat(statsService.streak(Set.of(TODAY.minusDays(1), TODAY.minusDays(2)), TODAY))
                .isEqualTo(2);
    }

    @Test
    void streakBreaksOnAGap() {
        assertThat(statsService.streak(Set.of(TODAY.minusDays(2), TODAY.minusDays(3)), TODAY)).isZero();
    }

    @Test
    void streakIsZeroWithoutRecords() {
        assertThat(statsService.streak(Set.of(), TODAY)).isZero();
    }

    private static LearningRecord day(LocalDate date) {
        LearningRecord record = new LearningRecord();
        record.setLearnDate(date);
        return record;
    }

    private static LearningRecord wordId(Long wordId) {
        LearningRecord record = new LearningRecord();
        record.setWordId(wordId);
        return record;
    }
}
