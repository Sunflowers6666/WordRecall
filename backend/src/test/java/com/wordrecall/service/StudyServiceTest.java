package com.wordrecall.service;

import com.wordrecall.common.BizException;
import com.wordrecall.common.ErrorCode;
import com.wordrecall.domain.User;
import com.wordrecall.domain.Word;
import com.wordrecall.dto.StudyDto;
import com.wordrecall.dto.StudyRecordDto;
import com.wordrecall.dto.WordDto;
import com.wordrecall.mapper.WordMapper;
import com.wordrecall.support.MybatisTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    void usesUserBatchSizeWhenRequestSizeIsMissing() {
        User user = userWithWordbook(10L);
        user.setBatchSize(30);
        when(userService.getById(1L)).thenReturn(user);
        when(learningRecordService.findLearnedWordIds(1L, 10L)).thenReturn(Set.of());
        when(wordMapper.selectList(any())).thenReturn(manyWords(40));
        when(wordService.count(any())).thenReturn(100L);
        when(learningRecordService.countByUserAndDate(1L, DAY)).thenReturn(0L);

        StudyDto batch = studyService.startSession(1L, null, DAY);

        assertThat(batch.words()).hasSize(30);
    }

    @Test
    void previewsOnlyUnlearnedWordsCappedAtRequestedSize() {
        when(userService.getById(1L)).thenReturn(userWithWordbook(10L));
        when(learningRecordService.findLearnedWordIds(1L, 10L)).thenReturn(Set.of(1L));
        when(wordMapper.selectList(any())).thenReturn(List.of(
                word(1L, 10L), word(2L, 10L), word(3L, 10L), word(4L, 10L)));
        when(wordService.count(any())).thenReturn(6L);
        when(learningRecordService.countByUserAndDate(1L, DAY)).thenReturn(0L);

        StudyDto batch = studyService.startSession(1L, 2, DAY);

        assertThat(batch.words()).extracting(WordDto::id).containsExactly(2L, 3L);
        assertThat(batch.todayWordCount()).isZero();
        assertThat(batch.remaining()).isEqualTo(3);
        // 预览阶段不写任何学习记录
        verify(learningRecordService, never()).recordStudy(any(), any(), any(), any());
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
    void startSessionRejectsUserWithoutCurrentWordbook() {
        when(userService.getById(1L)).thenReturn(userWithWordbook(null));

        assertThatThrownBy(() -> studyService.startSession(1L, 20, DAY))
                .isInstanceOfSatisfying(BizException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }

    @Test
    void recordsWordsOfTheCurrentWordbook() {
        when(userService.getById(1L)).thenReturn(userWithWordbook(10L));
        when(wordService.findByIds(any())).thenReturn(List.of(word(1L, 10L), word(2L, 10L)));
        when(learningRecordService.recordStudy(eq(1L), eq(10L), any(), eq(DAY))).thenReturn(2);
        when(learningRecordService.countByUserAndDate(1L, DAY)).thenReturn(2L);

        StudyRecordDto result = studyService.recordWords(1L, List.of(1L, 2L), DAY);

        assertThat(result.recorded()).isEqualTo(2);
        assertThat(result.todayWordCount()).isEqualTo(2);
    }

    @Test
    void recordsDeduplicateRepeatedIdsBeforeWriting() {
        when(userService.getById(1L)).thenReturn(userWithWordbook(10L));
        when(wordService.findByIds(any())).thenReturn(List.of(word(1L, 10L), word(2L, 10L)));
        when(learningRecordService.recordStudy(eq(1L), eq(10L), any(), eq(DAY))).thenReturn(2);
        when(learningRecordService.countByUserAndDate(1L, DAY)).thenReturn(2L);

        studyService.recordWords(1L, List.of(1L, 1L, 2L), DAY);

        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(learningRecordService).recordStudy(eq(1L), eq(10L), captor.capture(), eq(DAY));
        assertThat(captor.getValue()).containsExactly(1L, 2L);
    }

    @Test
    void recordRejectsWordFromAnotherWordbook() {
        when(userService.getById(1L)).thenReturn(userWithWordbook(10L));
        when(wordService.findByIds(any())).thenReturn(List.of(word(1L, 99L)));

        assertThatThrownBy(() -> studyService.recordWords(1L, List.of(1L), DAY))
                .isInstanceOfSatisfying(BizException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.BAD_REQUEST));
        verify(learningRecordService, never()).recordStudy(any(), any(), any(), any());
    }

    @Test
    void recordRejectsUnknownWord() {
        when(userService.getById(1L)).thenReturn(userWithWordbook(10L));
        when(wordService.findByIds(any())).thenReturn(List.of(word(1L, 10L)));

        assertThatThrownBy(() -> studyService.recordWords(1L, List.of(1L, 2L), DAY))
                .isInstanceOfSatisfying(BizException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void recordRejectsEmptyList() {
        when(userService.getById(1L)).thenReturn(userWithWordbook(10L));

        assertThatThrownBy(() -> studyService.recordWords(1L, List.of(), DAY))
                .isInstanceOfSatisfying(BizException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }

    private static User userWithWordbook(Long wordbookId) {
        User user = new User();
        user.setId(1L);
        user.setCurrentWordbookId(wordbookId);
        user.setBatchSize(20);
        return user;
    }

    private static List<Word> manyWords(int count) {
        List<Word> words = new java.util.ArrayList<>();
        for (long id = 1; id <= count; id++) {
            words.add(word(id, 10L));
        }
        return words;
    }

    private static Word word(Long id, Long wordbookId) {
        Word word = new Word();
        word.setId(id);
        word.setWordbookId(wordbookId);
        word.setSpelling("word" + id);
        word.setMeaning("meaning" + id);
        return word;
    }
}
