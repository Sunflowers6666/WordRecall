package com.wordrecall.controller;

import com.wordrecall.common.ApiResponse;
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
}
