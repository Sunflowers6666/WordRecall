package com.wordrecall.service;

import com.wordrecall.common.BizException;
import com.wordrecall.common.ErrorCode;
import com.wordrecall.domain.LearningRecord;
import com.wordrecall.mapper.LearningRecordMapper;
import com.wordrecall.support.MybatisTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LearningRecordServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 16);

    @BeforeAll
    static void initMybatisMetadata() {
        MybatisTestSupport.initAll();
    }

    private final LearningRecordMapper learningRecordMapper = mock(LearningRecordMapper.class);
    private final LearningRecordService learningRecordService = new LearningRecordService(learningRecordMapper);

    @Test
    void recordsOnlyWordsNotYetRecordedOnThatDay() {
        when(learningRecordMapper.selectList(any())).thenReturn(List.of(recordFor(2L)));
        when(learningRecordMapper.insert(ArgumentMatchers.<LearningRecord>any())).thenReturn(1);

        int inserted = learningRecordService.recordStudy(1L, 10L, List.of(1L, 2L, 3L), DAY);

        assertThat(inserted).isEqualTo(2);
        ArgumentCaptor<LearningRecord> captor = ArgumentCaptor.forClass(LearningRecord.class);
        verify(learningRecordMapper, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(LearningRecord::getWordId).containsExactly(1L, 3L);
        assertThat(captor.getAllValues()).allSatisfy(record -> {
            assertThat(record.getUserId()).isEqualTo(1L);
            assertThat(record.getWordbookId()).isEqualTo(10L);
            assertThat(record.getLearnDate()).isEqualTo(DAY);
            assertThat(record.getIsWrong()).isFalse();
        });
    }

    @Test
    void skipsDatabaseWriteWhenBatchIsEmpty() {
        assertThat(learningRecordService.recordStudy(1L, 10L, List.of(), DAY)).isZero();
        verify(learningRecordMapper, never()).insert(ArgumentMatchers.<LearningRecord>any());
    }

    @Test
    void submitReviewMarksWrongWordsAndResetsTheRest() {
        when(learningRecordMapper.selectList(any())).thenReturn(List.of(
                recordFor(11L), recordFor(12L), recordFor(13L)));
        when(learningRecordMapper.update(isNull(), any())).thenReturn(3).thenReturn(2);

        LearningRecordService.ReviewOutcome outcome =
                learningRecordService.submitReview(1L, DAY, List.of(11L, 13L, 13L));

        assertThat(outcome.total()).isEqualTo(3);
        assertThat(outcome.wrong()).isEqualTo(2);
        verify(learningRecordMapper, times(2)).update(isNull(), any());
    }

    @Test
    void submitReviewWithoutWrongWordsOnlyResets() {
        when(learningRecordMapper.selectList(any())).thenReturn(List.of(recordFor(11L)));
        when(learningRecordMapper.update(isNull(), any())).thenReturn(1);

        LearningRecordService.ReviewOutcome outcome = learningRecordService.submitReview(1L, DAY, List.of());

        assertThat(outcome.total()).isEqualTo(1);
        assertThat(outcome.wrong()).isZero();
        verify(learningRecordMapper, times(1)).update(isNull(), any());
    }

    @Test
    void submitReviewRejectsWordThatDoesNotBelongToTheDay() {
        when(learningRecordMapper.selectList(any())).thenReturn(List.of(recordFor(11L)));

        assertThatThrownBy(() -> learningRecordService.submitReview(1L, DAY, List.of(99L)))
                .isInstanceOfSatisfying(BizException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.BAD_REQUEST));
        verify(learningRecordMapper, never()).update(isNull(), any());
    }

    @Test
    void submitReviewFailsWhenTheDayHasNoRecords() {
        when(learningRecordMapper.selectList(any())).thenReturn(List.of());

        assertThatThrownBy(() -> learningRecordService.submitReview(1L, DAY, Set.of()))
                .isInstanceOfSatisfying(BizException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    private static LearningRecord recordFor(Long wordId) {
        LearningRecord record = new LearningRecord();
        record.setWordId(wordId);
        record.setLearnDate(DAY);
        record.setIsWrong(false);
        return record;
    }
}
