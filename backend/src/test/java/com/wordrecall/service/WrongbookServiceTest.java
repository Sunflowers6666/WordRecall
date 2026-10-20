package com.wordrecall.service;

import com.wordrecall.domain.LearningRecord;
import com.wordrecall.domain.Word;
import com.wordrecall.dto.WrongbookDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WrongbookServiceTest {

    private final LearningRecordService learningRecordService = mock(LearningRecordService.class);
    private final WordService wordService = mock(WordService.class);
    private final WrongbookService wrongbookService = new WrongbookService(learningRecordService, wordService);

    @Test
    void keepsOnlyWordsWhoseLatestRecordIsWrong() {
        when(learningRecordService.findByUserOrdered(1L)).thenReturn(List.of(
                record(11L, 1L, LocalDate.of(2026, 10, 20), false),   // w1 最新：已掌握
                record(10L, 1L, LocalDate.of(2026, 10, 18), true),    // w1 更早：错过
                record(21L, 2L, LocalDate.of(2026, 10, 19), true),    // w2 最新：错
                record(31L, 3L, LocalDate.of(2026, 10, 17), false)));
        when(wordService.findByIds(any())).thenReturn(List.of(word(2L, "beta")));

        WrongbookDto dto = wrongbookService.list(1L, 0, 100);

        assertThat(dto.total()).isEqualTo(1);
        assertThat(dto.words()).hasSize(1);
        assertThat(dto.words().get(0).id()).isEqualTo(2L);
        assertThat(dto.words().get(0).isWrong()).isTrue();
    }

    @Test
    void paginatesWrongWords() {
        when(learningRecordService.findByUserOrdered(1L)).thenReturn(List.of(
                record(1L, 1L, LocalDate.of(2026, 10, 20), true),
                record(2L, 2L, LocalDate.of(2026, 10, 20), true),
                record(3L, 3L, LocalDate.of(2026, 10, 20), true)));
        when(wordService.findByIds(any())).thenReturn(List.of(word(1L, "alpha"), word(2L, "beta")));

        WrongbookDto dto = wrongbookService.list(1L, 0, 2);

        assertThat(dto.total()).isEqualTo(3);
        assertThat(dto.words()).hasSize(2);
    }

    @Test
    void submitClearsOnlyLatestWrongRecords() {
        when(learningRecordService.findLatestForWord(1L, 1L)).thenReturn(record(11L, 1L, LocalDate.now(), true));
        when(learningRecordService.findLatestForWord(1L, 2L)).thenReturn(record(21L, 2L, LocalDate.now(), true));
        when(learningRecordService.findLatestForWord(1L, 3L)).thenReturn(record(31L, 3L, LocalDate.now(), false));
        when(learningRecordService.clearWrong(11L)).thenReturn(1);
        when(learningRecordService.clearWrong(21L)).thenReturn(1);

        int cleared = wrongbookService.submit(1L, List.of(1L, 2L, 2L, 3L));

        assertThat(cleared).isEqualTo(2);
        verify(learningRecordService, never()).clearWrong(31L);
    }

    @Test
    void submitIgnoresWordsWithoutRecords() {
        when(learningRecordService.findLatestForWord(1L, 9L)).thenReturn(null);

        assertThat(wrongbookService.submit(1L, List.of(9L))).isZero();
    }

    private static LearningRecord record(Long id, Long wordId, LocalDate date, boolean wrong) {
        LearningRecord record = new LearningRecord();
        record.setId(id);
        record.setWordId(wordId);
        record.setLearnDate(date);
        record.setIsWrong(wrong);
        return record;
    }

    private static Word word(Long id, String spelling) {
        Word word = new Word();
        word.setId(id);
        word.setSpelling(spelling);
        word.setMeaning("含义" + id);
        return word;
    }
}
