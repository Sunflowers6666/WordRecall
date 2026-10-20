package com.wordrecall.controller;

import com.wordrecall.common.ApiResponse;
import com.wordrecall.common.BizException;
import com.wordrecall.common.ErrorCode;
import com.wordrecall.domain.Word;
import com.wordrecall.dto.WordDto;
import com.wordrecall.dto.WordItemRequest;
import com.wordrecall.security.UserPrincipal;
import com.wordrecall.service.WordService;
import com.wordrecall.service.WordbookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 词条编辑/删除，仅自建词库的本人可操作。 */
@RestController
@RequestMapping("/api/words")
@RequiredArgsConstructor
public class WordController {

    private final WordService wordService;
    private final WordbookService wordbookService;

    @GetMapping("/{id}")
    public ApiResponse<WordDto> get(@AuthenticationPrincipal UserPrincipal principal,
                                    @PathVariable Long id) {
        Word word = wordService.requireById(id);
        requireReadable(principal, word);
        return ApiResponse.ok(WordDto.from(word, false));
    }

    @PutMapping("/{id}")
    public ApiResponse<WordDto> update(@AuthenticationPrincipal UserPrincipal principal,
                                       @PathVariable Long id,
                                       @Valid @RequestBody WordItemRequest request) {
        Word word = wordService.requireById(id);
        wordbookService.requireOwned(principal.userId(), word.getWordbookId());
        return ApiResponse.ok(WordDto.from(wordService.update(id, request), false));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal UserPrincipal principal,
                                    @PathVariable Long id) {
        Word word = wordService.requireById(id);
        wordbookService.requireOwned(principal.userId(), word.getWordbookId());
        wordService.delete(id);
        return ApiResponse.ok(null);
    }

    /** 预置词库可读；自建词库只有本人可读，其余按不存在处理。 */
    private void requireReadable(UserPrincipal principal, Word word) {
        var wordbook = wordbookService.findById(word.getWordbookId());
        if (wordbook == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "词库不存在");
        }
        if (wordbook.getOwnerId() != null && !wordbook.getOwnerId().equals(principal.userId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "单词不存在");
        }
    }
}
