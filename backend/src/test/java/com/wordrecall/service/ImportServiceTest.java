package com.wordrecall.service;

import com.wordrecall.common.BizException;
import com.wordrecall.common.ErrorCode;
import com.wordrecall.domain.Word;
import com.wordrecall.domain.Wordbook;
import com.wordrecall.dto.ImportResultDto;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImportServiceTest {

    private final SheetParser parser = new SheetParser();
    private final WordService wordService = mock(WordService.class);
    private final WordbookService wordbookService = mock(WordbookService.class);
    private final ImportService importService = new ImportService(parser, wordService, wordbookService);

    private MockMultipartFile csv(String text) {
        return new MockMultipartFile("file", "我的词表.csv", "text/csv",
                text.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void importsIntoNewBookNamedAfterFile() throws Exception {
        when(wordbookService.create(eq(1L), eq("我的词表"), any())).thenReturn(book(10L, "我的词表"));
        when(wordService.maxSortOrder(10L)).thenReturn(0);
        when(wordService.save(ArgumentMatchers.<Word>any())).thenAnswer(invocation -> invocation.getArgument(0));

        ImportResultDto result = importService.importInto(1L, null, null,
                csv("单词,释义\nalpha,阿尔法\nbeta,贝塔\nalpha,重复\n"));

        assertThat(result.bookId()).isEqualTo(10L);
        assertThat(result.total()).isEqualTo(3);
        assertThat(result.imported()).isEqualTo(2);
        assertThat(result.skippedDuplicate()).isEqualTo(1);
        assertThat(result.problems()).hasSize(1);
        assertThat(result.problems().get(0).reason()).isEqualTo("文件内重复");
        verify(wordService, times(2)).save(ArgumentMatchers.<Word>any());
    }

    @Test
    void importsIntoExistingBook() throws Exception {
        when(wordbookService.requireOwned(1L, 9L)).thenReturn(book(9L, "已有词库"));
        when(wordService.maxSortOrder(9L)).thenReturn(5);
        when(wordService.save(ArgumentMatchers.<Word>any())).thenAnswer(invocation -> invocation.getArgument(0));

        ImportResultDto result = importService.importInto(1L, 9L, null, csv("apple,苹果\n"));

        assertThat(result.imported()).isEqualTo(1);
        verify(wordbookService, never()).create(anyLong(), any(), any());
        verify(wordService).save(ArgumentMatchers.argThat(word -> word.getSortOrder() == 6));
    }

    @Test
    void skipsRowsWithoutMeaning() throws Exception {
        when(wordbookService.requireOwned(1L, 9L)).thenReturn(book(9L, "已有词库"));
        when(wordService.maxSortOrder(9L)).thenReturn(0);
        when(wordService.save(ArgumentMatchers.<Word>any())).thenAnswer(invocation -> invocation.getArgument(0));

        ImportResultDto result = importService.importInto(1L, 9L, null,
                csv("单词,释义\napple,苹果\nbroken,\n"));

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.skippedInvalid()).isEqualTo(1);
        assertThat(result.problems().get(0).spelling()).isEqualTo("broken");
    }

    @Test
    void rejectsForeignBook() throws Exception {
        when(wordbookService.requireOwned(1L, 9L))
                .thenThrow(new BizException(ErrorCode.NOT_FOUND, "词库不存在"));

        assertThatThrownBy(() -> importService.importInto(1L, 9L, null, csv("apple,苹果\n")))
                .isInstanceOf(BizException.class);
    }

    @Test
    void previewCountsValidAndInvalid() throws Exception {
        var preview = importService.preview(csv("单词,释义\napple,苹果\nbroken,\n"));

        assertThat(preview.total()).isEqualTo(2);
        assertThat(preview.valid()).isEqualTo(1);
        assertThat(preview.invalid()).isEqualTo(1);
        assertThat(preview.fileName()).isEqualTo("我的词表.csv");
    }

    @Test
    void rejectsFileWithNoRows() {
        assertThatThrownBy(() -> importService.preview(csv("单词,释义\n")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("没有可导入的行");
    }

    private static Wordbook book(Long id, String name) {
        Wordbook wordbook = new Wordbook();
        wordbook.setId(id);
        wordbook.setName(name);
        wordbook.setSource("USER");
        wordbook.setOwnerId(1L);
        return wordbook;
    }
}
