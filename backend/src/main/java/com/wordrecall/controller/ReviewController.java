package com.wordrecall.controller;

import com.wordrecall.common.ApiResponse;
import com.wordrecall.dto.ReviewDto;
import com.wordrecall.dto.ReviewSubmitDto;
import com.wordrecall.dto.ReviewSubmitRequest;
import com.wordrecall.security.UserPrincipal;
import com.wordrecall.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ApiResponse<ReviewDto> byDate(@AuthenticationPrincipal UserPrincipal principal,
                                         @RequestParam
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(reviewService.byDate(principal.userId(), date));
    }

    @PostMapping("/submit")
    public ApiResponse<ReviewSubmitDto> submit(@AuthenticationPrincipal UserPrincipal principal,
                                               @Valid @RequestBody ReviewSubmitRequest request) {
        return ApiResponse.ok(reviewService.submit(principal.userId(), request.date(), request.wrongWordIds()));
    }
}
