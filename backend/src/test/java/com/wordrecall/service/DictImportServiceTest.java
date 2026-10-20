package com.wordrecall.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordrecall.domain.Word;
import com.wordrecall.domain.Wordbook;
import com.wordrecall.dto.DictImportResult;
import com.wordrecall.dto.DictWord;
import com.wordrecall.dto.DictWordbookFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DictImportServiceTest {

    private WordbookService wordbookService;
    private WordService wordService;
    private DictImportService dictImportService;

    @BeforeEach
    void setUp() {
        wordbookService = mock(WordbookService.class);
        wordService = mock(WordService.class);
        dictImportService = new DictImportService(new ObjectMapper(), wordbookService, wordService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void importsWordsAndAssignsWordbookId() {
        when(wordbookService.findByName("CET-4")).thenReturn(null);
        when(wordbookService.save(any(Wordbook.class))).thenAnswer(invocation -> {
            Wordbook wordbook = invocation.getArgument(0);
            wordbook.setId(10L);
            return true;
        });
        when(wordService.count(any(Wrapper.class))).thenReturn(0L);
        when(wordService.saveBatch(anyList(), anyInt())).thenReturn(true);

        DictWordbookFile file = new DictWordbookFile(
                "CET-4",
                "大学英语四级核心词汇",
                "BUILTIN",
                "MIT",
                "ECDICT",
                List.of(
                        new DictWord("apple", "ˈæpl", "苹果", null, null, 1),
                        new DictWord("banana", "bəˈnɑːnə", "香蕉", null, null, 2)
                )
        );

        DictImportResult result = dictImportService.importFile(file);

        assertThat(result.imported()).isEqualTo(2);
        assertThat(result.skipped()).isFalse();

        ArgumentCaptor<List<Word>> captor = ArgumentCaptor.forClass(List.class);
        verify(wordService).saveBatch(captor.capture(), eq(500));
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue().get(0).getWordbookId()).isEqualTo(10L);
        assertThat(captor.getValue().get(0).getSpelling()).isEqualTo("apple");
        assertThat(captor.getValue().get(0).getSortOrder()).isEqualTo(1);
    }

    @Test
    void skipsWordbookThatAlreadyHasWords() {
        Wordbook existing = new Wordbook();
        existing.setId(10L);
        existing.setName("CET-4");
        when(wordbookService.findByName("CET-4")).thenReturn(existing);
        when(wordService.count(any(Wrapper.class))).thenReturn(3L);

        DictImportResult result = dictImportService.importFile(new DictWordbookFile(
                "CET-4",
                "大学英语四级核心词汇",
                "BUILTIN",
                "MIT",
                "ECDICT",
                List.of(new DictWord("apple", "ˈæpl", "苹果", null, null, 1))
        ));

        assertThat(result.skipped()).isTrue();
        assertThat(result.imported()).isZero();
        verify(wordService, never()).saveBatch(anyList(), anyInt());
    }
}
