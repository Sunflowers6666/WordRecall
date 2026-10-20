package com.wordrecall.service;

import com.wordrecall.common.BizException;
import com.wordrecall.common.ErrorCode;
import com.wordrecall.domain.Wordbook;
import com.wordrecall.mapper.WordbookMapper;
import com.wordrecall.support.MybatisTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WordbookServiceTest {

    @BeforeAll
    static void initMybatisMetadata() {
        MybatisTestSupport.initAll();
    }

    private final WordbookMapper wordbookMapper = mock(WordbookMapper.class);
    private final WordService wordService = mock(WordService.class);
    private final WordbookService wordbookService = new WordbookService(wordbookMapper, wordService);

    @Test
    void createStoresOwnedUserBook() {
        when(wordbookMapper.selectOne(any())).thenReturn(null);
        when(wordbookMapper.insert(ArgumentMatchers.<Wordbook>any())).thenReturn(1);

        Wordbook created = wordbookService.create(1L, "  我的词表  ", "随便记的");

        assertThat(created.getName()).isEqualTo("我的词表");
        assertThat(created.getOwnerId()).isEqualTo(1L);
        assertThat(created.getSource()).isEqualTo("USER");
        assertThat(created.getDescription()).isEqualTo("随便记的");
    }

    @Test
    void createRejectsDuplicateName() {
        when(wordbookMapper.selectOne(any())).thenReturn(builtin(1L, "CET-4"));

        assertThatThrownBy(() -> wordbookService.create(1L, "CET-4", null))
                .isInstanceOfSatisfying(BizException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.CONFLICT));
        verify(wordbookMapper, never()).insert(ArgumentMatchers.<Wordbook>any());
    }

    @Test
    void renameOwnBook() {
        when(wordbookMapper.selectById(10L)).thenReturn(owned(10L, 1L));
        when(wordbookMapper.selectOne(any())).thenReturn(null);
        when(wordbookMapper.update(isNull(), any())).thenReturn(1);

        Wordbook updated = wordbookService.update(1L, 10L, "新名字", null);

        assertThat(updated.getName()).isEqualTo("新名字");
    }

    @Test
    void updateRejectsBookOfAnotherUser() {
        when(wordbookMapper.selectById(10L)).thenReturn(owned(10L, 2L));

        assertThatThrownBy(() -> wordbookService.update(1L, 10L, "新名字", null))
                .isInstanceOfSatisfying(BizException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_FOUND));
        verify(wordbookMapper, never()).update(isNull(), any());
    }

    @Test
    void updateRejectsEmptyRequest() {
        when(wordbookMapper.selectById(10L)).thenReturn(owned(10L, 1L));

        assertThatThrownBy(() -> wordbookService.update(1L, 10L, null, null))
                .isInstanceOfSatisfying(BizException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }

    @Test
    void deleteRemovesOwnBook() {
        when(wordbookMapper.selectById(10L)).thenReturn(owned(10L, 1L));
        when(wordbookMapper.deleteById(10L)).thenReturn(1);

        wordbookService.delete(1L, 10L);

        verify(wordbookMapper).deleteById(10L);
    }

    @Test
    void deleteRejectsBuiltinBook() {
        when(wordbookMapper.selectById(1L)).thenReturn(builtin(1L, "CET-4"));

        assertThatThrownBy(() -> wordbookService.delete(1L, 1L))
                .isInstanceOfSatisfying(BizException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_FOUND));
        verify(wordbookMapper, never()).deleteById(1L);
    }

    @Test
    void requireOwnedRejectsBuiltinAndMissing() {
        when(wordbookMapper.selectById(1L)).thenReturn(builtin(1L, "CET-4"));
        when(wordbookMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> wordbookService.requireOwned(1L, 1L))
                .isInstanceOfSatisfying(BizException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_FOUND));
        assertThatThrownBy(() -> wordbookService.requireOwned(1L, 99L))
                .isInstanceOfSatisfying(BizException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    private static Wordbook owned(Long id, Long ownerId) {
        Wordbook wordbook = new Wordbook();
        wordbook.setId(id);
        wordbook.setName("book-" + id);
        wordbook.setSource("USER");
        wordbook.setOwnerId(ownerId);
        return wordbook;
    }

    private static Wordbook builtin(Long id, String name) {
        Wordbook wordbook = new Wordbook();
        wordbook.setId(id);
        wordbook.setName(name);
        wordbook.setSource("BUILTIN");
        wordbook.setOwnerId(null);
        return wordbook;
    }
}
