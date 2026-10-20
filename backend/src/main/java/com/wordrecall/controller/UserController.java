package com.wordrecall.controller;

import com.wordrecall.common.ApiResponse;
import com.wordrecall.dto.UpdateUserRequest;
import com.wordrecall.dto.UpdateWordbookRequest;
import com.wordrecall.dto.UserDto;
import com.wordrecall.security.UserPrincipal;
import com.wordrecall.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PatchMapping("/me")
    public ApiResponse<UserDto> updateMe(@AuthenticationPrincipal UserPrincipal principal,
                                         @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.ok(UserDto.from(userService.updateNickname(principal.userId(), request.nickname())));
    }

    @PutMapping("/me/wordbook")
    public ApiResponse<UserDto> switchWordbook(@AuthenticationPrincipal UserPrincipal principal,
                                               @Valid @RequestBody UpdateWordbookRequest request) {
        return ApiResponse.ok(UserDto.from(
                userService.updateCurrentWordbook(principal.userId(), request.wordbookId())));
    }
}
