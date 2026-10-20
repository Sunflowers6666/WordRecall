package com.wordrecall.service;

import com.wordrecall.common.BizException;
import com.wordrecall.common.ErrorCode;
import com.wordrecall.domain.Word;
import com.wordrecall.dto.WordItemRequest;
import com.wordrecall.mapper.WordMapper;
import com.wordrecall.support.MybatisTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WordItemServiceTest {

    private static final WordItemRequest APPLE = new WordItemRequest(" apple ", " /ˈæpl/ ", " 苹果 ", null, null);

    @BeforeAll
    static void initMybatisMetadata() {
        MybatisTestSupport.initAll();
    }

    private final WordMapper wordMapper = mock(WordMapper.class);
    private final WordService wordService = new WordService(wordMapper);

    @Test
    void addAssignsNextSortOrderAndTrimsFields() {
        when(wordMapper.selectList(any())).thenReturn(List.of(wordWithSort(5)));
        when(wordMapper.insert(ArgumentMatchers.<Word>any())).thenReturn(1);

        Word added = wordService.add(10L, APPLE);

        assertThat(added.getWordbookId()).isEqualTo(10L);
        assertThat(added.getSpelling()).isEqualTo("apple");
        assertThat(added.getPhonetic()).isEqualTo("/ˈæpl/");
        assertThat(added.getMeaning()).isEqualTo("苹果");
        assertThat(added.getSortOrder()).isEqualTo(6);
        assertThat(added.getExampleSentence()).isNull();
    }

    @Test
    void addStartsSortOrderAtOneWhenWordbookEmpty() {
        when(wordMapper.selectList(any())).thenReturn(List.of());
        when(wordMapper.insert(ArgumentMatchers.<Word>any())).thenReturn(1);

        assertThat(wordService.add(10L, APPLE).getSortOrder()).isEqualTo(1);
    }

    @Test
    void addRejectsDuplicateWordInSameBook() {
        when(wordMapper.selectList(any())).thenReturn(List.of());
        when(wordMapper.insert(ArgumentMatchers.<Word>any()))
                .thenThrow(new DuplicateKeyException("uk_word_wordbook_spelling"));

        assertThatThrownBy(() -> wordService.add(10L, APPLE))
                .isInstanceOfSatisfying(BizException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    void updateChangesFields() {
        when(wordMapper.selectById(7L)).thenReturn(storedWord(7L));
        when(wordMapper.updateById(ArgumentMatchers.<Word>any())).thenReturn(1);

        Word updated = wordService.update(7L, new WordItemRequest("banana", null, "香蕉", "A banana.", "一根香蕉。"));

        assertThat(updated.getSpelling()).isEqualTo("banana");
        assertThat(updated.getPhonetic()).isNull();
        assertThat(updated.getMeaning()).isEqualTo("香蕉");
        assertThat(updated.getExampleSentence()).isEqualTo("A banana.");
    }

    @Test
    void updateMissingWordIsNotFound() {
        when(wordMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> wordService.update(99L, APPLE))
                .isInstanceOfSatisfying(BizException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_FOUND));
        verify(wordMapper, never()).updateById(ArgumentMatchers.<Word>any());
    }

    @Test
    void deleteRemovesWord() {
        when(wordMapper.selectById(7L)).thenReturn(storedWord(7L));
        when(wordMapper.deleteById(7L)).thenReturn(1);

        wordService.delete(7L);

        verify(wordMapper).deleteById(7L);
    }

    private static Word wordWithSort(int sortOrder) {
        Word word = new Word();
        word.setSortOrder(sortOrder);
        return word;
    }

    private static Word storedWord(Long id) {
        Word word = new Word();
        word.setId(id);
        word.setWordbookId(10L);
        word.setSpelling("apple");
        word.setMeaning("苹果");
        word.setSortOrder(1);
        return word;
    }
}
