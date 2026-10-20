package com.wordrecall.service;

import com.wordrecall.common.BizException;
import com.wordrecall.common.ErrorCode;
import com.wordrecall.domain.LearningRecord;
import com.wordrecall.dto.CalendarDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CalendarServiceTest {

    private final LearningRecordService learningRecordService = mock(LearningRecordService.class);
    private final CalendarService calendarService = new CalendarService(learningRecordService);

    @Test
    void groupsRecordsByDateAndSummarizesMonth() {
        when(learningRecordService.findByUserAndDateRange(eq(1L),
                eq(LocalDate.of(2026, 10, 1)), eq(LocalDate.of(2026, 10, 31))))
                .thenReturn(List.of(
                        record(LocalDate.of(2026, 10, 1), false),
                        record(LocalDate.of(2026, 10, 1), true),
                        record(LocalDate.of(2026, 10, 3), false)));

        CalendarDto calendar = calendarService.month(1L, 2026, 10);

        assertThat(calendar.month()).isEqualTo("2026-10");
        assertThat(calendar.days()).hasSize(2);
        assertThat(calendar.days().get(0).date()).isEqualTo("2026-10-01");
        assertThat(calendar.days().get(0).wordCount()).isEqualTo(2);
        assertThat(calendar.days().get(0).wrongCount()).isEqualTo(1);
        assertThat(calendar.days().get(1).date()).isEqualTo("2026-10-03");
        assertThat(calendar.days().get(1).wordCount()).isEqualTo(1);
        assertThat(calendar.days().get(1).wrongCount()).isZero();
        assertThat(calendar.summary().days()).isEqualTo(2);
        assertThat(calendar.summary().words()).isEqualTo(3);
        assertThat(calendar.summary().wrong()).isEqualTo(1);
    }

    @Test
    void returnsEmptyMonthWhenNothingLearned() {
        when(learningRecordService.findByUserAndDateRange(any(), any(), any())).thenReturn(List.of());

        CalendarDto calendar = calendarService.month(1L, 2026, 2);

        assertThat(calendar.month()).isEqualTo("2026-02");
        assertThat(calendar.days()).isEmpty();
        assertThat(calendar.summary().days()).isZero();
        assertThat(calendar.summary().words()).isZero();
        assertThat(calendar.summary().wrong()).isZero();
    }

    @Test
    void rejectsMonthOutOfRange() {
        assertThatThrownBy(() -> calendarService.month(1L, 2026, 13))
                .isInstanceOfSatisfying(BizException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.BAD_REQUEST));
        assertThatThrownBy(() -> calendarService.month(1L, 1999, 1))
                .isInstanceOfSatisfying(BizException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }

    private static LearningRecord record(LocalDate date, boolean wrong) {
        LearningRecord record = new LearningRecord();
        record.setLearnDate(date);
        record.setIsWrong(wrong);
        return record;
    }
}
