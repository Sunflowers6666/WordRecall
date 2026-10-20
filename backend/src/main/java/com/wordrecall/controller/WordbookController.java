package com.wordrecall.controller;

import com.wordrecall.common.ApiResponse;
import com.wordrecall.common.BizException;
import com.wordrecall.common.ErrorCode;
import com.wordrecall.domain.User;
import com.wordrecall.domain.Word;
import com.wordrecall.domain.Wordbook;
import com.wordrecall.dto.WordDto;
import com.wordrecall.dto.WordItemRequest;
import com.wordrecall.dto.WordbookCreateRequest;
import com.wordrecall.dto.WordbookDto;
import com.wordrecall.dto.WordbookUpdateRequest;
import com.wordrecall.dto.WordbookWordsDto;
import com.wordrecall.security.UserPrincipal;
import com.wordrecall.service.UserService;
import com.wordrecall.service.WordService;
import com.wordrecall.service.WordbookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wordbooks")
@RequiredArgsConstructor
public class WordbookController {

    private final WordbookService wordbookService;
    private final WordService wordService;
    private final UserService userService;

    /** 匿名只看预置词库；登录后返回预置 + 自己的词库。 */
    @GetMapping
    public ApiResponse<List<WordbookDto>> list(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ApiResponse.ok(toDtos(wordbookService.listBuiltin(), null));
        }
        User user = userService.getById(principal.userId());
        return ApiResponse.ok(toDtos(wordbookService.listForUser(principal.userId()),
                user.getCurrentWordbookId()));
    }

    @PostMapping
    public ApiResponse<WordbookDto> create(@AuthenticationPrincipal UserPrincipal principal,
                                           @Valid @RequestBody WordbookCreateRequest request) {
        Wordbook wordbook = wordbookService.create(principal.userId(), request.name(), request.description());
        return ApiResponse.ok(toDto(wordbook, null));
    }

    @PatchMapping("/{id}")
    public ApiResponse<WordbookDto> update(@AuthenticationPrincipal UserPrincipal principal,
                                           @PathVariable Long id,
                                           @Valid @RequestBody WordbookUpdateRequest request) {
        Wordbook wordbook = wordbookService.update(principal.userId(), id, request.name(), request.description());
        return ApiResponse.ok(toDto(wordbook, null));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal UserPrincipal principal,
                                    @PathVariable Long id) {
        wordbookService.delete(principal.userId(), id);
        return ApiResponse.ok(null);
    }

    /** 词条列表：预置词库可读，自建词库仅本人可读。 */
    @GetMapping("/{id}/words")
    public ApiResponse<WordbookWordsDto> words(@AuthenticationPrincipal UserPrincipal principal,
                                               @PathVariable Long id,
                                               @RequestParam(defaultValue = "0") int offset,
                                               @RequestParam(defaultValue = "100") int limit) {
        Wordbook wordbook = wordbookService.findById(id);
        if (wordbook == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "词库不存在");
        }
        if (wordbook.getOwnerId() != null && !wordbook.getOwnerId().equals(principal.userId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "词库不存在");
        }
        List<WordDto> words = wordService.listByWordbook(id, offset, limit).stream()
                .map(word -> WordDto.from(word, false))
                .toList();
        return ApiResponse.ok(new WordbookWordsDto(wordService.countInWordbook(id), words));
    }

    @PostMapping("/{id}/words")
    public ApiResponse<WordDto> addWord(@AuthenticationPrincipal UserPrincipal principal,
                                        @PathVariable Long id,
                                        @Valid @RequestBody WordItemRequest request) {
        wordbookService.requireOwned(principal.userId(), id);
        Word word = wordService.add(id, request);
        return ApiResponse.ok(WordDto.from(word, false));
    }

    private List<WordbookDto> toDtos(List<Wordbook> wordbooks, Long currentWordbookId) {
        return wordbooks.stream()
                .map(wordbook -> toDto(wordbook, currentWordbookId))
                .toList();
    }

    private WordbookDto toDto(Wordbook wordbook, Long currentWordbookId) {
        return new WordbookDto(
                wordbook.getId(),
                wordbook.getName(),
                wordbook.getDescription(),
                wordbookService.countWords(wordbook.getId()),
                wordbook.getId().equals(currentWordbookId),
                wordbook.getOwnerId() != null,
                wordbook.getSource());
    }
}
