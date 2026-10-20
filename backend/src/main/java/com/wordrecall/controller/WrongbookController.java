package com.wordrecall.controller;

import com.wordrecall.common.ApiResponse;
import com.wordrecall.dto.WrongbookDto;
import com.wordrecall.dto.WrongbookSubmitRequest;
import com.wordrecall.security.UserPrincipal;
import com.wordrecall.service.WrongbookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wrongbook")
@RequiredArgsConstructor
public class WrongbookController {

    private final WrongbookService wrongbookService;

    @GetMapping
    public ApiResponse<WrongbookDto> list(@AuthenticationPrincipal UserPrincipal principal,
                                          @RequestParam(defaultValue = "0") int offset,
                                          @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.ok(wrongbookService.list(principal.userId(), offset, limit));
    }

    @PostMapping("/submit")
    public ApiResponse<Integer> submit(@AuthenticationPrincipal UserPrincipal principal,
                                       @Valid @RequestBody WrongbookSubmitRequest request) {
        return ApiResponse.ok(wrongbookService.submit(principal.userId(), request.rememberedWordIds()));
    }
}
