package com.wordrecall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(@NotBlank(message = "昵称不能为空")
                                @Size(max = 64, message = "昵称最多 64 个字符")
                                String nickname) {
}
