package com.wordrecall.controller;

import com.wordrecall.common.ApiResponse;
import com.wordrecall.dto.StatsDto;
import com.wordrecall.dto.UpdateUserRequest;
import com.wordrecall.dto.UpdateWordbookRequest;
import com.wordrecall.dto.UserDto;
import com.wordrecall.security.UserPrincipal;
import com.wordrecall.service.StatsService;
import com.wordrecall.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final UserService userService;
    private final StatsService statsService;

    /** 修改昵称和/或每批词数，至少传一个。 */
    @PatchMapping("/me")
    public ApiResponse<UserDto> updateMe(@AuthenticationPrincipal UserPrincipal principal,
                                         @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.ok(UserDto.from(
                userService.updateProfile(principal.userId(), request.nickname(), request.batchSize())));
    }

    @PutMapping("/me/wordbook")
    public ApiResponse<UserDto> switchWordbook(@AuthenticationPrincipal UserPrincipal principal,
                                               @Valid @RequestBody UpdateWordbookRequest request) {
        return ApiResponse.ok(UserDto.from(
                userService.updateCurrentWordbook(principal.userId(), request.wordbookId())));
    }

    @GetMapping("/me/stats")
    public ApiResponse<StatsDto> stats(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(statsService.stats(principal.userId(), LocalDate.now(ZONE)));
    }
}
