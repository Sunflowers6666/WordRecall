package com.wordrecall.controller;

import com.wordrecall.common.ApiResponse;
import com.wordrecall.dto.CalendarDto;
import com.wordrecall.security.UserPrincipal;
import com.wordrecall.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping
    public ApiResponse<CalendarDto> month(@AuthenticationPrincipal UserPrincipal principal,
                                          @RequestParam int year,
                                          @RequestParam int month) {
        return ApiResponse.ok(calendarService.month(principal.userId(), year, month));
    }
}
