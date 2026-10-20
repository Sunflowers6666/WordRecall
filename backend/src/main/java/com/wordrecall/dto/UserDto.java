package com.wordrecall.dto;

import com.wordrecall.domain.User;

public record UserDto(Long id, String nickname, String avatarUrl, Long currentWordbookId) {

    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getNickname(), user.getAvatarUrl(), user.getCurrentWordbookId());
    }
}
