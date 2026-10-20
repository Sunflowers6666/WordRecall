package com.wordrecall.controller;

import com.wordrecall.common.ApiResponse;
import com.wordrecall.domain.User;
import com.wordrecall.dto.WordbookDto;
import com.wordrecall.security.UserPrincipal;
import com.wordrecall.service.UserService;
import com.wordrecall.service.WordbookService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Anonymous visitors may browse wordbooks; {@code selected} is only meaningful when a
 * token is present.
 */
@RestController
@RequestMapping("/api/wordbooks")
@RequiredArgsConstructor
public class WordbookController {

    private final WordbookService wordbookService;
    private final UserService userService;

    @GetMapping
    public ApiResponse<List<WordbookDto>> list(@AuthenticationPrincipal UserPrincipal principal) {
        Long currentWordbookId = null;
        if (principal != null) {
            User user = userService.getById(principal.userId());
            currentWordbookId = user.getCurrentWordbookId();
        }

        Long current = currentWordbookId;
        List<WordbookDto> data = wordbookService.listAll().stream()
                .map(wordbook -> new WordbookDto(
                        wordbook.getId(),
                        wordbook.getName(),
                        wordbook.getDescription(),
                        wordbookService.countWords(wordbook.getId()),
                        wordbook.getId().equals(current)))
                .toList();
        return ApiResponse.ok(data);
    }
}
