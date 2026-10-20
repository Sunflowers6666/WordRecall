package com.wordrecall.service;

import com.wordrecall.common.BizException;
import com.wordrecall.common.ErrorCode;
import com.wordrecall.domain.User;
import com.wordrecall.domain.Word;
import com.wordrecall.dto.StudyDto;
import com.wordrecall.dto.WordDto;
import com.wordrecall.mapper.WordMapper;
import com.wordrecall.support.MybatisTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudyServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 16);

    @BeforeAll
    static void initMybatisMetadata() {
        MybatisTestSupport.initAll();
    }

    private final UserService userService = mock(UserService.class);
    private final WordService wordService = mock(WordService.class);
    private final WordMapper wordMapper = mock(WordMapper.class);
    private final LearningRecordService learningRecordService = mock(LearningRecordService.class);
    private final StudyService studyService =
            new StudyService(userService, wordService, wordMapper, learningRecordService);

    @Test
    void returnsOnlyUnlearnedWordsCappedAtRequestedSize() {
        when(userService.getById(1L)).thenReturn(userWithWordbook(10L));
        when(learningRecordService.findLearnedWordIds(1L, 10L)).thenReturn(Set.of(1L));
        when(wordMapper.selectList(any())).thenReturn(List.of(
                word(1L), word(2L), word(3L), word(4L)));
        when(wordService.count(any())).thenReturn(6L);
        when(learningRecordService.countByUserAndDate(1L, DAY)).thenReturn(3L);

        StudyDto batch = studyService.startSession(1L, 2, DAY);

        assertThat(batch.words()).extracting(WordDto::id).containsExactly(2L, 3L);
        assertThat(batch.todayWordCount()).isEqualTo(3);
        assertThat(batch.remaining()).isEqualTo(3);
        verify(learningRecordService).recordStudy(1L, 10L, List.of(2L, 3L), DAY);
    }

    @Test
    void reportsFinishedWordbookWithoutRecordingAnything() {
        when(userService.getById(1L)).thenReturn(userWithWordbook(10L));
        when(learningRecordService.findLearnedWordIds(1L, 10L)).thenReturn(Set.of(1L, 2L, 3L, 4L, 5L));
        when(wordMapper.selectList(any())).thenReturn(List.of());
        when(wordService.count(any())).thenReturn(5L);
        when(learningRecordService.countByUserAndDate(1L, DAY)).thenReturn(5L);

        StudyDto batch = studyService.startSession(1L, 20, DAY);

        assertThat(batch.words()).isEmpty();
        assertThat(batch.remaining()).isZero();
        verify(learningRecordService, never()).recordStudy(any(), any(), any(), any());
    }

    @Test
    void rejectsUserWithoutCurrentWordbook() {
        when(userService.getById(1L)).thenReturn(userWithWordbook(null));

        assertThatThrownBy(() -> studyService.startSession(1L, 20, DAY))
                .isInstanceOfSatisfying(BizException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }

    private static User userWithWordbook(Long wordbookId) {
        User user = new User();
        user.setId(1L);
        user.setCurrentWordbookId(wordbookId);
        return user;
    }

    private static Word word(Long id) {
        Word word = new Word();
        word.setId(id);
        word.setSpelling("word" + id);
        word.setMeaning("meaning" + id);
        return word;
    }
}
