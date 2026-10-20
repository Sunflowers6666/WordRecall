package com.wordrecall.service;

import com.wordrecall.domain.LearningRecord;
import com.wordrecall.domain.Word;
import com.wordrecall.dto.ReviewDto;
import com.wordrecall.dto.ReviewSubmitDto;
import com.wordrecall.dto.WordDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReviewServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 16);

    private final LearningRecordService learningRecordService = mock(LearningRecordService.class);
    private final WordService wordService = mock(WordService.class);
    private final ReviewService reviewService = new ReviewService(learningRecordService, wordService);

    @Test
    void returnsWordsInLearningOrderWithWrongFlags() {
        when(learningRecordService.findByUserAndDate(1L, DAY))
                .thenReturn(List.of(recordFor(5L, false), recordFor(6L, true)));
        when(wordService.findByIds(any())).thenReturn(List.of(word(5L), word(6L)));

        ReviewDto review = reviewService.byDate(1L, DAY);

        assertThat(review.date()).isEqualTo("2026-10-16");
        assertThat(review.words()).extracting(WordDto::id).containsExactly(5L, 6L);
        assertThat(review.words()).extracting(WordDto::isWrong).containsExactly(false, true);
        assertThat(review.words().get(0).spelling()).isEqualTo("word5");
    }

    @Test
    void returnsEmptyListWhenTheDayHasNoRecords() {
        when(learningRecordService.findByUserAndDate(1L, DAY)).thenReturn(List.of());

        ReviewDto review = reviewService.byDate(1L, DAY);

        assertThat(review.date()).isEqualTo("2026-10-16");
        assertThat(review.words()).isEmpty();
    }

    @Test
    void submitDelegatesToLearningRecordsAndReturnsTotals() {
        when(learningRecordService.submitReview(1L, DAY, Set.of(6L)))
                .thenReturn(new LearningRecordService.ReviewOutcome(20, 1));

        ReviewSubmitDto result = reviewService.submit(1L, DAY, List.of(6L));

        assertThat(result.date()).isEqualTo("2026-10-16");
        assertThat(result.total()).isEqualTo(20);
        assertThat(result.wrong()).isEqualTo(1);
    }

    @Test
    void submitTreatsMissingWrongListAsEmpty() {
        when(learningRecordService.submitReview(1L, DAY, Set.of()))
                .thenReturn(new LearningRecordService.ReviewOutcome(5, 0));

        ReviewSubmitDto result = reviewService.submit(1L, DAY, null);

        assertThat(result.total()).isEqualTo(5);
        assertThat(result.wrong()).isZero();
    }

    private static LearningRecord recordFor(Long wordId, boolean wrong) {
        LearningRecord record = new LearningRecord();
        record.setWordId(wordId);
        record.setLearnDate(DAY);
        record.setIsWrong(wrong);
        return record;
    }

    private static Word word(Long id) {
        Word word = new Word();
        word.setId(id);
        word.setSpelling("word" + id);
        word.setMeaning("meaning" + id);
        return word;
    }
}
